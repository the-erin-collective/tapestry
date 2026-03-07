package com.tapestry.gameplay.traits;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TraitDefinition.
 */
class TraitDefinitionTest {
    
    @Test
    void testValidTraitCreation() {
        TraitDefinition trait = new TraitDefinition("fish_food", "tapestry:fish_items");
        
        assertEquals("fish_food", trait.getName());
        assertEquals("tapestry:fish_items", trait.getTag());
        assertTrue(trait.getItems().isEmpty());
        assertTrue(trait.getConsumers().isEmpty());
        assertFalse(trait.isFrozen());
    }
    
    @Test
    void testValidTraitNameFormats() {
        // Valid formats
        assertDoesNotThrow(() -> new TraitDefinition("fish_food", "tapestry:fish_items"));
        assertDoesNotThrow(() -> new TraitDefinition("milk_like", "tapestry:milk_items"));
        assertDoesNotThrow(() -> new TraitDefinition("egg123", "tapestry:egg_items"));
        assertDoesNotThrow(() -> new TraitDefinition("plant_fiber_2", "tapestry:plant_fibers"));
    }
    
    @Test
    void testInvalidTraitName_Null() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new TraitDefinition(null, "tapestry:fish_items")
        );
        
        assertTrue(exception.getMessage().contains("cannot be null or empty"));
    }
    
    @Test
    void testInvalidTraitName_Empty() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new TraitDefinition("", "tapestry:fish_items")
        );
        
        assertTrue(exception.getMessage().contains("cannot be null or empty"));
    }
    
    @Test
    void testInvalidTraitName_Uppercase() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new TraitDefinition("FishFood", "tapestry:fish_items")
        );
        
        assertTrue(exception.getMessage().contains("must contain only lowercase letters"));
    }
    
    @Test
    void testInvalidTraitName_Hyphen() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new TraitDefinition("fish-food", "tapestry:fish_items")
        );
        
        assertTrue(exception.getMessage().contains("must contain only lowercase letters"));
    }
    
    @Test
    void testInvalidTraitName_Space() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new TraitDefinition("fish food", "tapestry:fish_items")
        );
        
        assertTrue(exception.getMessage().contains("must contain only lowercase letters"));
    }
    
    @Test
    void testValidTagFormats() {
        // Valid namespace:path formats
        assertDoesNotThrow(() -> new TraitDefinition("test", "tapestry:fish_items"));
        assertDoesNotThrow(() -> new TraitDefinition("test", "minecraft:cat_food"));
        assertDoesNotThrow(() -> new TraitDefinition("test", "mymod:custom_tag"));
        assertDoesNotThrow(() -> new TraitDefinition("test", "mod-name:tag-name"));
        assertDoesNotThrow(() -> new TraitDefinition("test", "mod.name:tag.name"));
        assertDoesNotThrow(() -> new TraitDefinition("test", "tapestry:items/fish"));
    }
    
    @Test
    void testInvalidTag_Null() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new TraitDefinition("fish_food", null)
        );
        
        assertTrue(exception.getMessage().contains("Tag cannot be null or empty"));
    }
    
    @Test
    void testInvalidTag_Empty() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new TraitDefinition("fish_food", "")
        );
        
        assertTrue(exception.getMessage().contains("Tag cannot be null or empty"));
    }
    
    @Test
    void testInvalidTag_NoColon() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new TraitDefinition("fish_food", "tapestry_fish_items")
        );
        
        assertTrue(exception.getMessage().contains("must follow namespace:path format"));
    }
    
    @Test
    void testInvalidTag_Uppercase() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new TraitDefinition("fish_food", "Tapestry:fish_items")
        );
        
        assertTrue(exception.getMessage().contains("must follow namespace:path format"));
    }
    
    @Test
    void testInvalidTag_Space() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new TraitDefinition("fish_food", "tapestry:fish items")
        );
        
        assertTrue(exception.getMessage().contains("must follow namespace:path format"));
    }
    
    @Test
    void testAddItem() {
        TraitDefinition trait = new TraitDefinition("fish_food", "tapestry:fish_items");
        
        trait.addItem("minecraft:cod");
        trait.addItem("minecraft:salmon");
        
        assertEquals(2, trait.getItems().size());
        assertTrue(trait.getItems().contains("minecraft:cod"));
        assertTrue(trait.getItems().contains("minecraft:salmon"));
    }
    
    @Test
    void testAddItem_Duplicate() {
        TraitDefinition trait = new TraitDefinition("fish_food", "tapestry:fish_items");
        
        trait.addItem("minecraft:cod");
        trait.addItem("minecraft:cod");
        
        // Set should only contain one instance
        assertEquals(1, trait.getItems().size());
    }
    
    @Test
    void testAddConsumer() {
        TraitDefinition trait = new TraitDefinition("fish_food", "tapestry:fish_items");
        Consumption consumption = new Consumption("fish_food", "minecraft:cat", "food");
        
        trait.addConsumer(consumption);
        
        assertEquals(1, trait.getConsumers().size());
        assertEquals(consumption, trait.getConsumers().get(0));
    }
    
    @Test
    void testAddMultipleConsumers() {
        TraitDefinition trait = new TraitDefinition("fish_food", "tapestry:fish_items");
        Consumption consumption1 = new Consumption("fish_food", "minecraft:cat", "food");
        Consumption consumption2 = new Consumption("fish_food", "minecraft:dolphin", "food");
        
        trait.addConsumer(consumption1);
        trait.addConsumer(consumption2);
        
        assertEquals(2, trait.getConsumers().size());
    }
    
    @Test
    void testFreeze() {
        TraitDefinition trait = new TraitDefinition("fish_food", "tapestry:fish_items");
        
        assertFalse(trait.isFrozen());
        
        trait.freeze();
        
        assertTrue(trait.isFrozen());
    }
    
    @Test
    void testAddItem_AfterFreeze() {
        TraitDefinition trait = new TraitDefinition("fish_food", "tapestry:fish_items");
        trait.freeze();
        
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> trait.addItem("minecraft:cod")
        );
        
        assertTrue(exception.getMessage().contains("Cannot add item"));
        assertTrue(exception.getMessage().contains("after COMPOSITION phase"));
        assertTrue(exception.getMessage().contains("frozen"));
    }
    
    @Test
    void testAddConsumer_AfterFreeze() {
        TraitDefinition trait = new TraitDefinition("fish_food", "tapestry:fish_items");
        trait.freeze();
        
        Consumption consumption = new Consumption("fish_food", "minecraft:cat", "food");
        
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> trait.addConsumer(consumption)
        );
        
        assertTrue(exception.getMessage().contains("Cannot add consumer"));
        assertTrue(exception.getMessage().contains("after COMPOSITION phase"));
        assertTrue(exception.getMessage().contains("frozen"));
    }
    
    @Test
    void testGetItems_Immutable() {
        TraitDefinition trait = new TraitDefinition("fish_food", "tapestry:fish_items");
        trait.addItem("minecraft:cod");
        
        // Should throw UnsupportedOperationException
        assertThrows(UnsupportedOperationException.class, () -> {
            trait.getItems().add("minecraft:salmon");
        });
    }
    
    @Test
    void testGetConsumers_Immutable() {
        TraitDefinition trait = new TraitDefinition("fish_food", "tapestry:fish_items");
        Consumption consumption = new Consumption("fish_food", "minecraft:cat", "food");
        trait.addConsumer(consumption);
        
        // Should throw UnsupportedOperationException
        assertThrows(UnsupportedOperationException.class, () -> {
            trait.getConsumers().add(new Consumption("fish_food", "minecraft:dolphin", "food"));
        });
    }
}
