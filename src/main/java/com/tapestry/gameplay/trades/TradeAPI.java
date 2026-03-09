package com.tapestry.gameplay.trades;

import com.tapestry.gameplay.patch.PatchRegistry;
import com.tapestry.gameplay.patch.PatchSet;
import com.tapestry.gameplay.patch.PatchTarget;
import com.tapestry.lifecycle.PhaseController;
import com.tapestry.lifecycle.TapestryPhase;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Public API for modifying villager trades using the Gameplay Patch Engine.
 * 
 * <p>The TradeAPI provides a fluent interface for mods to register trade modifications
 * during the TS_REGISTER phase. Builder functions are stored and executed at phase
 * completion, translating high-level operations into patch operations that are
 * registered with the PatchRegistry.</p>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * TradeAPI.modify(
 *     Identifier.of("minecraft:villager/fisherman"),
 *     trades -> {
 *         trades.remove(Map.of("input", "minecraft:cod", "level", 1));
 *         trades.replaceInput(
 *             Map.of("input", "minecraft:cod"),
 *             Identifier.of("minecraft:salmon")
 *         );
 *         trades.add(Map.of(
 *             "input", "minecraft:nautilus_shell",
 *             "output", "minecraft:emerald",
 *             "level", 2
 *         ));
 *     }
 * );
 * }</pre>
 * 
 * <p>The API follows this lifecycle:</p>
 * <ol>
 *   <li>During TS_REGISTER phase: Mods call {@link #modify} to register builder functions</li>
 *   <li>At phase completion: {@link #executeBuilders} is called to process all builders</li>
 *   <li>Each builder function is executed with a new TradeTableBuilder instance</li>
 *   <li>The builder produces a PatchSet containing all operations</li>
 *   <li>The PatchSet is registered with the PatchRegistry</li>
 * </ol>
 * 
 * @see TradeTableBuilder
 * @see PatchRegistry
 * @see PatchSet
 */
public class TradeAPI {
    private static final Logger LOGGER = LoggerFactory.getLogger(TradeAPI.class);
    
    /**
     * Stores pending builder functions to be executed at phase completion.
     */
    private static final List<PendingModification> pendingModifications = new ArrayList<>();
    
    /**
     * The PatchRegistry instance to register generated patch sets with.
     */
    private static PatchRegistry registry;
    
    /**
     * The mod identifier for the currently executing mod.
     * This should be set by the framework before mods execute.
     */
    private static Identifier currentModId;
    
    /**
     * Default priority for trade modifications when not explicitly specified.
     */
    private static final int DEFAULT_PRIORITY = 0;
    
    /**
     * Registers a trade modification for the specified profession.
     * 
     * <p>This method must be called during the TS_REGISTER phase. The builder function
     * is stored and will be executed at phase completion to generate patch operations.</p>
     * 
     * <p>The builder function receives a {@link TradeTableBuilder} instance that provides
     * methods for adding, removing, and modifying trades. All operations are accumulated
     * and translated into a {@link PatchSet} when the builder completes.</p>
     * 
     * @param professionId The identifier of the villager profession to modify
     * @param builderFunction The function that configures trade modifications
     * @throws IllegalStateException if called outside the TS_REGISTER phase
     * @throws NullPointerException if professionId or builderFunction is null
     */
    public static void modify(Identifier professionId, Consumer<TradeTableBuilder> builderFunction) {
        Objects.requireNonNull(professionId, "Profession identifier cannot be null");
        Objects.requireNonNull(builderFunction, "Builder function cannot be null");
        
        // Validate we're in the correct phase
        PhaseController phaseController = PhaseController.getInstance();
        TapestryPhase currentPhase = phaseController.getCurrentPhase();
        
        if (currentPhase != TapestryPhase.TS_REGISTER) {
            throw new IllegalStateException(
                String.format(
                    "TradeAPI.modify() can only be called during TS_REGISTER phase. Current phase: %s",
                    currentPhase
                )
            );
        }
        
        // Store the builder function for later execution
        pendingModifications.add(new PendingModification(
            professionId,
            builderFunction,
            getCurrentModId(),
            DEFAULT_PRIORITY
        ));
        
        LOGGER.debug("Registered trade modification for profession {} from mod {}", 
            professionId, getCurrentModId());
    }
    
    /**
     * Registers a trade modification with a custom priority.
     * 
     * <p>This overload allows mods to specify a custom priority value to control
     * the order in which modifications are applied. Lower priority values are
     * applied first.</p>
     * 
     * @param professionId The identifier of the villager profession to modify
     * @param builderFunction The function that configures trade modifications
     * @param priority The priority value for ordering (lower values apply first)
     * @throws IllegalStateException if called outside the TS_REGISTER phase
     * @throws NullPointerException if professionId or builderFunction is null
     */
    public static void modify(Identifier professionId, Consumer<TradeTableBuilder> builderFunction, int priority) {
        Objects.requireNonNull(professionId, "Profession identifier cannot be null");
        Objects.requireNonNull(builderFunction, "Builder function cannot be null");
        
        // Validate we're in the correct phase
        PhaseController phaseController = PhaseController.getInstance();
        TapestryPhase currentPhase = phaseController.getCurrentPhase();
        
        if (currentPhase != TapestryPhase.TS_REGISTER) {
            throw new IllegalStateException(
                String.format(
                    "TradeAPI.modify() can only be called during TS_REGISTER phase. Current phase: %s",
                    currentPhase
                )
            );
        }
        
        // Store the builder function for later execution
        pendingModifications.add(new PendingModification(
            professionId,
            builderFunction,
            getCurrentModId(),
            priority
        ));
        
        LOGGER.debug("Registered trade modification for profession {} from mod {} with priority {}", 
            professionId, getCurrentModId(), priority);
    }
    
    /**
     * Executes all stored builder functions and registers the generated patch sets.
     * 
     * <p>This method should be called at the end of the TS_REGISTER phase by the
     * framework. It processes all pending modifications in registration order:</p>
     * <ol>
     *   <li>Creates a new TradeTableBuilder for each modification</li>
     *   <li>Executes the builder function to accumulate operations</li>
     *   <li>Calls builder.build() to generate a PatchSet</li>
     *   <li>Registers the PatchSet with the PatchRegistry</li>
     * </ol>
     * 
     * <p>After execution, the pending modifications list is cleared.</p>
     * 
     * @param patchRegistry The PatchRegistry to register generated patch sets with
     * @throws NullPointerException if patchRegistry is null
     */
    public static void executeBuilders(PatchRegistry patchRegistry) {
        Objects.requireNonNull(patchRegistry, "PatchRegistry cannot be null");
        
        LOGGER.info("Executing {} pending trade modifications", pendingModifications.size());
        
        int successCount = 0;
        int failureCount = 0;
        
        for (PendingModification modification : pendingModifications) {
            try {
                // Create a new builder for this modification
                TradeTableBuilder builder = new TradeTableBuilder();
                
                // Execute the builder function
                modification.builderFunction.accept(builder);
                
                // Create the patch target
                PatchTarget<TradeTable> target = new PatchTarget<>(
                    modification.professionId,
                    TradeTable.class
                );
                
                // Build the patch set
                PatchSet<TradeTable> patchSet = builder.build(
                    modification.modId,
                    target,
                    modification.priority
                );
                
                // Register with the patch registry
                patchRegistry.register(patchSet);
                
                successCount++;
                
                LOGGER.debug("Successfully registered trade patch set for {} from mod {} ({} operations)",
                    modification.professionId, modification.modId, patchSet.operations().size());
                
            } catch (Exception e) {
                failureCount++;
                LOGGER.error("Failed to execute trade modification for {} from mod {}: {}",
                    modification.professionId, modification.modId, e.getMessage(), e);
            }
        }
        
        LOGGER.info("Trade modification execution complete: {} successful, {} failed",
            successCount, failureCount);
        
        // Clear pending modifications
        pendingModifications.clear();
    }
    
    /**
     * Sets the PatchRegistry instance to use for registration.
     * 
     * <p>This method should be called by the framework during initialization.</p>
     * 
     * @param patchRegistry The PatchRegistry instance
     */
    public static void setRegistry(PatchRegistry patchRegistry) {
        registry = patchRegistry;
    }
    
    /**
     * Sets the current mod identifier.
     * 
     * <p>This method should be called by the framework before executing mod code
     * to track which mod is registering modifications.</p>
     * 
     * @param modId The identifier of the currently executing mod
     */
    public static void setCurrentModId(Identifier modId) {
        currentModId = modId;
    }
    
    /**
     * Gets the current mod identifier.
     * 
     * <p>If no mod identifier has been set, returns a default identifier.</p>
     * 
     * @return The current mod identifier
     */
    private static Identifier getCurrentModId() {
        if (currentModId == null) {
            return Identifier.of("unknown:unknown");
        }
        return currentModId;
    }
    
    /**
     * Clears all pending modifications.
     * 
     * <p>This method is primarily for testing purposes to reset the API state.</p>
     */
    static void clearPendingModifications() {
        pendingModifications.clear();
    }
    
    /**
     * Gets the count of pending modifications.
     * 
     * <p>This method is primarily for testing purposes.</p>
     * 
     * @return The number of pending modifications
     */
    static int getPendingModificationCount() {
        return pendingModifications.size();
    }
    
    /**
     * Represents a pending trade modification to be executed at phase completion.
     */
    private static class PendingModification {
        final Identifier professionId;
        final Consumer<TradeTableBuilder> builderFunction;
        final Identifier modId;
        final int priority;
        
        PendingModification(
            Identifier professionId,
            Consumer<TradeTableBuilder> builderFunction,
            Identifier modId,
            int priority
        ) {
            this.professionId = professionId;
            this.builderFunction = builderFunction;
            this.modId = modId;
            this.priority = priority;
        }
    }
}
