package com.tapestry.gameplay.traits;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents a registered gameplay trait.
 * 
 * A trait is a named behavior classification that items can possess.
 * Traits map to Minecraft tags for vanilla compatibility.
 */
public class TraitDefinition {
    private final String name;
    private final String tag;
    private final Set<String> items;
    private final List<Consumption> consumers;
    private volatile boolean frozen;
    
    /**
     * Creates a new trait definition.
     * 
     * @param name the unique trait identifier
     * @param tag the Minecraft tag this trait maps to
     * @throws IllegalArgumentException if name or tag format is invalid
     */
    public TraitDefinition(String name, String tag) {
        validateTraitName(name);
        validateTagFormat(tag);
        
        this.name = name;
        this.tag = tag;
        this.items = new HashSet<>();
        this.consumers = new ArrayList<>();
        this.frozen = false;
    }
    
    /**
     * Validates trait name format.
     * Trait names must be non-empty and contain only lowercase letters, numbers, and underscores.
     * 
     * @param name the trait name to validate
     * @throws IllegalArgumentException if name format is invalid
     */
    private static void validateTraitName(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Trait name cannot be null or empty");
        }
        
        if (!name.matches("^[a-z0-9_]+$")) {
            throw new IllegalArgumentException(
                "Trait name '" + name + "' must contain only lowercase letters, numbers, and underscores"
            );
        }
    }
    
    /**
     * Validates tag format follows Minecraft namespace:path convention.
     * 
     * @param tag the tag to validate
     * @throws IllegalArgumentException if tag format is invalid
     */
    private static void validateTagFormat(String tag) {
        if (tag == null || tag.isEmpty()) {
            throw new IllegalArgumentException("Tag cannot be null or empty");
        }
        
        if (!tag.matches("^[a-z0-9_.\\-]+:[a-z0-9_.\\-/]+$")) {
            throw new IllegalArgumentException(
                "Tag '" + tag + "' must follow namespace:path format (e.g., 'tapestry:fish_items')"
            );
        }
    }
    
    /**
     * Gets the trait name.
     * 
     * @return the trait name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Gets the tag mapping.
     * 
     * @return the Minecraft tag
     */
    public String getTag() {
        return tag;
    }
    
    /**
     * Gets all items that possess this trait.
     * 
     * @return unmodifiable set of item identifiers
     */
    public Set<String> getItems() {
        return Collections.unmodifiableSet(items);
    }
    
    /**
     * Gets all consumers of this trait.
     * 
     * @return unmodifiable list of consumption declarations
     */
    public List<Consumption> getConsumers() {
        return Collections.unmodifiableList(consumers);
    }
    
    /**
     * Adds a consumer to this trait.
     * 
     * @param consumption the consumption declaration
     * @throws IllegalStateException if trait is frozen
     */
    public void addConsumer(Consumption consumption) {
        if (frozen) {
            throw new IllegalStateException(
                "Cannot add consumer to trait '" + name + "' after COMPOSITION phase. Trait is frozen."
            );
        }
        consumers.add(consumption);
    }
    
    /**
     * Adds an item to this trait.
     * 
     * @param itemId the item identifier
     * @throws IllegalStateException if trait is frozen
     */
    public void addItem(String itemId) {
        if (frozen) {
            throw new IllegalStateException(
                "Cannot add item to trait '" + name + "' after COMPOSITION phase. Trait is frozen."
            );
        }
        items.add(itemId);
    }
    
    /**
     * Freezes this trait definition, making it immutable.
     */
    public void freeze() {
        frozen = true;
    }
    
    /**
     * Checks if this trait is frozen.
     * 
     * @return true if frozen
     */
    public boolean isFrozen() {
        return frozen;
    }
}
