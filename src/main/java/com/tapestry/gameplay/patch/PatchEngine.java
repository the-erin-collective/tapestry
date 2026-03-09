package com.tapestry.gameplay.patch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.function.Function; // used by wrapper support

/**
 * Core engine that applies patches to gameplay data.
 * 
 * <p>The PatchEngine is responsible for applying patch operations to gameplay
 * data objects in a deterministic order. It accepts a pre-compiled {@link PatchPlan},
 * evaluates conditions using a {@link PatchContext}, and applies operations while
 * handling errors according to the configured mode.</p>
 * 
 * <p>The engine supports two error handling modes:</p>
 * <ul>
 *   <li><b>Lenient mode (default):</b> Logs errors and continues applying remaining patches</li>
 *   <li><b>Strict mode:</b> Throws exceptions immediately on any failure</li>
 * </ul>
 * 
 * <p>Configuration options:</p>
 * <ul>
 *   <li><b>tapestry.strictPatches</b> (boolean, default false): Enable strict mode</li>
 *   <li><b>tapestry.debugPatches</b> (boolean, default false): Enable debug logging and no-op detection</li>
 *   <li><b>tapestry.patchPerformanceThreshold</b> (integer, default 100): Performance warning threshold in milliseconds</li>
 * </ul>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * // Create engine with configuration
 * Map<String, Object> config = Map.of(
 *     "tapestry.strictPatches", false,
 *     "tapestry.debugPatches", true,
 *     "tapestry.patchPerformanceThreshold", 100
 * );
 * PatchEngine engine = new PatchEngine(plan, context, config);
 * 
 * // Apply patches to a target
 * TradeTable modifiedTable = engine.applyPatches(tradeTarget, originalTable);
 * }</pre>
 * 
 * <p>The engine is designed to be reused across multiple patch application cycles,
 * including datapack reload. The same engine instance can apply patches to different
 * targets using the same compiled {@link PatchPlan}.</p>
 */
public class PatchEngine {
    private static final Logger LOGGER = LoggerFactory.getLogger(PatchEngine.class);
    
    private final PatchPlan plan;
    private final PatchContext context;
    private final boolean strictMode;
    private final boolean debugMode;
    private final int performanceThreshold;
    
    /**
     * Creates a new PatchEngine with the specified plan, context, and configuration.
     * 
     * <p>The configuration map should contain the following optional keys:</p>
     * <ul>
     *   <li><b>tapestry.strictPatches</b> (Boolean): Enable strict error handling (default: false)</li>
     *   <li><b>tapestry.debugPatches</b> (Boolean): Enable debug logging (default: false)</li>
     *   <li><b>tapestry.patchPerformanceThreshold</b> (Integer): Performance warning threshold in milliseconds (default: 100)</li>
     * </ul>
     * 
     * <p>In strict mode, any patch failure causes the engine to throw a
     * {@link PatchApplicationException} immediately. In lenient mode (default),
     * failures are logged and the engine continues with remaining patches.</p>
     * 
     * <p>Debug mode enables detailed logging including:</p>
     * <ul>
     *   <li>Condition evaluation results</li>
     *   <li>Individual operation execution</li>
     *   <li>No-op operation detection</li>
     *   <li>Detailed error context</li>
     * </ul>
     * 
     * <p>The performance threshold determines when warnings are logged for slow
     * patch application. If patch application takes longer than the threshold,
     * a warning is logged.</p>
     * 
     * @param plan The compiled patch plan containing sorted patch sets
     * @param context The context for evaluating patch conditions
     * @param config The configuration map containing engine settings
     * @throws NullPointerException if plan, context, or config is null
     */
    public PatchEngine(PatchPlan plan, PatchContext context, Map<String, Object> config) {
        if (plan == null) {
            throw new NullPointerException("PatchPlan cannot be null");
        }
        if (context == null) {
            throw new NullPointerException("PatchContext cannot be null");
        }
        if (config == null) {
            throw new NullPointerException("Configuration map cannot be null");
        }
        
        this.plan = plan;
        this.context = context;
        this.strictMode = getBoolean(config, "tapestry.strictPatches", false);
        this.debugMode = getBoolean(config, "tapestry.debugPatches", false);
        this.performanceThreshold = getInteger(config, "tapestry.patchPerformanceThreshold", 100);
        
        LOGGER.info("PatchEngine initialized (strictMode={}, debugMode={}, performanceThreshold={}ms)", 
            strictMode, debugMode, performanceThreshold);
    }
    
