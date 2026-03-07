package com.tapestry.gameplay.items;

import org.junit.jupiter.api.Test;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ItemValidation utility class.
 */
class ItemValidationTest {
    
    @Test
    void testValidateStackSize_Valid() {
        assertDoesNotThrow(() -> ItemValidation.validateStackSize(1));
        assertDoesNotThrow(() -> ItemValidation.validateStackSize(16));
        assertDoesNotThrow(() -> ItemValidation.validateStackSize(64));
    }
    
    @Test
    void testValidateStackSize_TooSmall() {
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> ItemValidation.validateStackSize(0)
        );
        assertTrue(ex.getMessage().contains("between 1 and 64"));
    }
    
    @Test
    void testValidateStackSize_TooLarge() {
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> ItemValidation.validateStackSize(65)
        );
        assertTrue(ex.getMessage().contains("between 1 and 64"));
    }
    
    @Test
    void testValidateDurability_Valid() {
        assertDoesNotThrow(() -> ItemValidation.validateDurability(0));
        assertDoesNotThrow(() -> ItemValidation.validateDurability(100));
        assertDoesNotThrow(() -> ItemValidation.validateDurability(1000));
    }
    
    @Test
    void testValidateDurability_Negative() {
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> ItemValidation.validateDurability(-1)
        );
        assertTrue(ex.getMessage().contains("must be >= 0"));
    }
    
    @Test
    void testValidateFood_Valid() {
        FoodComponent food = new FoodComponent(10, 0.5f);
        assertDoesNotThrow(() -> ItemValidation.validateFood(food));
        
        FoodComponent minFood = new FoodComponent(0, 0.0f);
        assertDoesNotThrow(() -> ItemValidation.validateFood(minFood));
        
        FoodComponent maxFood = new FoodComponent(20, 1.0f);
        assertDoesNotThrow(() -> ItemValidation.validateFood(maxFood));
    }
    
    @Test
    void testValidateFood_Null() {
        assertDoesNotThrow(() -> ItemValidation.validateFood(null));
    }
    
    @Test
    void testValidateFood_HungerTooLow() {
        FoodComponent food = new FoodComponent(-1, 0.5f);
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> ItemValidation.validateFood(food)
        );
        assertTrue(ex.getMessage().contains("hunger must be between 0 and 20"));
    }
    
    @Test
    void testValidateFood_HungerTooHigh() {
        FoodComponent food = new FoodComponent(21, 0.5f);
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> ItemValidation.validateFood(food)
        );
        assertTrue(ex.getMessage().contains("hunger must be between 0 and 20"));
    }
    
    @Test
    void testValidateFood_SaturationTooLow() {
        FoodComponent food = new FoodComponent(10, -0.1f);
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> ItemValidation.validateFood(food)
        );
        assertTrue(ex.getMessage().contains("saturation must be between 0.0 and 1.0"));
    }
    
    @Test
    void testValidateFood_SaturationTooHigh() {
        FoodComponent food = new FoodComponent(10, 1.1f);
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> ItemValidation.validateFood(food)
        );
        assertTrue(ex.getMessage().contains("saturation must be between 0.0 and 1.0"));
    }
    
    @Test
    void testValidateItemId_Valid() {
        assertDoesNotThrow(() -> ItemValidation.validateItemId("minecraft:stone"));
        assertDoesNotThrow(() -> ItemValidation.validateItemId("mymod:custom_item"));
        assertDoesNotThrow(() -> ItemValidation.validateItemId("mod-name:item.name"));
        assertDoesNotThrow(() -> ItemValidation.validateItemId("mod_name:item_name"));
    }
    
    @Test
    void testValidateItemId_Null() {
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> ItemValidation.validateItemId(null)
        );
        assertTrue(ex.getMessage().contains("cannot be null or empty"));
    }
    
    @Test
    void testValidateItemId_Empty() {
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> ItemValidation.validateItemId("")
        );
        assertTrue(ex.getMessage().contains("cannot be null or empty"));
    }
    
    @Test
    void testValidateItemId_NoColon() {
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> ItemValidation.validateItemId("invaliditem")
        );
        assertTrue(ex.getMessage().contains("namespace:path format"));
    }
    
    @Test
    void testValidateItemId_InvalidCharacters() {
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> ItemValidation.validateItemId("mod:Item_Name")
        );
        assertTrue(ex.getMessage().contains("namespace:path format"));
    }
    
    @Test
    void testValidateItemIdUniqueness_Unique() {
        Set<String> existingIds = new HashSet<>();
        existingIds.add("minecraft:stone");
        existingIds.add("mymod:item1");
        
        assertDoesNotThrow(() -> 
            ItemValidation.validateItemIdUniqueness("mymod:item2", existingIds)
        );
    }
    
    @Test
    void testValidateItemIdUniqueness_Duplicate() {
        Set<String> existingIds = new HashSet<>();
        existingIds.add("minecraft:stone");
        existingIds.add("mymod:item1");
        
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> ItemValidation.validateItemIdUniqueness("mymod:item1", existingIds)
        );
        assertTrue(ex.getMessage().contains("already registered"));
    }
    
    @Test
    void testValidateItemOptions_Valid() {
        ItemOptions options = new ItemOptions()
            .stackSize(32)
            .durability(100)
            .food(new FoodComponent(5, 0.6f));
        
        assertDoesNotThrow(() -> ItemValidation.validateItemOptions(options));
    }
    
    @Test
    void testValidateItemOptions_Null() {
        assertDoesNotThrow(() -> ItemValidation.validateItemOptions(null));
    }
    
    @Test
    void testValidateItemOptions_InvalidStackSize() {
        ItemOptions options = new ItemOptions().stackSize(100);
        
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> ItemValidation.validateItemOptions(options)
        );
        assertTrue(ex.getMessage().contains("Stack size"));
    }
    
    @Test
    void testValidateItemOptions_InvalidDurability() {
        ItemOptions options = new ItemOptions().durability(-5);
        
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> ItemValidation.validateItemOptions(options)
        );
        assertTrue(ex.getMessage().contains("Durability"));
    }
    
    @Test
    void testValidateItemOptions_InvalidFood() {
        ItemOptions options = new ItemOptions()
            .food(new FoodComponent(25, 0.5f));
        
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> ItemValidation.validateItemOptions(options)
        );
        assertTrue(ex.getMessage().contains("hunger"));
    }
}
