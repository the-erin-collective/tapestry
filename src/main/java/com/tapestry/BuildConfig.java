package com.tapestry;

/**
 * Build configuration constants generated from Gradle properties.
 * This class provides access to version information and other build-time constants.
 */
public final class BuildConfig {
    
    /**
     * The current version of Tapestry from gradle.properties.
     */
    public static final String VERSION = "0.1.2";
    
    /**
     * The Minecraft version this build targets.
     */
    public static final String MINECRAFT_VERSION = "1.21.11";
    
    /**
     * The Fabric Loader version.
     */
    public static final String LOADER_VERSION = "0.18.4";
    
    /**
     * The Fabric API version.
     */
    public static final String FABRIC_API_VERSION = "0.141.3+1.21.11";
    
    /**
     * The Java version used for compilation.
     */
    public static final String JAVA_VERSION = "21";
    
    // Private constructor to prevent instantiation
    private BuildConfig() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
    
    /**
     * Gets the version string for use in RPC handshakes.
     * @return The version string
     */
    public static String getVersion() {
        return VERSION;
    }
    
    /**
     * Gets the full version information including Minecraft version.
     * @return Full version string
     */
    public static String getFullVersion() {
        return VERSION + " (MC " + MINECRAFT_VERSION + ")";
    }
}
