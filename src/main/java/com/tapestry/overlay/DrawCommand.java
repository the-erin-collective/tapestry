package com.tapestry.overlay;

import net.minecraft.client.gui.DrawContext;

/**
 * Interface for drawing commands that can be recorded by JavaScript
 * and executed safely on the render thread.
 * 
 * This implements the Command Pattern to separate JS execution
 * from actual rendering, solving thread affinity issues.
 */
public interface DrawCommand {
    
    /**
     * Executes the drawing command on the render thread.
     * 
     * @param ctx the draw context for rendering
     */
    void execute(DrawContext ctx);
    
    /**
     * Gets the type of this command for debugging.
     * 
     * @return the command type
     */
    default String getType() {
        return this.getClass().getSimpleName();
    }
}
