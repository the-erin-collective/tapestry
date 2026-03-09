package com.tapestry.gameplay.patch;

/**
 * Tracks patch application metrics.
 */
public record PatchStatistics(
    int totalTargets,
    int totalOperations,
    int applied,
    int failed,
    int skipped,
    int noOps,
    int missingTargets
) {
    /**
     * Creates a new PatchStatistics with all counters set to zero.
     */
    public PatchStatistics() {
        this(0, 0, 0, 0, 0, 0, 0);
    }
    
    /**
     * Returns a new PatchStatistics with the applied counter incremented.
     */
    public PatchStatistics incrementApplied() {
        return new PatchStatistics(
            totalTargets, totalOperations, 
            applied + 1, failed, skipped, noOps, missingTargets
        );
    }
    
    /**
     * Returns a new PatchStatistics with the failed counter incremented.
     */
    public PatchStatistics incrementFailed() {
        return new PatchStatistics(
            totalTargets, totalOperations, 
            applied, failed + 1, skipped, noOps, missingTargets
        );
    }
    
    /**
     * Returns a new PatchStatistics with the skipped counter incremented.
     */
    public PatchStatistics incrementSkipped() {
        return new PatchStatistics(
            totalTargets, totalOperations, 
            applied, failed, skipped + 1, noOps, missingTargets
        );
    }
    
    /**
     * Returns a new PatchStatistics with the noOps counter incremented.
     */
    public PatchStatistics incrementNoOps() {
        return new PatchStatistics(
            totalTargets, totalOperations, 
            applied, failed, skipped, noOps + 1, missingTargets
        );
    }
    
    /**
     * Returns a new PatchStatistics with the missingTargets counter incremented.
     */
    public PatchStatistics incrementMissingTargets() {
        return new PatchStatistics(
            totalTargets, totalOperations, 
            applied, failed, skipped, noOps, missingTargets + 1
        );
    }
}
