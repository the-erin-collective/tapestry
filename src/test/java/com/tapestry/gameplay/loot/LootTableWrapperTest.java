package com.tapestry.gameplay.loot;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for LootTableWrapper AST visitor pattern.
 */
class LootTableWrapperTest {
    
    private JsonObject createSimpleLootTable() {
        JsonObject table = new JsonObject();
        JsonArray pools = new JsonArray();
        
        JsonObject pool = new JsonObject();
        JsonArray entries = new JsonArray();
        
        JsonObject entry = new JsonObject();
        entry.addProperty("type", "minecraft:item");
        entry.addProperty("name", "minecraft:cod");
        
        entries.add(entry);
        pool.add("entries", entries);
        pools.add(pool);
        table.add("pools", pools);
        
        return table;
    }
    
    private JsonObject createNestedLootTable() {
        JsonObject table = new JsonObject();
        JsonArray pools = new JsonArray();
        
        JsonObject pool = new JsonObject();
        JsonArray entries = new JsonArray();
        
        // Create an alternatives entry with nested items
        JsonObject alternatives = new JsonObject();
        alternatives.addProperty("type", "minecraft:alternatives");
        
        JsonArray children = new JsonArray();
        
        JsonObject child1 = new JsonObject();
        child1.addProperty("type", "minecraft:item");
        child1.addProperty("name", "minecraft:cod");
        children.add(child1);
        
        JsonObject child2 = new JsonObject();
        child2.addProperty("type", "minecraft:item");
        child2.addProperty("name", "minecraft:salmon");
        children.add(child2);
        
        alternatives.add("children", children);
        entries.add(alternatives);
        pool.add("entries", entries);
        pools.add(pool);
        table.add("pools", pools);
        
        return table;
    }
    
    private JsonObject createGroupLootTable() {
        JsonObject table = new JsonObject();
        JsonArray pools = new JsonArray();
        
        JsonObject pool = new JsonObject();
        JsonArray entries = new JsonArray();
        
        // Create a group entry with nested items
        JsonObject group = new JsonObject();
        group.addProperty("type", "minecraft:group");
        
        JsonArray children = new JsonArray();
        
        JsonObject child1 = new JsonObject();
        child1.addProperty("type", "minecraft:item");
        child1.addProperty("name", "minecraft:pufferfish");
        children.add(child1);
        
        JsonObject child2 = new JsonObject();
        child2.addProperty("type", "minecraft:item");
        child2.addProperty("name", "minecraft:tropical_fish");
        children.add(child2);
        
        group.add("children", children);
        entries.add(group);
        pool.add("entries", entries);
        pools.add(pool);
        table.add("pools", pools);
        
        return table;
    }
    
    @Test
    void testConstructorWithNullAst() {
        assertThrows(IllegalArgumentException.class, () -> {
            new LootTableWrapper(null);
        });
    }
    
    @Test
    void testReplaceWithNullOldItem() {
        JsonObject table = createSimpleLootTable();
        LootTableWrapper wrapper = new LootTableWrapper(table);
        
        assertThrows(IllegalArgumentException.class, () -> {
            wrapper.replace(null, "minecraft:nori");
        });
    }
    
    @Test
    void testReplaceWithEmptyOldItem() {
        JsonObject table = createSimpleLootTable();
        LootTableWrapper wrapper = new LootTableWrapper(table);
        
        assertThrows(IllegalArgumentException.class, () -> {
            wrapper.replace("", "minecraft:nori");
        });
    }
    
    @Test
    void testReplaceWithNullNewItem() {
        JsonObject table = createSimpleLootTable();
        LootTableWrapper wrapper = new LootTableWrapper(table);
        
        assertThrows(IllegalArgumentException.class, () -> {
            wrapper.replace("minecraft:cod", null);
        });
    }
    
    @Test
    void testReplaceWithEmptyNewItem() {
        JsonObject table = createSimpleLootTable();
        LootTableWrapper wrapper = new LootTableWrapper(table);
        
        assertThrows(IllegalArgumentException.class, () -> {
            wrapper.replace("minecraft:cod", "");
        });
    }
    
