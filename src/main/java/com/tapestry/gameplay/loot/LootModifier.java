package com.tapestry.gameplay.loot;

import com.tapestry.lifecycle.PhaseController;
import com.tapestry.lifecycle.TapestryPhase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * Provides safe loot table modification without file replacement.
 * 
 * Loot modifiers allow mods to modify existing loot tables without replacing
 * entire files, maintaining compatibility with other mods.
 */
public class LootModifier {
    private static final Logger LOGGER = LoggerFactory.getLogger(LootModifier.class);
    
    // Global instance for TypeScript API access
    private static LootModifier instance;
    
    private final List<LootModification> modifications = new ArrayList<>();
    
    /**
     * Gets the global LootModifier instance.
     * 
     * @return the global instance
     */
    public static synchronized LootModifier getInstance() {
        if (instance == null) {
            instance = new LootModifier();
        }
        return instance;
    }
    
    /**
     * Registers a loot table modification.
     * 
     * @param tableId the loot table identifier
     * @param modifier the modification function
     * @throws IllegalStateException if called outside TS_REGISTER phase
     */
    public void modify(String tableId, Consumer<LootTable> modifier) {
        PhaseController.getInstance().requirePhase(TapestryPhase.TS_REGISTER);
        
        if (tableId == null || tableId.isEmpty()) {
            throw new IllegalArgumentException("Loot table ID cannot be null or empty");
        }
        
        if (modifier == null) {
            throw new IllegalArgumentException("Modifier function cannot be null");
        }
        
        LootModification modification = new LootModification(tableId, modifier);
        modifications.add(modification);
        
        LOGGER.info("Registered loot modification for table: {}", tableId);
    }
    
    /**
     * Gets all registered loot modifications.
     * 
     * @return unmodifiable list of modifications
     */
    public List<LootModification> getAllModifications() {
        return Collections.unmodifiableList(modifications);
    }
}
