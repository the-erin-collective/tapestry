/**
 * Core patch engine infrastructure for modifying gameplay data.
 * 
 * <p>The patch engine provides a unified, type-safe system for applying structured
 * modifications to gameplay data objects. It ensures deterministic mod compatibility,
 * supports datapack reload, and provides comprehensive error handling.</p>
 * 
 * <p>Key components:</p>
 * <ul>
 *   <li>{@link com.tapestry.gameplay.patch.PatchTarget} - Identifies gameplay objects to modify</li>
 *   <li>{@link com.tapestry.gameplay.patch.PatchOperation} - Interface for single modifications</li>
 *   <li>{@link com.tapestry.gameplay.patch.PatchStatistics} - Tracks patch application metrics</li>
 *   <li>{@link com.tapestry.gameplay.patch.PatchPriority} - Standard priority constants</li>
 * </ul>
 * 
 * <p>Exception types:</p>
 * <ul>
 *   <li>{@link com.tapestry.gameplay.patch.PatchApplicationException} - General patch failures</li>
 *   <li>{@link com.tapestry.gameplay.patch.PatchTypeMismatchException} - Type safety violations</li>
 * </ul>
 */
package com.tapestry.gameplay.patch;
