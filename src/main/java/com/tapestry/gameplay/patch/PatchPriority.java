package com.tapestry.gameplay.patch;

/**
 * Standard priority constants for patch application ordering.
 * Patches are applied in ascending order by priority value.
 */
public final class PatchPriority {
    /**
     * Very early priority (-1000). Use for patches that must run before most other patches.
     */
    public static final int VERY_EARLY = -1000;
    
    /**
     * Early priority (-500). Use for patches that should run early in the sequence.
     */
    public static final int EARLY = -500;
    
    /**
     * Normal priority (0). This is the default priority when none is specified.
     */
    public static final int NORMAL = 0;
    
    /**
     * Late priority (500). Use for patches that should run late in the sequence.
     */
    public static final int LATE = 500;
    
    /**
     * Very late priority (1000). Use for patches that must run after most other patches.
     */
    public static final int VERY_LATE = 1000;
    
    private PatchPriority() {
        // Prevent instantiation
    }
}
