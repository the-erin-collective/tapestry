package com.tapestry.gameplay.items;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ItemDefinition data model.
 */
class ItemDefinitionTest {
    
    @Test
    void testItemDefinition_BasicProperties() {
        ItemOptions options = new ItemOptions()
            .stackSize(16)
            .traits("fish_food", "plant_fiber");
        
        ItemDefinition item = new ItemDefinition("mymod:nori", options);
        
        assertEquals("mymod:nori", item.getId());
        assertEquals(16, item.getStackSize());
        assertArrayEquals(new String[]{"fish_food", "plant_fiber"}, item.getTraits());
        assertFalse(item.isFood());
        assertFalse(item.isDurable());
        assertFalse(item.hasRecipeRemainder());
        assertFalse(item.hasUseHandler());
    }
    
    @Test
    void testItemDefinition_DefaultStackSize() {
        ItemDefinition item = new ItemDefinition("mymod:item", new ItemOptions());
        assertEquals(64, item.getStackSize());
    }
    
    @Test
    void testItemDefinition_NullOptions() {
        ItemDefinition item = new ItemDefinition("mymod:item", null);
        assertEquals(64, item.getStackSize());
        assertArrayEquals(new String[0], item.getTraits());
        assertNull(item.getFood());
        assertNull(item.getDurability());
        assertNull(item.getRecipeRemainder());
        assertNull(item.getUseHandler());
    }
    
    @Test
    void testItemDefinition_FoodItem() {
        FoodComponent food = new FoodComponent(10, 0.6f, true, false);
        ItemOptions options = new ItemOptions().food(food);
        
        ItemDefinition item = new ItemDefinition("mymod:food", options);
        
        assertTrue(item.isFood());
        assertNotNull(item.getFood());
        assertEquals(10, item.getFood().getHunger());
        assertEquals(0.6f, item.getFood().getSaturation());
        assertTrue(item.getFood().isAlwaysEdible());
        assertFalse(item.getFood().isSnack());
    }
    
    @Test
    void testItemDefinition_DurableItem() {
        ItemOptions options = new ItemOptions().durability(250);
        
        ItemDefinition item = new ItemDefinition("mymod:tool", options);
        
        assertTrue(item.isDurable());
        assertEquals(250, item.getDurability());
    }
    
    @Test
    void testItemDefinition_RecipeRemainder() {
        ItemOptions options = new ItemOptions()
            .recipeRemainder("minecraft:bucket");
        
        ItemDefinition item = new ItemDefinition("mymod:milk_bucket", options);
        
        assertTrue(item.hasRecipeRemainder());
        assertEquals("minecraft:bucket", item.getRecipeRemainder());
    }
    
    @Test
    void testItemDefinition_UseHandler() {
        UseHandler handler = ctx -> UseResult.success();
        ItemOptions options = new ItemOptions().use(handler);
        
        ItemDefinition item = new ItemDefinition("mymod:special", options);
        
        assertTrue(item.hasUseHandler());
        assertNotNull(item.getUseHandler());
        assertSame(handler, item.getUseHandler());
    }
    
    @Test
    void testItemDefinition_FabricItem() {
        ItemDefinition item = new ItemDefinition("mymod:item", new ItemOptions());
        
        assertNull(item.getFabricItem());
        
        Object fabricItem = new Object();
        item.setFabricItem(fabricItem);
        
        assertSame(fabricItem, item.getFabricItem());
    }
    
    @Test
    void testItemDefinition_ComplexItem() {
        FoodComponent food = new FoodComponent(5, 0.3f, false, true);
        UseHandler handler = ctx -> UseResult.replaceWith("minecraft:bowl");
        
        ItemOptions options = new ItemOptions()
            .stackSize(16)
            .traits("soup", "hot_food")
            .food(food)
            .recipeRemainder("minecraft:bowl")
            .use(handler);
        
        ItemDefinition item = new ItemDefinition("mymod:soup", options);
        
        assertEquals("mymod:soup", item.getId());
        assertEquals(16, item.getStackSize());
        assertArrayEquals(new String[]{"soup", "hot_food"}, item.getTraits());
        assertTrue(item.isFood());
        assertTrue(item.hasRecipeRemainder());
        assertTrue(item.hasUseHandler());
        assertFalse(item.isDurable());
        
        assertEquals(5, item.getFood().getHunger());
        assertEquals(0.3f, item.getFood().getSaturation());
        assertTrue(item.getFood().isSnack());
        
        assertEquals("minecraft:bowl", item.getRecipeRemainder());
    }
    
    @Test
    void testItemDefinition_EmptyTraits() {
        ItemDefinition item = new ItemDefinition("mymod:item", new ItemOptions());
        assertArrayEquals(new String[0], item.getTraits());
    }
}
