package com.tapestry.overlay;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import org.graalvm.polyglot.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles rendering of overlays using Fabric's HudRenderCallback.
 * 
 * This class integrates with Minecraft's HUD rendering pipeline to draw
 * registered overlays after the vanilla HUD is rendered.
 */
public class OverlayRenderer implements HudRenderCallback {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(OverlayRenderer.class);
    private static OverlayRenderer instance;
    
    private final OverlayRegistry registry;
    private final OverlaySanitizer sanitizer;
    
    private OverlayRenderer(OverlayRegistry registry) {
        this.registry = registry;
        this.sanitizer = new OverlaySanitizer();
    }
    
    /**
     * Gets the singleton instance.
     * 
     * @param registry the overlay registry
     * @return the overlay renderer instance
     */
    public static synchronized OverlayRenderer getInstance(OverlayRegistry registry) {
        if (instance == null) {
            instance = new OverlayRenderer(registry);
            // Register with Fabric
            HudRenderCallback.EVENT.register(instance);
            LOGGER.info("Registered overlay renderer with Fabric HudRenderCallback");
        }
        return instance;
    }
    
    /**
     * Called by Fabric during HUD rendering.
     * 
     * @param drawContext the draw context
     * @param tickDelta the tick delta for smooth animations
     */
    @Override
    public void onHudRender(DrawContext drawContext, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        
        // Skip rendering if client is not ready
        if (client.getWindow() == null) {
            return;
        }
        
        // Create overlay context
        ClientInfo clientInfo = ClientInfo.fromClient(client);
        OverlayContext context = new OverlayContext(
            drawContext.getScaledWindowWidth(),
            drawContext.getScaledWindowHeight(),
            tickDelta,
            clientInfo
        );
        
        // Get overlays in render order
        var overlays = registry.getOverlaysInRenderOrder();
        
        // Render each overlay
        for (var overlayEntry : overlays) {
            try {
                renderOverlay(drawContext, overlayEntry, context);
            } catch (Exception e) {
                // Disable the overlay permanently on error
                registry.disableOverlay(overlayEntry.fullId());
                LOGGER.error("Overlay '{}' crashed during render, disabling permanently", 
                    overlayEntry.fullId(), e);
            }
        }
    }
    
    /**
     * Renders a single overlay.
     * 
     * @param drawContext the draw context
     * @param overlayEntry the overlay to render
     * @param context the overlay context
     */
    private void renderOverlay(DrawContext drawContext, OverlayRegistry.OverlayEntry overlayEntry, 
                               OverlayContext context) {
        OverlayRegistry.OverlayDefinition definition = overlayEntry.definition();
        Value renderFunction = definition.getRenderFunction();
        
        // Execute the render function
        Value result = renderFunction.execute(
            createOverlayContextProxy(context)
        );
        
        // Sanitize and render the result
        if (result != null && !result.isNull()) {
            Object sanitized = sanitizer.sanitizeUINode(result);
            if (sanitized instanceof OverlaySanitizer.UINode) {
                renderUINode(drawContext, (OverlaySanitizer.UINode) sanitized, definition.getAnchor());
            }
        }
    }
    
    /**
     * Creates a proxy object for the overlay context that can be passed to JavaScript.
     * 
     * @param context the overlay context
     * @return a proxy object for JavaScript
     */
    private Object createOverlayContextProxy(OverlayContext context) {
        return new OverlayContextProxy(context);
    }
    
    /**
     * Renders a UI node to the screen.
     * 
     * @param drawContext the draw context
     * @param node the UI node to render
     * @param anchor the anchor point for positioning
     */
    private void renderUINode(DrawContext drawContext, OverlaySanitizer.UINode node, String anchor) {
        MatrixStack matrices = drawContext.getMatrices();
        
        // Calculate position based on anchor
        int[] position = calculateAnchorPosition(node.x, node.y, anchor, drawContext);
        int x = position[0];
        int y = position[1];
        
        switch (node.type) {
            case "box":
                if (node.color != null) {
                    // Render colored box
                    int width = convertCoordinate(node.width, drawContext.getScaledWindowWidth());
                    int height = convertCoordinate(node.height, drawContext.getScaledWindowHeight());
                    fillRect(drawContext, x, y, width, height, node.color);
                }
                
                // Render children
                if (node.children != null) {
                    for (OverlaySanitizer.UINode child : node.children) {
                        renderUINode(drawContext, child, anchor);
                    }
                }
                break;
                
            case "text":
                if (node.content != null) {
                    // Render text
                    drawText(drawContext, node.content, x, y, node.color);
                }
                break;
        }
    }
    
