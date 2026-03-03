package com.tapestry.overlay.commands;

import com.tapestry.overlay.DrawCommand;
import net.minecraft.client.gui.DrawContext;

/**
 * Command for rendering a filled rectangle on the overlay.
 */
public class BoxCommand implements DrawCommand {
    
    private final int x;
    private final int y;
    private final int width;
    private final int height;
    private final int color;
    
    public BoxCommand(int x, int y, int width, int height, int color) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.color = color;
    }
    
    @Override
    public void execute(DrawContext ctx) {
        ctx.fill(x, y, x + width, y + height, color);
    }
    
    @Override
    public String getType() {
        return "Box";
    }
    
    // Getters for debugging
    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public int getColor() { return color; }
}
