package com.tapestry.overlay;

import com.tapestry.scheduler.SchedulerService;
import com.tapestry.typescript.TypeScriptRuntime;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Handles rendering of overlays using Fabric's HudRenderCallback.
 */
public class OverlayRenderer implements HudRenderCallback {

    private static final Logger LOGGER = LoggerFactory.getLogger(OverlayRenderer.class);
    private static OverlayRenderer instance;

    private final OverlayRegistry registry;
    private final OverlaySanitizer sanitizer;
    private final Map<String, OverlaySanitizer.UINode> latestSanitizedNodes;
    private final Map<String, AtomicBoolean> overlayEvaluationInFlight;
    private final Map<String, Long> lastSnapshotLogAtMs;
    private final Map<String, Long> lastRenderLogAtMs;
    private final Map<String, Long> lastMissingSnapshotLogAtMs;
    private final Map<String, Long> lastDrawTextLogAtMs;

    private OverlayRenderer(OverlayRegistry registry, SchedulerService schedulerService) {
        this.registry = registry;
        this.sanitizer = new OverlaySanitizer();
        this.latestSanitizedNodes = new ConcurrentHashMap<>();
        this.overlayEvaluationInFlight = new ConcurrentHashMap<>();
        this.lastSnapshotLogAtMs = new ConcurrentHashMap<>();
        this.lastRenderLogAtMs = new ConcurrentHashMap<>();
        this.lastMissingSnapshotLogAtMs = new ConcurrentHashMap<>();
        this.lastDrawTextLogAtMs = new ConcurrentHashMap<>();
    }

