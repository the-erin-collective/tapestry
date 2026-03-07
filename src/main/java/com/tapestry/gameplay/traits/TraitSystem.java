package com.tapestry.gameplay.traits;

import com.tapestry.lifecycle.PhaseController;
import com.tapestry.lifecycle.TapestryPhase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The trait system provides emergent cross-mod compatibility through behavioral classifications.
 * 
 * Traits are named behavior classifications that items can possess, enabling automatic
 * compatibility across mods without explicit integration code.
 * 
 * Registration is only allowed during TS_REGISTER phase. After COMPOSITION phase,
 * all trait registries are frozen and immutable.
 */
public class TraitSystem {
    private static final Logger LOGGER = LoggerFactory.getLogger(TraitSystem.class);
    
    private final Map<String, TraitDefinition> traits = new ConcurrentHashMap<>();
    private final List<Consumption> consumptions = new ArrayList<>();
    private volatile boolean frozen = false;
    
    /**
     * Registers a new gameplay trait.
     * 
     * @param name the unique trait identifier
     * @param config optional configuration (tag mapping)
     * @throws IllegalStateException if called outside TS_REGISTER phase
     * @throws IllegalArgumentException if trait name already registered
     */
    public void register(String name, TraitConfig config) {
        PhaseController.getInstance().requirePhase(TapestryPhase.TS_REGISTER);
        
        if (frozen) {
            throw new IllegalStateException(
                "Cannot register trait '" + name + "' after COMPOSITION phase. Trait registry is frozen."
            );
        }
        
        if (traits.containsKey(name)) {
            throw new IllegalArgumentException(
                "Trait '" + name + "' is already registered. Duplicate trait names are not allowed."
            );
        }
        
        String tag = config != null && config.getTag() != null 
            ? config.getTag() 
            : "tapestry:" + name + "_items";
        
        validateTagFormat(tag);
        
        TraitDefinition definition = new TraitDefinition(name, tag);
        traits.put(name, definition);
        
        LOGGER.info("Registered trait '{}' with tag '{}'", name, tag);
    }
    
    /**
     * Declares that an entity consumes a specific trait.
     * 
     * @param name the trait name
     * @param config consumption configuration (entity, behavior)
     * @throws IllegalStateException if called outside TS_REGISTER phase
     */
    public void consume(String name, ConsumptionConfig config) {
        PhaseController.getInstance().requirePhase(TapestryPhase.TS_REGISTER);
        
        if (frozen) {
            throw new IllegalStateException(
                "Cannot consume trait '" + name + "' after COMPOSITION phase. Trait registry is frozen."
            );
        }
        
        Consumption consumption = new Consumption(name, config.getEntity(), config.getBehavior());
        consumptions.add(consumption);
        
        LOGGER.info("Registered consumption: entity '{}' consumes trait '{}' for behavior '{}'",
            config.getEntity(), name, config.getBehavior());
    }
    
    /**
     * Gets a trait definition by name.
     * 
     * @param name the trait name
     * @return the trait definition, or null if not found
     */
    public TraitDefinition getTrait(String name) {
        return traits.get(name);
    }
    
    /**
     * Gets all registered traits.
     * 
     * @return unmodifiable map of trait definitions
     */
    public Map<String, TraitDefinition> getAllTraits() {
        return Collections.unmodifiableMap(traits);
    }
    
    /**
     * Gets all consumption declarations.
     * 
     * @return unmodifiable list of consumptions
     */
    public List<Consumption> getConsumptions() {
        return Collections.unmodifiableList(consumptions);
    }
    
    /**
     * Freezes the trait registry, making it immutable.
     * Called after COMPOSITION phase completes.
     */
    public void freeze() {
        frozen = true;
        traits.values().forEach(TraitDefinition::freeze);
        LOGGER.info("Trait system frozen. {} traits registered, {} consumptions declared.",
            traits.size(), consumptions.size());
    }
    
    /**
     * Checks if the trait registry is frozen.
     * 
     * @return true if frozen
     */
    public boolean isFrozen() {
        return frozen;
    }
    
    /**
     * Validates that a tag name follows Minecraft namespace:path format.
     * 
     * @param tag the tag name to validate
     * @throws IllegalArgumentException if tag format is invalid
     */
    private void validateTagFormat(String tag) {
        if (!tag.matches("^[a-z0-9_.-]+:[a-z0-9_.-]+(/[a-z0-9_.-]+)*$")) {
            throw new IllegalArgumentException(
                "Invalid tag format: '" + tag + "'. Tags must follow namespace:path format."
            );
        }
    }
}
