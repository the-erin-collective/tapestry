package com.tapestry;

import net.minecraft.server.MinecraftServer;

/**
 * ServerContext provides a service locator pattern for accessing the current server instance.
 * This avoids static dependencies and provides better thread safety and testability.
 */
public class ServerContext {
    
    private static final ThreadLocal<MinecraftServer> currentServer = new ThreadLocal<>();
    
    /**
     * Sets the current server instance for the calling thread.
     * This should be called by server-side components when handling requests.
     * 
     * @param server The current MinecraftServer instance
     */
    public static void setCurrentServer(MinecraftServer server) {
        currentServer.set(server);
    }
    
    /**
     * Gets the current server instance for the calling thread.
     * 
     * @return The current MinecraftServer instance, or null if not set
     */
    public static MinecraftServer getCurrentServer() {
        return currentServer.get();
    }
    
    /**
     * Clears the current server instance for the calling thread.
     * This should be called after request processing is complete.
     */
    public static void clearCurrentServer() {
        currentServer.remove();
    }
    
    /**
     * Executes a runnable with the specified server context.
     * This is a convenience method for temporary context switching.
     * 
     * @param server The server instance to use as context
     * @param runnable The code to execute with the server context
     */
    public static void withServer(MinecraftServer server, Runnable runnable) {
        MinecraftServer previousServer = currentServer.get();
        try {
            currentServer.set(server);
            runnable.run();
        } finally {
            if (previousServer != null) {
                currentServer.set(previousServer);
            } else {
                currentServer.remove();
            }
        }
    }
    
    /**
     * Checks if a server instance is available in the current context.
     * 
     * @return true if a server instance is available, false otherwise
     */
    public static boolean hasServer() {
        return currentServer.get() != null;
    }
    
    /**
     * Gets the current server instance or throws an exception if not available.
     * This is a convenience method for cases where a server is required.
     * 
     * @return The current MinecraftServer instance
     * @throws IllegalStateException if no server instance is available
     */
    public static MinecraftServer requireServer() {
        MinecraftServer server = getCurrentServer();
        if (server == null) {
            throw new IllegalStateException("No server instance available in current context");
        }
        return server;
    }
}
