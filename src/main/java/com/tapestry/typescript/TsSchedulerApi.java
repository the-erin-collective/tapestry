package com.tapestry.typescript;

import com.tapestry.lifecycle.PhaseController;
import com.tapestry.lifecycle.TapestryPhase;
import com.tapestry.runtime.RuntimeContextFactory;
import com.tapestry.scheduler.SchedulerService;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * TypeScript API for scheduler operations.
 * 
 * Provides setTimeout, setInterval, nextTick, and clearInterval
 * with deterministic scheduling and fail-fast error handling.
 */
public class TsSchedulerApi {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(TsSchedulerApi.class);
    
    private final SchedulerService schedulerService;
    
    public TsSchedulerApi(SchedulerService schedulerService) {
        this.schedulerService = schedulerService;
    }
    
    /**
     * Creates the scheduler namespace object.
     * 
     * @return ProxyObject with scheduler functions
     */
    public ProxyObject createNamespace() {
        Map<String, Object> scheduler = new HashMap<>();
        
        // setTimeout function
        scheduler.put("setTimeout", (ProxyExecutable) args -> {
            if (args.length != 2) {
                throw new IllegalArgumentException("setTimeout requires exactly 2 arguments: (callback, delay)");
            }
            
            Value callback = args[0];
            Number delay = args[1].isNumber() ? args[1].asDouble() : 0;
            
            if (callback == null || !callback.canExecute()) {
                throw new IllegalArgumentException("First argument must be an executable function");
            }
            
            if (delay == null) {
                throw new IllegalArgumentException("Second argument must be a number");
            }
            
            String modId = TypeScriptRuntime.getCurrentModId();
            if (modId == null) {
                throw new IllegalStateException("No mod ID set in current context");
            }
            
            return schedulerService.setTimeout(callback, delay.longValue(), modId);
        });
        
        // setInterval function
        scheduler.put("setInterval", (ProxyExecutable) args -> {
            if (args.length != 2) {
                throw new IllegalArgumentException("setInterval requires exactly 2 arguments: (callback, interval)");
            }
            
            Value callback = args[0];
            Number interval = args[1].isNumber() ? args[1].asDouble() : 0;
            
            if (callback == null || !callback.canExecute()) {
                throw new IllegalArgumentException("First argument must be an executable function");
            }
            
            if (interval == null) {
                throw new IllegalArgumentException("Second argument must be a number");
            }
            
            String modId = TypeScriptRuntime.getCurrentModId();
            if (modId == null) {
                throw new IllegalStateException("No mod ID set in current context");
            }
            
            return schedulerService.setInterval(callback, interval.longValue(), modId);
        });
        
        // clearInterval function
        scheduler.put("clearInterval", (ProxyExecutable) args -> {
            if (args.length != 1) {
                throw new IllegalArgumentException("clearInterval requires exactly 1 argument: (handle)");
            }
            
            String handle = args[0].asString();
            if (handle == null) {
                throw new IllegalArgumentException("Handle must be a string");
            }
            
            String modId = TypeScriptRuntime.getCurrentModId();
            if (modId == null) {
                throw new IllegalStateException("No mod ID set in current context");
            }
            
            schedulerService.clearInterval(handle, modId);
            return null;
        });
        
        // nextTick function
        scheduler.put("nextTick", (ProxyExecutable) args -> {
            if (args.length != 1) {
                throw new IllegalArgumentException("nextTick requires exactly 1 argument: (callback)");
            }
            
            Value callback = args[0];
            
            if (callback == null || !callback.canExecute()) {
                throw new IllegalArgumentException("Argument must be an executable function");
            }
            
            String modId = TypeScriptRuntime.getCurrentModId();
            if (modId == null) {
                throw new IllegalStateException("No mod ID set in current context");
            }
            
            return schedulerService.nextTick(callback, modId);
        });
        
        return ProxyObject.fromMap(scheduler);
    }
}