    @Test
    void testSimpleItemReplacement() {
        JsonObject table = createSimpleLootTable();
        LootTableWrapper wrapper = new LootTableWrapper(table);
        
        wrapper.replace("minecraft:cod", "minecraft:nori");
        
        // Verify replacement
        JsonArray pools = table.getAsJsonArray("pools");
        JsonObject pool = pools.get(0).getAsJsonObject();
        JsonArray entries = pool.getAsJsonArray("entries");
        JsonObject entry = entries.get(0).getAsJsonObject();
        
        assertEquals("minecraft:nori", entry.get("name").getAsString());
    }
    
    @Test
    void testNoReplacementWhenItemNotFound() {
        JsonObject table = createSimpleLootTable();
        LootTableWrapper wrapper = new LootTableWrapper(table);
        
        wrapper.replace("minecraft:salmon", "minecraft:nori");
        
        // Verify no change
        JsonArray pools = table.getAsJsonArray("pools");
        JsonObject pool = pools.get(0).getAsJsonObject();
        JsonArray entries = pool.getAsJsonArray("entries");
        JsonObject entry = entries.get(0).getAsJsonObject();
        
        assertEquals("minecraft:cod", entry.get("name").getAsString());
    }
    
    @Test
    void testReplacementInAlternatives() {
        JsonObject table = createNestedLootTable();
        LootTableWrapper wrapper = new LootTableWrapper(table);
        
        wrapper.replace("minecraft:cod", "minecraft:nori");
        
        // Verify replacement in alternatives
        JsonArray pools = table.getAsJsonArray("pools");
        JsonObject pool = pools.get(0).getAsJsonObject();
        JsonArray entries = pool.getAsJsonArray("entries");
        JsonObject alternatives = entries.get(0).getAsJsonObject();
        JsonArray children = alternatives.getAsJsonArray("children");
        JsonObject child1 = children.get(0).getAsJsonObject();
        
        assertEquals("minecraft:nori", child1.get("name").getAsString());
        
        // Verify second child unchanged
        JsonObject child2 = children.get(1).getAsJsonObject();
        assertEquals("minecraft:salmon", child2.get("name").getAsString());
    }
    
    @Test
    void testReplacementInGroup() {
        JsonObject table = createGroupLootTable();
        LootTableWrapper wrapper = new LootTableWrapper(table);
        
        wrapper.replace("minecraft:pufferfish", "minecraft:nori");
        
        // Verify replacement in group
        JsonArray pools = table.getAsJsonArray("pools");
        JsonObject pool = pools.get(0).getAsJsonObject();
        JsonArray entries = pool.getAsJsonArray("entries");
        JsonObject group = entries.get(0).getAsJsonObject();
        JsonArray children = group.getAsJsonArray("children");
        JsonObject child1 = children.get(0).getAsJsonObject();
        
        assertEquals("minecraft:nori", child1.get("name").getAsString());
        
        // Verify second child unchanged
        JsonObject child2 = children.get(1).getAsJsonObject();
        assertEquals("minecraft:tropical_fish", child2.get("name").getAsString());
    }
    
    @Test
    void testMultipleReplacements() {
        JsonObject table = createNestedLootTable();
        LootTableWrapper wrapper = new LootTableWrapper(table);
        
        wrapper.replace("minecraft:cod", "minecraft:nori");
        wrapper.replace("minecraft:salmon", "minecraft:nori");
        
        // Verify both replacements
        JsonArray pools = table.getAsJsonArray("pools");
        JsonObject pool = pools.get(0).getAsJsonObject();
        JsonArray entries = pool.getAsJsonArray("entries");
        JsonObject alternatives = entries.get(0).getAsJsonObject();
        JsonArray children = alternatives.getAsJsonArray("children");
        
        JsonObject child1 = children.get(0).getAsJsonObject();
        assertEquals("minecraft:nori", child1.get("name").getAsString());
        
        JsonObject child2 = children.get(1).getAsJsonObject();
        assertEquals("minecraft:nori", child2.get("name").getAsString());
    }
    