    /**
     * Calculates the absolute position based on anchor and relative coordinates.
     * 
     * @param relativeX the relative X coordinate (can be integer or percentage string)
     * @param relativeY the relative Y coordinate (can be integer or percentage string)
     * @param anchor the anchor point
     * @param drawContext the draw context
     * @return the absolute position [x, y]
     */
    private int[] calculateAnchorPosition(Object relativeX, Object relativeY, String anchor, 
                                         DrawContext drawContext) {
        int screenWidth = drawContext.getScaledWindowWidth();
        int screenHeight = drawContext.getScaledWindowHeight();
        
        // Convert coordinates
        int x = convertCoordinate(relativeX, screenWidth);
        int y = convertCoordinate(relativeY, screenHeight);
        
        // Adjust based on anchor
        switch (anchor) {
            case "TOP_LEFT":
                // No adjustment needed
                break;
            case "TOP_CENTER":
                x = screenWidth / 2 + x;
                break;
            case "TOP_RIGHT":
                x = screenWidth - x;
                break;
            case "CENTER":
                x = screenWidth / 2 + x;
                y = screenHeight / 2 + y;
                break;
            case "BOTTOM_LEFT":
                y = screenHeight - y;
                break;
            case "BOTTOM_CENTER":
                x = screenWidth / 2 + x;
                y = screenHeight - y;
                break;
            case "BOTTOM_RIGHT":
                x = screenWidth - x;
                y = screenHeight - y;
                break;
        }
        
        return new int[]{x, y};
    }
    
    /**
     * Converts a coordinate value (integer or percentage string) to pixels.
     * 
     * @param coord the coordinate value
     * @param screenSize the screen dimension
     * @return the coordinate in pixels
     */
    private int convertCoordinate(Object coord, int screenSize) {
        if (coord instanceof Number) {
            return ((Number) coord).intValue();
        } else if (coord instanceof String) {
            String str = ((String) coord).trim();
            if (str.endsWith("%")) {
                try {
                    double percentage = Double.parseDouble(str.substring(0, str.length() - 1));
                    return (int) (screenSize * percentage / 100.0);
                } catch (NumberFormatException e) {
                    LOGGER.warn("Invalid percentage coordinate: {}", str);
                    return 0;
                }
            } else {
                try {
                    return Integer.parseInt(str);
                } catch (NumberFormatException e) {
                    LOGGER.warn("Invalid coordinate: {}", str);
                    return 0;
                }
            }
        }
        return 0;
    }
    
    /**
     * Draws text to the screen.
     * 
     * @param drawContext the draw context
     * @param text the text to draw
     * @param x the X position
     * @param y the Y position
     * @param color the text color (hex string)
     */
    private void drawText(DrawContext drawContext, String text, int x, int y, String color) {
        int colorInt = parseColor(color, 0xFFFFFF);
        drawContext.drawText(MinecraftClient.getInstance().textRenderer, text, x, y, colorInt, false);
    }
    
    /**
     * Draws a filled rectangle.
     * 
     * @param drawContext the draw context
     * @param x the X position
     * @param y the Y position
     * @param width the width
     * @param height the height
     * @param color the fill color (hex string)
     */
    private void fillRect(DrawContext drawContext, int x, int y, int width, int height, String color) {
        int colorInt = parseColor(color, 0x000000);
        drawContext.fill(x, y, x + width, y + height, colorInt);
    }
    
    /**
     * Parses a color string to an integer.
     * 
     * @param color the color string (e.g., "#FFFFFF" or "#RRGGBBAA")
     * @param defaultColor the default color if parsing fails
     * @return the color as an integer
     */
    private int parseColor(String color, int defaultColor) {
        if (color == null || !color.startsWith("#")) {
            return defaultColor;
        }
        
        try {
            String hex = color.substring(1);
            if (hex.length() == 6) {
                // RGB format
                return Integer.parseInt(hex, 16) | 0xFF000000; // Add alpha
            } else if (hex.length() == 8) {
                // RGBA format
                return Integer.parseInt(hex, 16);
            }
        } catch (NumberFormatException e) {
            LOGGER.warn("Invalid color format: {}", color);
        }
        
        return defaultColor;
    }
    
    /**
     * Proxy object for exposing OverlayContext to JavaScript.
     */
    private static class OverlayContextProxy {
        private final OverlayContext context;
        
        public OverlayContextProxy(OverlayContext context) {
            this.context = context;
        }
        
        public int getScreenWidth() { return context.getScreenWidth(); }
        public int getScreenHeight() { return context.getScreenHeight(); }
        public float getTickDelta() { return context.getTickDelta(); }
        public ClientInfo getClient() { return context.getClient(); }
    }
}
