package com.tapestry.overlay;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Thread-safe command buffer for recording drawing commands from JavaScript
 * and replaying them on the render thread.
 * 
 * Uses double-buffering with atomic reference swapping to prevent
 * concurrent modification between JS thread and render thread.
 */
public class CommandBuffer {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(CommandBuffer.class);
    
    // Double-buffered command lists
    private final AtomicReference<List<DrawCommand>> currentFrame;
    private final AtomicReference<List<DrawCommand>> renderFrame;
    
    // Working buffer for JS thread
    private List<DrawCommand> workingBuffer;
    
    public CommandBuffer() {
        this.currentFrame = new AtomicReference<>(new ArrayList<>());
        this.renderFrame = new AtomicReference<>(new ArrayList<>());
        this.workingBuffer = new ArrayList<>();
    }
    
    /**
     * Adds a drawing command to the current frame buffer.
     * This is called from the JS thread.
     * 
     * @param command the drawing command to add
     */
    public void addCommand(DrawCommand command) {
        workingBuffer.add(command);
        LOGGER.info("=== COMMAND BUFFER: Added command {} - total in working buffer: {} ===", 
                   command.getType(), workingBuffer.size());
    }
    
    /**
     * Clears the working buffer for a new frame.
     * This should be called at the start of each client tick.
     */
    public void beginFrame() {
        workingBuffer.clear();
        LOGGER.debug("=== COMMAND BUFFER: Began new frame - cleared working buffer ===");
    }
    
    /**
     * Swaps the working buffer with the current frame buffer.
     * This makes the recorded commands available to the render thread.
     * This should be called at the end of each client tick.
     */
    public void endFrame() {
        // Swap buffers atomically
        List<DrawCommand> previous = currentFrame.getAndSet(workingBuffer);
        renderFrame.set(previous);
        LOGGER.info("=== COMMAND BUFFER: End frame - swapped {} commands to render frame ===", 
                   workingBuffer.size());
    }
    
    /**
     * Gets an immutable snapshot of commands to render.
     * This is called from the render thread.
     * 
     * @return list of commands to execute
     */
    public List<DrawCommand> getRenderCommands() {
        List<DrawCommand> commands = renderFrame.get();
        LOGGER.info("=== COMMAND BUFFER: getRenderCommands() returning {} commands ===", commands.size());
        return commands;
    }
    
    /**
     * Gets the number of commands in the current frame.
     * Useful for debugging.
     * 
     * @return number of recorded commands
     */
    public int getCommandCount() {
        return workingBuffer.size();
    }
    
    /**
     * Clears all buffers (useful for cleanup).
     */
    public void clear() {
        workingBuffer.clear();
        currentFrame.get().clear();
        renderFrame.get().clear();
    }
}
