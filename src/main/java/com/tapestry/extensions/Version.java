package com.tapestry.extensions;

import java.util.Objects;

/**
 * Simple semantic version comparison utility for Phase 3.
 * Supports only major.minor.patch format for now.
 */
public final class Version {
    
    private final int major;
    private final int minor;
    private final int patch;
    
    private Version(int major, int minor, int patch) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
    }
    
    /**
     * Parses a semantic version string.
     * 
     * @param version version string like "1.2.3"
     * @return Version instance
     * @throws IllegalArgumentException if format is invalid
     */
    public static Version parse(String version) {
        Objects.requireNonNull(version, "Version cannot be null");
        
        String[] parts = version.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid version format: " + version + " (expected major.minor.patch)");
        }
        
        try {
            int major = Integer.parseInt(parts[0].trim());
            int minor = Integer.parseInt(parts[1].trim());
            int patch = Integer.parseInt(parts[2].trim());
            return new Version(major, minor, patch);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid version numbers in: " + version, e);
        }
    }
    
    /**
     * Compares this version to another.
     * 
     * @param other version to compare to
     * @return negative if this < other, 0 if equal, positive if this > other
     */
    public int compareTo(Version other) {
        Objects.requireNonNull(other, "Other version cannot be null");
        
        int majorCompare = Integer.compare(this.major, other.major);
        if (majorCompare != 0) return majorCompare;
        
        int minorCompare = Integer.compare(this.minor, other.minor);
        if (minorCompare != 0) return minorCompare;
        
        return Integer.compare(this.patch, other.patch);
    }
    
    /**
     * Checks if this version is at least the minimum required version.
     * 
     * @param minimum minimum required version
     * @return true if this >= minimum
     */
    public boolean isAtLeast(Version minimum) {
        return compareTo(minimum) >= 0;
    }
    
    @Override
    public String toString() {
        return major + "." + minor + "." + patch;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Version other = (Version) obj;
        return major == other.major && minor == other.minor && patch == other.patch;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(major, minor, patch);
    }
}
