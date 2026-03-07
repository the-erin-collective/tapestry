package com.tapestry.gameplay.traits;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Consumption.
 */
class ConsumptionTest {
    
    @Test
    void testValidConsumptionCreation() {
        Consumption consumption = new Consumption("fish_food", "minecraft:cat", "food");
        
        assertEquals("fish_food", consumption.getTraitName());
        assertEquals("minecraft:cat", consumption.getEntity());
        assertEquals("food", consumption.getBehavior());
    }
    
    @Test
    void testConsumptionWithDifferentBehaviors() {
        Consumption foodConsumption = new Consumption("fish_food", "minecraft:cat", "food");
        Consumption breedingConsumption = new Consumption("wheat", "minecraft:cow", "breeding");
        
        assertEquals("food", foodConsumption.getBehavior());
        assertEquals("breeding", breedingConsumption.getBehavior());
    }
    
    @Test
    void testConsumptionWithDifferentEntities() {
        Consumption catConsumption = new Consumption("fish_food", "minecraft:cat", "food");
        Consumption dolphinConsumption = new Consumption("fish_food", "minecraft:dolphin", "food");
        
        assertEquals("minecraft:cat", catConsumption.getEntity());
        assertEquals("minecraft:dolphin", dolphinConsumption.getEntity());
    }
    
    @Test
    void testConsumptionWithDifferentTraits() {
        Consumption fishConsumption = new Consumption("fish_food", "minecraft:cat", "food");
        Consumption milkConsumption = new Consumption("milk_like", "minecraft:cat", "cure");
        
        assertEquals("fish_food", fishConsumption.getTraitName());
        assertEquals("milk_like", milkConsumption.getTraitName());
    }
}
