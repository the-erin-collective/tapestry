package com.tapestry.gameplay.composition;

import com.tapestry.gameplay.items.ItemRegistration;
import com.tapestry.gameplay.traits.TraitSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Orchestrates the COMPOSITION phase execution.
 * 
 * The COMPOSITION phase performs:
 * 1. Trait-to-item mapping resolution
 * 2. Behavior tag generation
 * 3. Registry freezing
 * 
 * This ensures all trait relationships are resolved and behavior tags are
 * generated before the INITIALIZATION phase begins.
 */
public class CompositionOrchestrator {
    private static final Logger LOGGER = LoggerFactory.getLogger(CompositionOrchestrator.class);
    
    private final TraitSystem traitSystem;
    private final ItemRegistration itemRegistration;
    private final TraitResolver traitResolver;
    private final BehaviorTagGenerator tagGenerator;
    
    /**
     * Creates a new composition orchestrator.
     * 
     * @param traitSystem the trait system
     * @param itemRegistration the item registration system
     */
    public CompositionOrchestrator(TraitSystem traitSystem, ItemRegistration itemRegistration) {
        this.traitSystem = traitSystem;
        this.itemRegistration = itemRegistration;
        this.traitResolver = new TraitResolver(traitSystem, itemRegistration);
        this.tagGenerator = new BehaviorTagGenerator(traitSystem);
    }
    
    /**
     * Executes the COMPOSITION phase.
     * 
     * This method:
     * 1. Resolves trait-to-item mappings
     * 2. Generates behavior tags for vanilla compatibility
     * 3. Freezes all trait registries
     * 
     * @throws IllegalStateException if trait resolution fails
     * @throws IOException if tag generation fails
     */
    public void executeComposition() throws IOException {
        LOGGER.info("=== COMPOSITION PHASE ===");
        LOGGER.info("Starting composition phase execution...");
        
        // Step 1: Resolve trait-to-item mappings
        LOGGER.info("Step 1: Resolving trait-to-item mappings...");
        TraitResolver.ResolutionResult resolutionResult = traitResolver.resolve();
        LOGGER.info("Trait resolution complete: {} traits, {} items, {} mappings",
            resolutionResult.getTraitCount(),
            resolutionResult.getItemCount(),
            resolutionResult.getMappingCount());
        
        // Step 2: Generate behavior tags
        LOGGER.info("Step 2: Generating behavior tags...");
        int tagCount = tagGenerator.generateTags();
        LOGGER.info("Behavior tag generation complete: {} tags generated", tagCount);
        
        // Step 3: Freeze registries
        LOGGER.info("Step 3: Freezing trait registries...");
        traitSystem.freeze();
        LOGGER.info("Trait registries frozen");
        
        LOGGER.info("COMPOSITION phase execution complete");
    }
    
    /**
     * Gets the trait resolver.
     * 
     * @return the trait resolver
     */
    public TraitResolver getTraitResolver() {
        return traitResolver;
    }
    
    /**
     * Gets the behavior tag generator.
     * 
     * @return the behavior tag generator
     */
    public BehaviorTagGenerator getTagGenerator() {
        return tagGenerator;
    }
}