    /**
     * Gets the singleton instance.
     *
     * @param registry the overlay registry
     * @param schedulerService the scheduler service for client-side ticking
     * @return the overlay renderer instance
     */
    public static synchronized OverlayRenderer getInstance(OverlayRegistry registry, SchedulerService schedulerService) {
        if (instance == null) {
            instance = new OverlayRenderer(registry, schedulerService);
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
    public void onHudRender(DrawContext drawContext, RenderTickCounter tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();

        // Skip rendering if client is not ready
        if (client.getWindow() == null) {
            return;
        }

        // Queue JS overlay evaluation on TWILA-JS and render latest sanitized snapshot.
        for (OverlayRegistry.OverlayEntry entry : registry.getOverlaysInRenderOrder()) {
            if (entry.definition().isVisible()) {
                OverlayContext context = new OverlayContext(
                    drawContext.getScaledWindowWidth(),
                    drawContext.getScaledWindowHeight(),
                    0.0f, // Simple tick delta
                    null // Simple client info for now
                );

                queueOverlayEvaluation(entry, context);
                renderLatestSnapshot(drawContext, entry);
            }
        }
    }

    /**
     * Queues overlay JS execution on the TypeScript runtime thread.
     */
    private void queueOverlayEvaluation(OverlayRegistry.OverlayEntry overlayEntry, OverlayContext context) {
        String fullId = overlayEntry.fullId();
        OverlayRegistry.OverlayDefinition definition = overlayEntry.definition();

        AtomicBoolean inFlight = overlayEvaluationInFlight.computeIfAbsent(fullId, ignored -> new AtomicBoolean(false));
        if (!inFlight.compareAndSet(false, true)) {
            return;
        }

        TypeScriptRuntime.executeTaskSafely(() -> {
            try {
                Value result = definition.getRenderFunction().execute(createOverlayContextProxy(context));
                if (result == null || result.isNull()) {
                    latestSanitizedNodes.remove(fullId);
                    return;
                }

                Object sanitized = sanitizer.sanitizeUINode(result);
                if (sanitized instanceof OverlaySanitizer.UINode uiNode) {
                    latestSanitizedNodes.put(fullId, uiNode);
                    if (shouldLogNow(lastSnapshotLogAtMs, fullId, 1500)) {
                        LOGGER.info("[TWILA][OverlayDebug] Snapshot updated: id={}, type={}, relX={}, relY={}, content={}",
                            fullId, uiNode.type, uiNode.x, uiNode.y, uiNode.content);
                    }
                } else {
                    latestSanitizedNodes.remove(fullId);
                    LOGGER.warn("[TWILA] Render output for overlay '{}' is not a UINode", fullId);
                }
            } catch (Exception e) {
                latestSanitizedNodes.remove(fullId);
                LOGGER.error("[JS ERROR] [TWILA] Overlay render failed: {}", e.getMessage(), e);
            } finally {
                inFlight.set(false);
            }
        });
    }

    /**
     * Renders the latest sanitized node snapshot for an overlay.
     */
    private void renderLatestSnapshot(DrawContext drawContext, OverlayRegistry.OverlayEntry overlayEntry) {
        String fullId = overlayEntry.fullId();
        OverlaySanitizer.UINode node = latestSanitizedNodes.get(fullId);
        if (node != null) {
            if (shouldLogNow(lastRenderLogAtMs, fullId, 1500)) {
                int[] abs = calculateAnchorPosition(node.x, node.y, overlayEntry.definition().getAnchor(), drawContext);
                LOGGER.info(
                    "[TWILA][OverlayDebug] Rendering snapshot: id={}, anchor={}, rel=({}, {}), abs=({}, {}), screen={}x{}, type={}, content={}",
                    fullId,
                    overlayEntry.definition().getAnchor(),
                    node.x, node.y,
                    abs[0], abs[1],
                    drawContext.getScaledWindowWidth(),
                    drawContext.getScaledWindowHeight(),
                    node.type,
                    node.content
                );
            }
            renderUINode(drawContext, node, overlayEntry.definition().getAnchor());
        } else if (shouldLogNow(lastMissingSnapshotLogAtMs, fullId, 1500)) {
            LOGGER.info("[TWILA][OverlayDebug] No snapshot available yet for id={}", fullId);
        }
    }

    /**
     * Simple per-overlay time gate to keep debug logs readable.
     */
    private boolean shouldLogNow(Map<String, Long> gate, String key, long intervalMs) {
        long now = System.currentTimeMillis();
        Long last = gate.get(key);
        if (last == null || now - last >= intervalMs) {
            gate.put(key, now);
            return true;
        }
        return false;
    }

    /**
     * Creates a proxy object for the overlay context that can be passed to JavaScript.
     *
     * @param context the overlay context
     * @return a proxy object for JavaScript
     */
    private Object createOverlayContextProxy(OverlayContext context) {
        Map<String, Object> members = new HashMap<>();
        members.put("screenWidth", context.getScreenWidth());
        members.put("screenHeight", context.getScreenHeight());
        members.put("tickDelta", context.getTickDelta());
        members.put("client", context.getClient());
        members.put("getScreenWidth", (ProxyExecutable) args -> context.getScreenWidth());
        members.put("getScreenHeight", (ProxyExecutable) args -> context.getScreenHeight());
        members.put("getTickDelta", (ProxyExecutable) args -> context.getTickDelta());
        members.put("getClient", (ProxyExecutable) args -> context.getClient());
        return ProxyObject.fromMap(members);
    }

    /**
     * Renders a UI node to the screen.
     *
     * @param drawContext the draw context
     * @param node the UI node to render
     * @param anchor the anchor point for positioning
     */
    private void renderUINode(DrawContext drawContext, OverlaySanitizer.UINode node, String anchor) {
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
                    // For center-based anchors, center text width around anchor X.
                    int drawX = x;
                    MinecraftClient client = MinecraftClient.getInstance();
                    if (isCenterHorizontalAnchor(anchor) && client.textRenderer != null) {
                        drawX = x - (client.textRenderer.getWidth(node.content) / 2);
                    }
                    drawText(drawContext, node.content, drawX, y, node.color);
                }
                break;
        }
    }

    private boolean isCenterHorizontalAnchor(String anchor) {
        return "TOP_CENTER".equals(anchor)
            || "CENTER".equals(anchor)
            || "BOTTOM_CENTER".equals(anchor);
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
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.textRenderer == null) {
            if (shouldLogNow(lastDrawTextLogAtMs, "textRenderer-null", 1500)) {
                LOGGER.warn("[TWILA][OverlayDebug] textRenderer is null, skipping draw");
            }
            return;
        }

        int colorInt = parseColor(color, 0xFFFFFF);
        int textWidth = client.textRenderer.getWidth(text);
        int textHeight = 9;

        // Draw a high-contrast backdrop so text remains visible against world and shaders.
        int bgPadding = 2;
        int bgColor = 0xA0000000;
        drawContext.fill(
            x - bgPadding,
            y - bgPadding,
            x + textWidth + bgPadding,
            y + textHeight + bgPadding,
            bgColor
        );

        // Use shadow for additional contrast.
        drawContext.drawText(client.textRenderer, text, x, y, colorInt, true);

        if (shouldLogNow(lastDrawTextLogAtMs, "drawText", 1500)) {
            LOGGER.info(
                "[TWILA][OverlayDebug] drawText executed: text='{}', pos=({}, {}), width={}, color=0x{}",
                text,
                x,
                y,
                textWidth,
                Integer.toHexString(colorInt)
            );
        }
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

}
