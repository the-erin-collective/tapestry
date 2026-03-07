package com.tapestry.gameplay.loot;

/**
 * Wrapper for loot table modification.
 * 
 * Provides methods to modify loot table contents without replacing entire files.
 */
public interface LootTable {
    /**
     * Replaces all occurrences of an item in the loot table.
     * 
     * Walks the loot table AST recursively and replaces matching item entries.
     * Handles nested structures (alternatives, groups, conditions) correctly.
     * 
     * @param oldItem the item identifier to replace
     * @param newItem the replacement item identifier
     */
    void replace(String oldItem, String newItem);
}