    /**
     * Retrieves a boolean value from the configuration map.
     * 
     * @param config The configuration map
     * @param key The configuration key
     * @param defaultValue The default value if the key is not present or not a boolean
     * @return The boolean value from the configuration, or the default value
     */
    private boolean getBoolean(Map<String, Object> config, String key, boolean defaultValue) {
        Object value = config.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return defaultValue;
    }
    
    /**
     * Retrieves an integer value from the configuration map.
     * 
     * @param config The configuration map
     * @param key The configuration key
     * @param defaultValue The default value if the key is not present or not an integer
     * @return The integer value from the configuration, or the default value
     */
    private int getInteger(Map<String, Object> config, String key, int defaultValue) {
        Object value = config.get(key);
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }
    
    // The applyPatches method will be implemented in Task 7.2


    /**
     * Applies all patches for the given target to the gameplay data object.
     *
     * <p>This method is the core of the PatchEngine. It retrieves patches from the
     * compiled {@link PatchPlan}, evaluates conditions, applies operations in order,
     * and tracks statistics.</p>
     *
     * <p>The application process:</p>
     * <ol>
     *   <li>Retrieve patches for the target from the PatchPlan</li>
     *   <li>Return unchanged data if no patches exist</li>
     *   <li>For each PatchSet:
     *     <ul>
     *       <li>Evaluate the condition using {@link PatchSet#shouldApply(PatchContext)}</li>
     *       <li>If false, log debug message and increment skipped counter</li>
     *       <li>If true, apply each operation in the operations list</li>
     *       <li>Catch exceptions and handle based on mode (lenient vs strict)</li>
     *     </ul>
     *   </li>
     *   <li>Measure and log duration</li>
     *   <li>Return the modified target</li>
     * </ol>
     *
     * <p>Error handling depends on the configured mode:</p>
     * <ul>
     *   <li><b>Lenient mode (default):</b> Logs errors and continues with remaining operations</li>
     *   <li><b>Strict mode:</b> Throws {@link PatchApplicationException} immediately on any failure</li>
     * </ul>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * PatchTarget<TradeTable> target = new PatchTarget<>(
     *     new Identifier("minecraft:villager/farmer"),
     *     TradeTable.class
     * );
     *
     * TradeTable originalTable = // ... load from game data
     * TradeTable modifiedTable = engine.applyPatches(target, originalTable);
     * }</pre>
     *
     * @param <T> The type of gameplay data being modified
     * @param target The target identifier and type information
     * @param gameplayData The gameplay data object to modify
     * @return The modified gameplay data object (same instance, mutated)
     * @throws PatchApplicationException if an operation fails in strict mode
     * @throws NullPointerException if target or gameplayData is null
     */
    public <T> T applyPatches(PatchTarget<T> target, T gameplayData) {
        return applyPatches(target, gameplayData, true);
    }

    /**
     * Applies patches to vanilla data by first wrapping it in a {@link com.tapestry.gameplay.model.TapestryModel}.
     *
     * <p>This convenience method simplifies the common pattern where mods prefer to work against
     * a higher-level model object rather than the raw vanilla type. The provided {@code wrapper}
     * function creates a wrapper instance around the vanilla data. After patches have been applied
     * to the wrapper, {@code unwrap()} is called and the resulting vanilla object is returned.
     *
     * @param <T> the vanilla data type
     * @param <W> the wrapper type which extends {@link com.tapestry.gameplay.model.TapestryModel}
     * @param target the patch target describing the identifier and wrapper class
     * @param vanillaData the vanilla data object to modify
     * @param wrapper factory that produces a wrapper from the vanilla data
     * @return the modified vanilla data object (same instance, mutated)
     * @throws NullPointerException if any argument is null
     */
    public <T, W extends com.tapestry.gameplay.model.TapestryModel<T>> T applyPatchesWithWrapper(
            PatchTarget<W> target,
            T vanillaData,
            Function<T, W> wrapper
    ) {
        if (target == null) {
            throw new NullPointerException("PatchTarget cannot be null");
        }
        if (vanillaData == null) {
            throw new NullPointerException("Gameplay data cannot be null");
        }
        if (wrapper == null) {
            throw new NullPointerException("Wrapper function cannot be null");
        }

        W wrapped = wrapper.apply(vanillaData);
        // applyPatches will mutate the wrapped object and return the same instance
        W result = applyPatches(target, wrapped, true);
        return result.unwrap();
    }

