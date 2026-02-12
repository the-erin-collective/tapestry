package com.tapestry.typescript;

import java.nio.file.Path;

/**
 * Represents a discovered TypeScript mod file.
 * 
 * @param path the file path
 * @param sourceName the source name for error reporting
 * @param classpath whether this mod was discovered from classpath
 */
public record DiscoveredMod(
    Path path,
    String sourceName,
    boolean classpath
) {}