    @Test
    void testConditionsPreserved() {
        JsonObject table = createSimpleLootTable();
        
        // Add a condition to the entry
        JsonArray pools = table.getAsJsonArray("pools");
        JsonObject pool = pools.get(0).getAsJsonObject();
        JsonArray entries = pool.getAsJsonArray("entries");
        JsonObject entry = entries.get(0).getAsJsonObject();
        
        JsonArray conditions = new JsonArray();
        JsonObject condition = new JsonObject();
        condition.addProperty("condition", "minecraft:random_chance");
        condition.addProperty("chance", 0.5);
        conditions.add(condition);
        entry.add("conditions", conditions);
        
        LootTableWrapper wrapper = new LootTableWrapper(table);
        wrapper.replace("minecraft:cod", "minecraft:nori");
        
        // Verify condition still exists
        assertTrue(entry.has("conditions"));
        JsonArray preservedConditions = entry.getAsJsonArray("conditions");
        assertEquals(1, preservedConditions.size());
        JsonObject preservedCondition = preservedConditions.get(0).getAsJsonObject();
        assertEquals("minecraft:random_chance", preservedCondition.get("condition").getAsString());
        assertEquals(0.5, preservedCondition.get("chance").getAsDouble());
    }
    
    @Test
    void testTagEntriesNotModified() {
        JsonObject table = new JsonObject();
        JsonArray pools = new JsonArray();
        
        JsonObject pool = new JsonObject();
        JsonArray entries = new JsonArray();
        
        JsonObject tagEntry = new JsonObject();
        tagEntry.addProperty("type", "minecraft:tag");
        tagEntry.addProperty("name", "minecraft:fishes");
        
        entries.add(tagEntry);
        pool.add("entries", entries);
        pools.add(pool);
        table.add("pools", pools);
        
        LootTableWrapper wrapper = new LootTableWrapper(table);
        wrapper.replace("minecraft:fishes", "minecraft:nori");
        
        // Verify tag entry unchanged
        JsonArray resultPools = table.getAsJsonArray("pools");
        JsonObject resultPool = resultPools.get(0).getAsJsonObject();
        JsonArray resultEntries = resultPool.getAsJsonArray("entries");
        JsonObject resultEntry = resultEntries.get(0).getAsJsonObject();
        
        assertEquals("minecraft:fishes", resultEntry.get("name").getAsString());
    }
    
    @Test
    void testGetAst() {
        JsonObject table = createSimpleLootTable();
        LootTableWrapper wrapper = new LootTableWrapper(table);
        
        assertSame(table, wrapper.getAst());
    }


    @Test
    void testMultipleOccurrencesInSamePool() {
        JsonObject table = new JsonObject();
        JsonArray pools = new JsonArray();

        JsonObject pool = new JsonObject();
        JsonArray entries = new JsonArray();

        // Add multiple entries with the same item
        JsonObject entry1 = new JsonObject();
        entry1.addProperty("type", "minecraft:item");
        entry1.addProperty("name", "minecraft:cod");
        entries.add(entry1);

        JsonObject entry2 = new JsonObject();
        entry2.addProperty("type", "minecraft:item");
        entry2.addProperty("name", "minecraft:salmon");
        entries.add(entry2);

        JsonObject entry3 = new JsonObject();
        entry3.addProperty("type", "minecraft:item");
        entry3.addProperty("name", "minecraft:cod");
        entries.add(entry3);

        pool.add("entries", entries);
        pools.add(pool);
        table.add("pools", pools);

        LootTableWrapper wrapper = new LootTableWrapper(table);
        wrapper.replace("minecraft:cod", "minecraft:nori");

        // Verify both cod entries were replaced
        JsonArray resultPools = table.getAsJsonArray("pools");
        JsonObject resultPool = resultPools.get(0).getAsJsonObject();
        JsonArray resultEntries = resultPool.getAsJsonArray("entries");

        JsonObject resultEntry1 = resultEntries.get(0).getAsJsonObject();
        assertEquals("minecraft:nori", resultEntry1.get("name").getAsString());

        JsonObject resultEntry2 = resultEntries.get(1).getAsJsonObject();
        assertEquals("minecraft:salmon", resultEntry2.get("name").getAsString());

        JsonObject resultEntry3 = resultEntries.get(2).getAsJsonObject();
        assertEquals("minecraft:nori", resultEntry3.get("name").getAsString());
    }