    /**
     * Applies all patches for the given target to the gameplay data object.
     *
     * <p>This overload allows specifying whether the target exists in the gameplay data.
     * If the target does not exist, the behavior depends on the configured mode:</p>
     * <ul>
     *   <li><b>Lenient mode (default):</b> Logs a warning and skips all operations for the target</li>
     *   <li><b>Strict mode:</b> Throws {@link PatchApplicationException}</li>
     * </ul>
     *
     * @param <T> The type of gameplay data being modified
     * @param target The target identifier and type information
     * @param gameplayData The gameplay data object to modify (may be null if targetExists is false)
     * @param targetExists Whether the target exists in the gameplay data
     * @return The modified gameplay data object (same instance, mutated), or null if target doesn't exist
     * @throws PatchApplicationException if target is missing in strict mode, or if an operation fails in strict mode
     * @throws NullPointerException if target is null, or if gameplayData is null when targetExists is true
     */
    public <T> T applyPatches(PatchTarget<T> target, T gameplayData, boolean targetExists) {
        if (target == null) {
            throw new NullPointerException("PatchTarget cannot be null");
        }
        if (targetExists && gameplayData == null) {
            throw new NullPointerException("Gameplay data cannot be null when target exists");
        }

        // Retrieve patches for this target
        List<PatchSet<T>> patches = plan.getPatchesFor(target);

        // If no patches, return unchanged data
        if (patches.isEmpty()) {
            LOGGER.debug("No patches registered for target: {}", target.id());
            return gameplayData;
        }

        // Create statistics tracker
        PatchStatistics stats = new PatchStatistics();

        // Check if target exists
        if (!targetExists) {
            // Increment missing target count
            stats = stats.incrementMissingTargets();
            handleMissingTarget(target, patches);
            // Log summary even for missing targets to track statistics
            logSummary(target, stats, 0);
            return gameplayData;
        }
        long startTime = System.nanoTime();

        LOGGER.debug("Applying {} patch sets to target: {}", patches.size(), target.id());

        // Apply each patch set
        for (PatchSet<T> patchSet : patches) {
            // Evaluate condition
            if (!patchSet.shouldApply(context)) {
                if (debugMode) {
                    LOGGER.debug("Skipping patch set from {} for target {} (condition not met)",
                        patchSet.modId(), target.id());
                }
                stats = stats.incrementSkipped();
                continue;
            }

            if (debugMode) {
                LOGGER.debug("Applying patch set from {} to target {} ({} operations)",
                    patchSet.modId(), target.id(), patchSet.operations().size());
            }

            // Apply each operation in the patch set
            for (PatchOperation<T> operation : patchSet.operations()) {
                try {
                    if (debugMode) {
                        LOGGER.debug("Applying operation: {} from mod: {}",
                            operation.getDebugId().orElse(operation.getClass().getSimpleName()),
                            patchSet.modId());
                    }

                    // Copy state before operation for no-op detection (only in debug mode)
                    String stateBefore = debugMode ? captureState(gameplayData) : null;

                    operation.apply(gameplayData);
                    stats = stats.incrementApplied();

                    // Detect no-op operations (only in debug mode)
                    if (debugMode && isNoOp(stateBefore, gameplayData)) {
                        LOGGER.debug("No-op operation detected: {} on {} from {}",
                            operation.getDebugId().orElse(operation.getClass().getSimpleName()),
                            target.id(),
                            patchSet.modId());
                        stats = stats.incrementNoOps();
                    }

                } catch (Exception e) {
                    handlePatchError(patchSet, operation, target, e);
                    stats = stats.incrementFailed();
                }
            }
        }

        // Measure duration
        long duration = System.nanoTime() - startTime;

        // Log summary
        logSummary(target, stats, duration);

        return gameplayData;
    }

    /**
     * Handles errors that occur during patch application.
     *
     * <p>In lenient mode (default), logs the error with full context and allows
     * execution to continue. In strict mode, throws a {@link PatchApplicationException}
     * immediately.</p>
     *
     * @param patchSet The patch set containing the failed operation
     * @param operation The operation that failed
     * @param target The target being modified
     * @param error The exception that occurred
     * @throws PatchApplicationException if strict mode is enabled
     */
    private void handlePatchError(PatchSet<?> patchSet,
                                  PatchOperation<?> operation,
                                  PatchTarget<?> target,
                                  Exception error) {
        String operationId = operation.getDebugId()
            .orElse(operation.getClass().getSimpleName());

        String message = String.format(
            "Error applying patch from mod %s to target %s (operation: %s): %s",
            patchSet.modId(),
            target.id(),
            operationId,
            error.getMessage()
        );

        LOGGER.error(message, error);

        if (strictMode) {
            throw new PatchApplicationException(message, error);
        }
    }

