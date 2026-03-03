package com.tapestry.overlay.commands;

import com.tapestry.overlay.DrawCommand;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Command for rendering text on the overlay.
 */
public class TextCommand implements DrawCommand {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(TextCommand.class);
    
    private final String text;
    private final int x;
    private final int y;
    private final int color;
    
    public TextCommand(String text, int x, int y, int color) {
        this.text = text;
        this.x = x;
        this.y = y;
        this.color = color;
    }
    
    @Override
    public void execute(DrawContext ctx) {
        LOGGER.info("=== TEXT COMMAND: Rendering '{}' at ({}, {}) with color 0x{:06X} ===", text, x, y, color);
        LOGGER.info("=== TEXT COMMAND: Screen dimensions: {}x{} ===", 
                   MinecraftClient.getInstance().getWindow().getScaledWidth(),
                   MinecraftClient.getInstance().getWindow().getScaledHeight());
        LOGGER.info("=== TEXT COMMAND: Text renderer available: {} ===", 
                   MinecraftClient.getInstance().textRenderer != null);
        
        try {
            ctx.drawText(
                MinecraftClient.getInstance().textRenderer,
                text,
                x,
                y,
                color,
                false
            );
            LOGGER.info("=== TEXT COMMAND: drawText completed successfully ===");
        } catch (Exception e) {
            LOGGER.error("=== TEXT COMMAND: Error during drawText ===", e);
        }
    }
    
    @Override
    public String getType() {
        return "Text";
    }
    
    // Getters for debugging
    public String getText() { return text; }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getColor() { return color; }
}
