package com.tapestry.scheduler;

import com.tapestry.lifecycle.PhaseController;
import com.tapestry.lifecycle.TapestryPhase;
import org.graalvm.polyglot.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Deterministic scheduler service for TypeScript mods.
 * 
 * Provides setTimeout, setInterval, and nextTick functionality
 * with deterministic ordering and fail-fast error handling.
 */
public class SchedulerService {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(SchedulerService.class);
    
    // Deterministic ordering: tick order, scheduled time, modId ascending, registration order
    private final TreeMap<Long, List<ScheduledTask>> tickWheel = new TreeMap<>();
    private final Map<String, ScheduledTask> intervalTasks = new ConcurrentHashMap<>();
    private final AtomicLong nextHandle = new AtomicLong(0);
    private final AtomicLong nextRegistrationOrder = new AtomicLong(0);
    
    private long currentTick = 0;
    private boolean isRunning = false;
    
    /**
     * Schedules a one-shot task to run after delay ticks.
     * 
     * @param callback JavaScript function to execute
     * @param delay delay in ticks
     * @param modId mod ID scheduling the task
     * @return opaque task handle
     */
    public String setTimeout(Value callback, long delay, String modId) {
        PhaseController.getInstance().requireAtLeast(TapestryPhase.RUNTIME);
        
        if (callback == null || !callback.canExecute()) {
            throw new IllegalArgumentException("Callback must be an executable function");
        }
        
        if (delay < 0) {
            throw new IllegalArgumentException("Delay must be non-negative");
        }
        
        String handle = "timeout_" + nextHandle.incrementAndGet();
        long targetTick = currentTick + delay;
        
        ScheduledTask task = new ScheduledTask(
            handle, callback, modId, targetTick, false, 
            nextRegistrationOrder.incrementAndGet()
        );
        
        tickWheel.computeIfAbsent(targetTick, k -> new ArrayList<>()).add(task);
        
        LOGGER.debug("Scheduled timeout {} for mod {} at tick {} (delay: {})", 
            handle, modId, targetTick, delay);
        
        return handle;
    }
    
    /**
     * Schedules a repeating task to run every interval ticks.
     * 
     * @param callback JavaScript function to execute
     * @param interval interval in ticks
     * @param modId mod ID scheduling the task
     * @return opaque task handle
     */
    public String setInterval(Value callback, long interval, String modId) {
        PhaseController.getInstance().requireAtLeast(TapestryPhase.RUNTIME);
        
        if (callback == null || !callback.canExecute()) {
            throw new IllegalArgumentException("Callback must be an executable function");
        }
        
        if (interval <= 0) {
            throw new IllegalArgumentException("Interval must be positive");
        }
        
        String handle = "interval_" + nextHandle.incrementAndGet();
        long targetTick = currentTick + interval;
        
        ScheduledTask task = new ScheduledTask(
            handle, callback, modId, targetTick, true, 
            nextRegistrationOrder.incrementAndGet()
        );
        task.setInterval(interval);
        
        intervalTasks.put(handle, task);
        tickWheel.computeIfAbsent(targetTick, k -> new ArrayList<>()).add(task);
        
        LOGGER.debug("Scheduled interval {} for mod {} every {} ticks (first at tick {})", 
            handle, modId, interval, targetTick);
        
        return handle;
    }
    
    /**
     * Cancels an interval task.
     * 
     * @param handle interval task handle
     * @param modId mod ID canceling the task
     */
    public void clearInterval(String handle, String modId) {
        PhaseController.getInstance().requireAtLeast(TapestryPhase.RUNTIME);
        
        ScheduledTask task = intervalTasks.remove(handle);
        if (task != null) {
            task.setCancelled(true);
            LOGGER.debug("Cancelled interval {} for mod {}", handle, modId);
        }
    }
    
    /**
     * Schedules a task to run on the next tick.
     * 
     * @param callback JavaScript function to execute
     * @param modId mod ID scheduling the task
     * @return opaque task handle
     */
    public String nextTick(Value callback, String modId) {
        return setTimeout(callback, 1, modId);
    }
    
    /**
     * Advances the scheduler to the next tick and executes due tasks.
     * 
     * @param tick the current server tick
     */
    public void tick(long tick) {
        currentTick = tick;
        
        List<ScheduledTask> dueTasks = tickWheel.remove(tick);
        if (dueTasks == null || dueTasks.isEmpty()) {
            return;
        }
        
        // Sort for deterministic execution: modId ascending, then registration order
        dueTasks.sort(Comparator
            .comparing(ScheduledTask::modId)
            .thenComparing(ScheduledTask::registrationOrder));
        
        for (ScheduledTask task : dueTasks) {
            if (task.isCancelled()) {
                continue;
            }
            
            try {
                executeTask(task);
                
                // Reschedule interval tasks
                if (task.isInterval() && !task.isCancelled()) {
                    long nextTick = currentTick + task.getInterval();
                    task.setTargetTick(nextTick);
                    tickWheel.computeIfAbsent(nextTick, k -> new ArrayList<>()).add(task);
                }
                
            } catch (Exception e) {
                LOGGER.error("Scheduled task {} from mod {} failed at tick {}", 
                    task.handle(), task.modId(), tick, e);
                throw new RuntimeException(
                    String.format("Scheduled task '%s' from mod '%s' failed at tick %d", 
                        task.handle(), task.modId(), tick), e
                );
            }
        }
    }
    
    /**
     * Executes a scheduled task with proper context.
     */
    private void executeTask(ScheduledTask task) {
        // Create immutable context object
        Map<String, Object> context = new HashMap<>();
        context.put("modId", task.modId());
        context.put("tick", currentTick);
        context.put("handle", task.handle());
        
        // Execute the callback
        task.callback().executeVoid(context);
        
        LOGGER.debug("Executed scheduled task {} for mod {} at tick {}", 
            task.handle(), task.modId(), currentTick);
    }
    
    /**
     * Gets the current tick.
     */
    public long getCurrentTick() {
        return currentTick;
    }
    
    /**
     * Gets the number of pending tasks.
     */
    public int getPendingTaskCount() {
        return tickWheel.values().stream().mapToInt(List::size).sum();
    }
    
    /**
     * Clears all scheduled tasks (for testing/shutdown).
     */
    public void clear() {
        tickWheel.clear();
        intervalTasks.clear();
        currentTick = 0;
        isRunning = false;
        LOGGER.debug("Scheduler cleared");
    }
    
    /**
     * Represents a scheduled task.
     */
    private static class ScheduledTask {
        private final String handle;
        private final Value callback;
        private final String modId;
        private final int registrationOrder;
        private long targetTick;
        private boolean isInterval;
        private long interval;
        private boolean cancelled;
        
        ScheduledTask(String handle, Value callback, String modId, long targetTick, 
                     boolean isInterval, int registrationOrder) {
            this.handle = handle;
            this.callback = callback;
            this.modId = modId;
            this.targetTick = targetTick;
            this.isInterval = isInterval;
            this.registrationOrder = registrationOrder;
        }
        
        // Getters and setters
        String handle() { return handle; }
        Value callback() { return callback; }
        String modId() { return modId; }
        int registrationOrder() { return registrationOrder; }
        long targetTick() { return targetTick; }
        void setTargetTick(long targetTick) { this.targetTick = targetTick; }
        boolean isInterval() { return isInterval; }
        void setInterval(long interval) { this.interval = interval; this.isInterval = true; }
        long getInterval() { return interval; }
        boolean isCancelled() { return cancelled; }
        void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
    }
}
