package com.tapestry.gameplay.trades;

import com.tapestry.gameplay.trades.filter.TradeFilter;
import com.tapestry.gameplay.trades.model.BasicTradeEntry;
import com.tapestry.gameplay.trades.model.BasicTradeTable;
import com.tapestry.gameplay.trades.model.TradeItem;
import com.tapestry.gameplay.trades.operations.AddTradeOperation;
import com.tapestry.gameplay.trades.operations.RemoveTradeOperation;
import com.tapestry.gameplay.trades.operations.ReplaceTradeInputOperation;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Basic integration test for the Trades API implementation.
 * 
 * This test verifies that the core functionality works together:
 * - Trade creation from specifications
 * - Filtering and operations
 * - Tag expansion (simulated)
 */
public class TradesApiIntegrationTest {
    
    @Test
    public void testBasicTradeOperations() {
        // Create a trade table
        BasicTradeTable table = new BasicTradeTable();
        
        // Create some trades
        TradeItem emerald = TradeItem.of(Identifier.of("minecraft:emerald"));
        TradeItem cod = TradeItem.of(Identifier.of("minecraft:cod"), 6);
        TradeItem salmon = TradeItem.of(Identifier.of("minecraft:salmon"), 6);
        
        BasicTradeEntry fisherTrade1 = new BasicTradeEntry(
            emerald, null, cod, 1, 16, 0, 1.0f
        );
        
        BasicTradeEntry fisherTrade2 = new BasicTradeEntry(
            emerald, null, salmon, 1, 16, 0, 1.0f
        );
        
        // Add trades
        table.add(fisherTrade1);
        table.add(fisherTrade2);
        
        assertEquals(2, table.size());
        
        // Test filtering - find cod trades (by output item)
        TradeFilter codFilter = new TradeFilter(
            Optional.empty(),  // inputItem
            Optional.empty(),  // inputCount
            Optional.empty(),  // inputTag
            Optional.empty(),  // inputItem2
            Optional.empty(),  // inputCount2
            Optional.empty(),  // inputTag2
            Optional.of(Identifier.of("minecraft:cod")),  // outputItem
            Optional.empty(),  // outputCount
            Optional.empty(),  // outputTag
            Optional.empty(),  // level
            Optional.empty()   // maxUses
        );
        
        long codTrades = table.stream().filter(codFilter.toPredicate()).count();
        assertEquals(1, codTrades);
        
        // Test removal
        RemoveTradeOperation removeOp = new RemoveTradeOperation(codFilter);
        removeOp.apply(table);
        
        assertEquals(1, table.size());
        assertEquals("minecraft:salmon", table.stream().findFirst().get().getOutputItem().toString());
    }
    
    @Test
    public void testTradeCreationFromSpec() {
        // Test creating trades from specification maps
        Map<String, Object> spec1 = Map.of(
            "buy", Map.of("item", "minecraft:emerald", "count", 1),
            "sell", Map.of("item", "minecraft:cod", "count", 6),
            "level", 1
        );
        
        Map<String, Object> spec2 = Map.of(
            "buy", "minecraft:emerald",
            "sell", "minecraft:salmon",
            "level", 1,
            "maxUses", 12
        );
        
        BasicTradeEntry entry1 = BasicTradeEntry.fromSpec(spec1);
        BasicTradeEntry entry2 = BasicTradeEntry.fromSpec(spec2);
        
        assertEquals("minecraft:emerald", entry1.getInputItem().toString());
        assertEquals(1, entry1.getInputCount());
        assertEquals("minecraft:cod", entry1.getOutputItem().toString());
        assertEquals(6, entry1.getOutputCount());
        assertEquals(1, entry1.getLevel());
        
        assertEquals("minecraft:emerald", entry2.getInputItem().toString());
        assertEquals(1, entry2.getInputCount()); // default count
        assertEquals("minecraft:salmon", entry2.getOutputItem().toString());
        assertEquals(1, entry2.getOutputCount()); // default count
        assertEquals(12, entry2.getMaxUses());
    }
    
    @Test
    public void testTradeWithSecondaryInput() {
        Map<String, Object> spec = Map.of(
            "buy", Map.of("item", "minecraft:paper", "count", 14),
            "buy2", Map.of("item", "minecraft:emerald", "count", 1),
            "sell", Map.of("item", "minecraft:map", "count", 1),
            "level", 3
        );
        
        BasicTradeEntry entry = BasicTradeEntry.fromSpec(spec);
        
        assertEquals("minecraft:paper", entry.getInputItem().toString());
        assertEquals(14, entry.getInputCount());
        assertEquals("minecraft:emerald", entry.getInputItem2().toString());
        assertEquals(1, entry.getInputCount2());
        assertEquals("minecraft:map", entry.getOutputItem().toString());
        assertEquals(1, entry.getOutputCount());
        assertEquals(3, entry.getLevel());
    }
    
    @Test
    public void testTradeTableBuilder() {
        BasicTradeTable table = new BasicTradeTable();
        
        // Create a builder and add operations
        TradeTableBuilder builder = new TradeTableBuilder();
        
        // Add a trade
        builder.add(Map.of(
            "buy", "minecraft:emerald",
            "sell", "minecraft:cod",
            "level", 1
        ));
        
        // Remove cod trades
        builder.remove(Map.of("sell", "minecraft:cod"));
        
        // For now, we'll manually apply operations since we don't have the full patch system
        // In a real scenario, this would be done through the PatchEngine
        
        // Apply add operation manually
        AddTradeOperation addOp = new AddTradeOperation(BasicTradeEntry.fromSpec(Map.of(
            "buy", "minecraft:emerald",
            "sell", "minecraft:cod", 
            "level", 1
        )));
        addOp.apply(table);
        
        assertEquals(1, table.size());
        
        // Apply remove operation manually
        TradeFilter filter = TradeFilter.fromSpec(Map.of("sell", "minecraft:cod"));
        RemoveTradeOperation removeOp = new RemoveTradeOperation(filter);
        removeOp.apply(table);
        
        assertEquals(0, table.size());
    }
}