    @Test
    void testMultipleOccurrencesInDifferentPools() {
        JsonObject table = new JsonObject();
        JsonArray pools = new JsonArray();

        // First pool with cod
        JsonObject pool1 = new JsonObject();
        JsonArray entries1 = new JsonArray();
        JsonObject entry1 = new JsonObject();
        entry1.addProperty("type", "minecraft:item");
        entry1.addProperty("name", "minecraft:cod");
        entries1.add(entry1);
        pool1.add("entries", entries1);
        pools.add(pool1);

        // Second pool with cod
        JsonObject pool2 = new JsonObject();
        JsonArray entries2 = new JsonArray();
        JsonObject entry2 = new JsonObject();
        entry2.addProperty("type", "minecraft:item");
        entry2.addProperty("name", "minecraft:cod");
        entries2.add(entry2);
        pool2.add("entries", entries2);
        pools.add(pool2);

        table.add("pools", pools);

        LootTableWrapper wrapper = new LootTableWrapper(table);
        wrapper.replace("minecraft:cod", "minecraft:nori");

        // Verify both pools had cod replaced
        JsonArray resultPools = table.getAsJsonArray("pools");

        JsonObject resultPool1 = resultPools.get(0).getAsJsonObject();
        JsonArray resultEntries1 = resultPool1.getAsJsonArray("entries");
        JsonObject resultEntry1 = resultEntries1.get(0).getAsJsonObject();
        assertEquals("minecraft:nori", resultEntry1.get("name").getAsString());

        JsonObject resultPool2 = resultPools.get(1).getAsJsonObject();
        JsonArray resultEntries2 = resultPool2.getAsJsonArray("entries");
        JsonObject resultEntry2 = resultEntries2.get(0).getAsJsonObject();
        assertEquals("minecraft:nori", resultEntry2.get("name").getAsString());
    }

    @Test
    void testMultipleReplacementsInComplexTable() {
        JsonObject table = new JsonObject();
        JsonArray pools = new JsonArray();

        // Pool with mixed nested structures
        JsonObject pool = new JsonObject();
        JsonArray entries = new JsonArray();

        // Direct item entry
        JsonObject directEntry = new JsonObject();
        directEntry.addProperty("type", "minecraft:item");
        directEntry.addProperty("name", "minecraft:cod");
        entries.add(directEntry);

        // Alternatives with multiple items
        JsonObject alternatives = new JsonObject();
        alternatives.addProperty("type", "minecraft:alternatives");
        JsonArray altChildren = new JsonArray();

        JsonObject altChild1 = new JsonObject();
        altChild1.addProperty("type", "minecraft:item");
        altChild1.addProperty("name", "minecraft:salmon");
        altChildren.add(altChild1);

        JsonObject altChild2 = new JsonObject();
        altChild2.addProperty("type", "minecraft:item");
        altChild2.addProperty("name", "minecraft:cod");
        altChildren.add(altChild2);

        alternatives.add("children", altChildren);
        entries.add(alternatives);

        // Group with items
        JsonObject group = new JsonObject();
        group.addProperty("type", "minecraft:group");
        JsonArray groupChildren = new JsonArray();

        JsonObject groupChild1 = new JsonObject();
        groupChild1.addProperty("type", "minecraft:item");
        groupChild1.addProperty("name", "minecraft:pufferfish");
        groupChildren.add(groupChild1);

        JsonObject groupChild2 = new JsonObject();
        groupChild2.addProperty("type", "minecraft:item");
        groupChild2.addProperty("name", "minecraft:salmon");
        groupChildren.add(groupChild2);

        group.add("children", groupChildren);
        entries.add(group);

        pool.add("entries", entries);
        pools.add(pool);
        table.add("pools", pools);

        LootTableWrapper wrapper = new LootTableWrapper(table);

        // Replace cod and salmon with nori
        wrapper.replace("minecraft:cod", "minecraft:nori");
        wrapper.replace("minecraft:salmon", "minecraft:nori");

        // Verify all replacements
        JsonArray resultPools = table.getAsJsonArray("pools");
        JsonObject resultPool = resultPools.get(0).getAsJsonObject();
        JsonArray resultEntries = resultPool.getAsJsonArray("entries");

        // Check direct entry
        JsonObject resultDirectEntry = resultEntries.get(0).getAsJsonObject();
        assertEquals("minecraft:nori", resultDirectEntry.get("name").getAsString());

        // Check alternatives
        JsonObject resultAlternatives = resultEntries.get(1).getAsJsonObject();
        JsonArray resultAltChildren = resultAlternatives.getAsJsonArray("children");
        assertEquals("minecraft:nori", resultAltChildren.get(0).getAsJsonObject().get("name").getAsString());
        assertEquals("minecraft:nori", resultAltChildren.get(1).getAsJsonObject().get("name").getAsString());

        // Check group
        JsonObject resultGroup = resultEntries.get(2).getAsJsonObject();
        JsonArray resultGroupChildren = resultGroup.getAsJsonArray("children");
        assertEquals("minecraft:pufferfish", resultGroupChildren.get(0).getAsJsonObject().get("name").getAsString());
        assertEquals("minecraft:nori", resultGroupChildren.get(1).getAsJsonObject().get("name").getAsString());
    }