    /**
     * Handles the case where a patch target does not exist in the gameplay data.
     *
     * <p>In lenient mode (default), logs a warning message including the mod identifiers
     * and target identifier, then skips all operations for that target. In strict mode,
     * throws a {@link PatchApplicationException}.</p>
     *
     * <p>This method is called when the target identifier does not exist in the gameplay
     * data, allowing mods to register patches for optional content without causing errors.</p>
     *
     * @param target The missing target
     * @param patches The patch sets that would have been applied to the target
     * @throws PatchApplicationException if strict mode is enabled
     */
    private <T> void handleMissingTarget(PatchTarget<T> target, List<PatchSet<T>> patches) {
        // Collect mod identifiers for logging
        StringBuilder modIds = new StringBuilder();
        for (int i = 0; i < patches.size(); i++) {
            if (i > 0) {
                modIds.append(", ");
            }
            modIds.append(patches.get(i).modId());
        }

        String message = String.format(
            "Target %s does not exist in gameplay data. Skipping %d patch set(s) from: %s",
            target.id(),
            patches.size(),
            modIds.toString()
        );

        LOGGER.warn(message);

        if (strictMode) {
            throw new PatchApplicationException(message, null);
        }
    }

    /**
     * Captures the state of a gameplay data object for no-op detection.
     * 
     * <p>This method creates a string representation of the object's state
     * that can be compared after an operation to detect if any changes occurred.
     * The implementation uses the object's toString() method, which should provide
     * a meaningful representation of the object's state.</p>
     * 
     * <p>This method is only called when debug mode is enabled to avoid
     * performance overhead in production.</p>
     * 
     * @param <T> The type of gameplay data
     * @param gameplayData The gameplay data object to capture
     * @return A string representation of the object's state
     */
    private <T> String captureState(T gameplayData) {
        if (gameplayData == null) {
            return "null";
        }
        
        // Use toString() for state representation
        // This assumes gameplay data objects implement meaningful toString() methods
        return gameplayData.toString();
    }
    
    /**
     * Determines if an operation was a no-op by comparing state before and after.
     * 
     * <p>An operation is considered a no-op if the gameplay data object's state
     * is identical before and after the operation. This is detected by comparing
     * string representations of the object state.</p>
     * 
     * <p>This method is only called when debug mode is enabled to avoid
     * performance overhead in production.</p>
     * 
     * @param <T> The type of gameplay data
     * @param stateBefore The state captured before the operation
     * @param gameplayData The gameplay data object after the operation
     * @return true if the operation was a no-op, false otherwise
     */
    private <T> boolean isNoOp(String stateBefore, T gameplayData) {
        if (stateBefore == null) {
            return false;
        }
        
        String stateAfter = captureState(gameplayData);
        return stateBefore.equals(stateAfter);
    }

    /**
     * Logs a summary of patch application results.
     *
     * <p>This method logs an info message containing the number of applied, failed,
     * skipped, and no-op operations, along with the duration in milliseconds. If the duration
     * exceeds the performance threshold (100ms), a warning is also logged.</p>
     *
     * <p>When debug mode is enabled, no-op counts are included in the summary.</p>
     *
     * <p>Example output:</p>
     * <pre>
     * INFO: Applied 42 operations to minecraft:villager/farmer (2 failed, 5 skipped, 3 no-ops) in 45ms
     * WARN: Patch application took 150ms (expected < 100ms)
     * </pre>
     *
     * @param target The target that was modified
     * @param stats The statistics collected during patch application
     * @param durationNanos The duration of patch application in nanoseconds
     */
    private void logSummary(PatchTarget<?> target, PatchStatistics stats, long durationNanos) {
        long durationMs = durationNanos / 1_000_000;

        // Build log message based on what statistics are present
        if (stats.missingTargets() > 0) {
            // Target was missing - log simplified message
            LOGGER.info("Target {} not found - skipped all operations",
                target.id());
        } else if (debugMode && stats.noOps() > 0) {
            LOGGER.info("Applied {} operations to {} ({} failed, {} skipped, {} no-ops) in {}ms",
                stats.applied(),
                target.id(),
                stats.failed(),
                stats.skipped(),
                stats.noOps(),
                durationMs);
        } else {
            LOGGER.info("Applied {} operations to {} ({} failed, {} skipped) in {}ms",
                stats.applied(),
                target.id(),
                stats.failed(),
                stats.skipped(),
                durationMs);
        }

        if (durationMs > performanceThreshold) {
            LOGGER.warn("Patch application took {}ms (expected < {}ms)", 
                durationMs, performanceThreshold);
        }
    }

}
