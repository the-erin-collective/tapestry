package com.tapestry.gameplay.loot;

import com.tapestry.lifecycle.PhaseController;
import com.tapestry.lifecycle.TapestryPhase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for LootModifier registration and management.
 */
class LootModifierTest {
    
    private LootModifier lootModifier;
    
    @BeforeEach
    void setUp() {
        // Advance through phases to TS_REGISTER
        PhaseController.getInstance().advanceTo(TapestryPhase.DISCOVERY);
        PhaseController.getInstance().advanceTo(TapestryPhase.VALIDATION);
        PhaseController.getInstance().advanceTo(TapestryPhase.REGISTRATION);
        PhaseController.getInstance().advanceTo(TapestryPhase.FREEZE);
        PhaseController.getInstance().advanceTo(TapestryPhase.TS_LOAD);
        PhaseController.getInstance().advanceTo(TapestryPhase.TS_REGISTER);
        
        lootModifier = new LootModifier();
    }
    
    @AfterEach
    void tearDown() {
        PhaseController.reset();
    }
    
    @Test
    void testModifyWithValidParameters() {
        lootModifier.modify("minecraft:chests/simple_dungeon", table -> {
            table.replace("minecraft:bread", "minecraft:nori");
        });
        
        List<LootModification> modifications = lootModifier.getAllModifications();
        assertEquals(1, modifications.size());
        assertEquals("minecraft:chests/simple_dungeon", modifications.get(0).getTableId());
    }
    
    @Test
    void testModifyWithNullTableId() {
        assertThrows(IllegalArgumentException.class, () -> {
            lootModifier.modify(null, table -> {});
        });
    }
    
    @Test
    void testModifyWithEmptyTableId() {
        assertThrows(IllegalArgumentException.class, () -> {
            lootModifier.modify("", table -> {});
        });
    }
    
    @Test
    void testModifyWithNullModifier() {
        assertThrows(IllegalArgumentException.class, () -> {
            lootModifier.modify("minecraft:chests/simple_dungeon", null);
        });
    }
    
    @Test
    void testModifyOutsideRegisterPhase() {
        // Advance to RUNTIME phase (past TS_REGISTER)
        PhaseController.getInstance().advanceTo(TapestryPhase.TS_ACTIVATE);
        PhaseController.getInstance().advanceTo(TapestryPhase.TS_READY);
        PhaseController.getInstance().advanceTo(TapestryPhase.PERSISTENCE_READY);
        PhaseController.getInstance().advanceTo(TapestryPhase.EVENT);
        PhaseController.getInstance().advanceTo(TapestryPhase.RUNTIME);
        
        assertThrows(IllegalStateException.class, () -> {
            lootModifier.modify("minecraft:chests/simple_dungeon", table -> {});
        });
    }
    
    @Test
    void testMultipleModifications() {
        lootModifier.modify("minecraft:chests/simple_dungeon", table -> {
            table.replace("minecraft:bread", "minecraft:nori");
        });
        
        lootModifier.modify("minecraft:fishing/fish", table -> {
            table.replace("minecraft:cod", "minecraft:nori");
        });
        
        List<LootModification> modifications = lootModifier.getAllModifications();
        assertEquals(2, modifications.size());
    }
    
    @Test
    void testGetAllModificationsReturnsUnmodifiableList() {
        lootModifier.modify("minecraft:chests/simple_dungeon", table -> {});
        
        List<LootModification> modifications = lootModifier.getAllModifications();
        
        assertThrows(UnsupportedOperationException.class, () -> {
            modifications.add(new LootModification("test", table -> {}));
        });
    }
    
    @Test
    void testModificationFunctionIsStored() {
        boolean[] called = {false};
        
        lootModifier.modify("minecraft:chests/simple_dungeon", table -> {
            called[0] = true;
        });
        
        List<LootModification> modifications = lootModifier.getAllModifications();
        LootModification modification = modifications.get(0);
        
        // Execute the stored modifier
        modification.getModifier().accept(new LootTable() {
            @Override
            public void replace(String oldItem, String newItem) {
                // No-op for test
            }
        });
        
        assertTrue(called[0], "Modifier function should have been called");
    }
}
