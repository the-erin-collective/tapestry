package com.tapestry.gameplay.loot;

import java.util.function.Consumer;

/**
 * Represents a registered loot table modification.
 * 
 * Stores the table identifier and modification function.
 */
public class LootModification {
    private final String tableId;
    private final Consumer<LootTable> modifier;
    
    /**
     * Creates a loot modification.
     * 
     * @param tableId the loot table identifier
     * @param modifier the modification function
     */
    public LootModification(String tableId, Consumer<LootTable> modifier) {
        this.tableId = tableId;
        this.modifier = modifier;
    }
    
    /**
     * Gets the loot table identifier.
     * 
     * @return the table ID
     */
    public String getTableId() {
        return tableId;
    }
    
    /**
     * Gets the modification function.
     * 
     * @return the modifier
     */
    public Consumer<LootTable> getModifier() {
        return modifier;
    }
}