    @Test
    void testConditionsPreservedInNestedStructures() {
        JsonObject table = new JsonObject();
        JsonArray pools = new JsonArray();

        JsonObject pool = new JsonObject();
        JsonArray entries = new JsonArray();

        // Create alternatives with conditions
        JsonObject alternatives = new JsonObject();
        alternatives.addProperty("type", "minecraft:alternatives");

        // Add conditions to the alternatives entry
        JsonArray altConditions = new JsonArray();
        JsonObject altCondition = new JsonObject();
        altCondition.addProperty("condition", "minecraft:survives_explosion");
        altConditions.add(altCondition);
        alternatives.add("conditions", altConditions);

        JsonArray children = new JsonArray();

        JsonObject child1 = new JsonObject();
        child1.addProperty("type", "minecraft:item");
        child1.addProperty("name", "minecraft:cod");

        // Add conditions to the child entry
        JsonArray childConditions = new JsonArray();
        JsonObject childCondition = new JsonObject();
        childCondition.addProperty("condition", "minecraft:random_chance");
        childCondition.addProperty("chance", 0.75);
        childConditions.add(childCondition);
        child1.add("conditions", childConditions);

        children.add(child1);
        alternatives.add("children", children);
        entries.add(alternatives);
        pool.add("entries", entries);
        pools.add(pool);
        table.add("pools", pools);

        LootTableWrapper wrapper = new LootTableWrapper(table);
        wrapper.replace("minecraft:cod", "minecraft:nori");

        // Verify item was replaced
        JsonArray resultPools = table.getAsJsonArray("pools");
        JsonObject resultPool = resultPools.get(0).getAsJsonObject();
        JsonArray resultEntries = resultPool.getAsJsonArray("entries");
        JsonObject resultAlternatives = resultEntries.get(0).getAsJsonObject();
        JsonArray resultChildren = resultAlternatives.getAsJsonArray("children");
        JsonObject resultChild = resultChildren.get(0).getAsJsonObject();
        assertEquals("minecraft:nori", resultChild.get("name").getAsString());

        // Verify alternatives conditions preserved
        assertTrue(resultAlternatives.has("conditions"));
        JsonArray preservedAltConditions = resultAlternatives.getAsJsonArray("conditions");
        assertEquals(1, preservedAltConditions.size());
        assertEquals("minecraft:survives_explosion",
                     preservedAltConditions.get(0).getAsJsonObject().get("condition").getAsString());

        // Verify child conditions preserved
        assertTrue(resultChild.has("conditions"));
        JsonArray preservedChildConditions = resultChild.getAsJsonArray("conditions");
        assertEquals(1, preservedChildConditions.size());
        JsonObject preservedChildCondition = preservedChildConditions.get(0).getAsJsonObject();
        assertEquals("minecraft:random_chance", preservedChildCondition.get("condition").getAsString());
        assertEquals(0.75, preservedChildCondition.get("chance").getAsDouble());
    }

}
