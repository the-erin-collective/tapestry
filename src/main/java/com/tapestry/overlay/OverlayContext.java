package com.tapestry.overlay;

/**
 * Context provided to overlay render functions.
 * Contains read-only information about the current render state.
 */
public class OverlayContext {
    
    private final int screenWidth;
    private final int screenHeight;
    private final float tickDelta;
    private final ClientInfo client;
    
    public OverlayContext(int screenWidth, int screenHeight, float tickDelta, ClientInfo client) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.tickDelta = tickDelta;
        this.client = client;
    }
    
    /**
     * Gets the screen width in pixels.
     * 
     * @return the screen width
     */
    public int getScreenWidth() {
        return screenWidth;
    }
    
    /**
     * Gets the screen height in pixels.
     * 
     * @return the screen height
     */
    public int getScreenHeight() {
        return screenHeight;
    }
    
    /**
     * Gets the tick delta for smooth animations.
     * 
     * @return the tick delta
     */
    public float getTickDelta() {
        return tickDelta;
    }
    
    /**
     * Gets the client information snapshot.
     * 
     * @return the client info
     */
    public ClientInfo getClient() {
        return client;
    }
}
