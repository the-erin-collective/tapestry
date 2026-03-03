package com.tapestry.overlay;

import com.tapestry.overlay.commands.BoxCommand;
import com.tapestry.overlay.commands.TextCommand;
import org.graalvm.polyglot.HostAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Context object that records drawing commands instead of executing them.
 * 
 * This is passed to JavaScript overlay scripts instead of the actual
 * DrawContext, allowing JS to run on any thread while rendering happens
 * safely on the render thread.
 */
public class CommandRecordingContext {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(CommandRecordingContext.class);
    
    private final CommandBuffer commandBuffer;
    private final int screenWidth;
    private final int screenHeight;
    
    public CommandRecordingContext(CommandBuffer commandBuffer, int screenWidth, int screenHeight) {
        this.commandBuffer = commandBuffer;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
    }
    
    /**
     * Records a text drawing command.
     * 
     * @param text text to draw
     * @param x X position
     * @param y Y position
     */
    @HostAccess.Export
    public void text(String text, int x, int y) {
        text(text, x, y, 0xFFFFFF); // Default white color
    }
    
    /**
     * Records a text drawing command with color.
     * 
     * @param text text to draw
     * @param x X position
     * @param y Y position
     * @param color color in hex format (e.g., 0xFFFFFF for white)
     */
    @HostAccess.Export
    public void text(String text, int x, int y, int color) {
        LOGGER.info("=== COMMAND RECORDING: Recording text command: '{}' at ({}, {}) with color 0x{:06X} ===", text, x, y, color);
        commandBuffer.addCommand(new TextCommand(text, x, y, color));
        LOGGER.info("=== COMMAND RECORDING: Text command added to buffer ===");
    }
    
    /**
     * Records a filled rectangle drawing command.
     * 
     * @param x the X position
     * @param y the Y position
     * @param width the rectangle width
     * @param height the rectangle height
     * @param color the fill color in hex format
     */
    @HostAccess.Export
    public void box(int x, int y, int width, int height, int color) {
        LOGGER.debug("Recording box command: ({}, {}, {}, {}) with color 0x{:06X}", x, y, width, height, color);
        commandBuffer.addCommand(new BoxCommand(x, y, width, height, color));
    }
    
    /**
     * Records a filled rectangle drawing command with default color.
     * 
     * @param x the X position
     * @param y the Y position
     * @param width the rectangle width
     * @param height the rectangle height
     */
    @HostAccess.Export
    public void box(int x, int y, int width, int height) {
        box(x, y, width, height, 0x000000); // Default black color
    }
    
    /**
     * Gets the screen width for positioning calculations.
     * 
     * @return screen width in pixels
     */
    @HostAccess.Export
    public int getScreenWidth() {
        return screenWidth;
    }
    
    /**
     * Gets the screen height for positioning calculations.
     * 
     * @return screen height in pixels
     */
    @HostAccess.Export
    public int getScreenHeight() {
        return screenHeight;
    }
    
    /**
     * Calculates center X position.
     * 
     * @param offsetX offset from center
     * @return center X position
     */
    @HostAccess.Export
    public int getCenterX(int offsetX) {
        return screenWidth / 2 + offsetX;
    }
    
    /**
     * Calculates center Y position.
     * 
     * @param offsetY offset from center
     * @return center Y position
     */
    @HostAccess.Export
    public int getCenterY(int offsetY) {
        return screenHeight / 2 + offsetY;
    }
}
