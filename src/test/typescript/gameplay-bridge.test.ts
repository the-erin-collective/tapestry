/**
 * Unit tests for the TypeScript to Java gameplay bridge.
 * 
 * These tests verify that:
 * - Filter specs are correctly serialized to Java Map<String, Object>
 * - Builder method calls are properly delegated to Java
 * - Parameter validation works at compile time
 * - The bridge handles both overloads of modify methods
 */

import { describe, it, expect, beforeEach, jest } from '@jest/globals';

// Mock the Java types for testing
const mockJavaTypes: Record<string, any> = {};

// Mock Java.type function
(global as any).Java = {
  type: (className: string) => {
    if (!mockJavaTypes[className]) {
      mockJavaTypes[className] = {};
    }
    return mockJavaTypes[className];
  }
};

// Import after mocking Java
import { createGameplayAPI } from '../../main/typescript/gameplay-bridge';
import type { TradeModificationBuilder, LootModificationBuilder } from '../../main/typescript/gameplay';

describe('Gameplay Bridge', () => {
  let mockTradeAPI: any;
  let mockLootAPI: any;
  let mockIdentifier: any;
  let mockConsumer: any;
  
  beforeEach(() => {
    // Reset mocks
    jest.clearAllMocks();
    
    // Mock Identifier.of
    mockIdentifier = {
      of: jest.fn((id: string) => ({ toString: () => id }))
    };
    mockJavaTypes['net.minecraft.util.Identifier'] = mockIdentifier;
    
    // Mock Consumer constructor
    mockConsumer = jest.fn((impl: any) => impl);
    mockJavaTypes['java.util.function.Consumer'] = mockConsumer;
    
    // Mock TradeAPI
    mockTradeAPI = {
      modify: jest.fn()
    };
    mockJavaTypes['com.tapestry.gameplay.trades.TradeAPI'] = mockTradeAPI;
    
    // Mock LootAPI
    mockLootAPI = {
      modify: jest.fn()
    };
    mockJavaTypes['com.tapestry.gameplay.loot.LootAPI'] = mockLootAPI;
  });
  
  describe('TradeAPI Bridge', () => {
    it('should serialize trade filter spec correctly', () => {
      const gameplay = createGameplayAPI();
      const mockJavaBuilder = {
        remove: jest.fn()
      };
      
      // Capture the consumer passed to Java
      mockTradeAPI.modify.mockImplementation((id: any, consumer: any) => {
        consumer.accept(mockJavaBuilder);
      });
      
      gameplay.trades.modify('minecraft:villager/fisherman', (trades) => {
        trades.remove({ input: 'minecraft:cod', level: 1 });
      });
      
      // Verify the filter was serialized correctly
      expect(mockJavaBuilder.remove).toHaveBeenCalledWith({
        inputItem: 'minecraft:cod',
        level: 1
      });
    });
    
    it('should normalize "input" to "inputItem"', () => {
      const gameplay = createGameplayAPI();
      const mockJavaBuilder = {
        remove: jest.fn()
      };
      
      mockTradeAPI.modify.mockImplementation((id: any, consumer: any) => {
        consumer.accept(mockJavaBuilder);
      });
      
      gameplay.trades.modify('minecraft:villager/fisherman', (trades) => {
        trades.remove({ input: 'minecraft:cod' });
      });
      
      expect(mockJavaBuilder.remove).toHaveBeenCalledWith({
        inputItem: 'minecraft:cod'
      });
    });
    
    it('should normalize "output" to "outputItem"', () => {
      const gameplay = createGameplayAPI();
      const mockJavaBuilder = {
        remove: jest.fn()
      };
      
      mockTradeAPI.modify.mockImplementation((id: any, consumer: any) => {
        consumer.accept(mockJavaBuilder);
      });
      
      gameplay.trades.modify('minecraft:villager/fisherman', (trades) => {
        trades.remove({ output: 'minecraft:emerald' });
      });
      
      expect(mockJavaBuilder.remove).toHaveBeenCalledWith({
        outputItem: 'minecraft:emerald'
      });
    });
    
    it('should handle replaceInput with identifier conversion', () => {
      const gameplay = createGameplayAPI();
      const mockJavaBuilder = {
        replaceInput: jest.fn()
      };
      
      mockTradeAPI.modify.mockImplementation((id: any, consumer: any) => {
        consumer.accept(mockJavaBuilder);
      });
      
      gameplay.trades.modify('minecraft:villager/fisherman', (trades) => {
        trades.replaceInput({ input: 'minecraft:cod' }, 'minecraft:salmon');
      });
      
      expect(mockIdentifier.of).toHaveBeenCalledWith('minecraft:salmon');
      expect(mockJavaBuilder.replaceInput).toHaveBeenCalledWith(
        { inputItem: 'minecraft:cod' },
        expect.any(Object)
      );
    });
    
    it('should handle replaceOutput with identifier conversion', () => {
      const gameplay = createGameplayAPI();
      const mockJavaBuilder = {
        replaceOutput: jest.fn()
      };
      
      mockTradeAPI.modify.mockImplementation((id: any, consumer: any) => {
        consumer.accept(mockJavaBuilder);
      });
      
      gameplay.trades.modify('minecraft:villager/fisherman', (trades) => {
        trades.replaceOutput({ output: 'minecraft:emerald' }, 'minecraft:diamond');
      });
      
      expect(mockIdentifier.of).toHaveBeenCalledWith('minecraft:diamond');
      expect(mockJavaBuilder.replaceOutput).toHaveBeenCalledWith(
        { outputItem: 'minecraft:emerald' },
        expect.any(Object)
      );
    });
    
    it('should handle add with trade spec serialization', () => {
      const gameplay = createGameplayAPI();
      const mockJavaBuilder = {
        add: jest.fn()
      };
      
      mockTradeAPI.modify.mockImplementation((id: any, consumer: any) => {
        consumer.accept(mockJavaBuilder);
      });
      
      gameplay.trades.modify('minecraft:villager/fisherman', (trades) => {
        trades.add({
          input: 'minecraft:nautilus_shell',
          output: 'minecraft:emerald',
          level: 2,
          maxUses: 12
        });
      });
      
      expect(mockJavaBuilder.add).toHaveBeenCalledWith({
        input: 'minecraft:nautilus_shell',
        output: 'minecraft:emerald',
        level: 2,
        maxUses: 12
      });
    });
    
    it('should support method chaining', () => {
      const gameplay = createGameplayAPI();
      const mockJavaBuilder = {
        remove: jest.fn(),
        replaceInput: jest.fn(),
        add: jest.fn()
      };
      
      mockTradeAPI.modify.mockImplementation((id: any, consumer: any) => {
        consumer.accept(mockJavaBuilder);
      });
      
      gameplay.trades.modify('minecraft:villager/fisherman', (trades) => {
        trades
          .remove({ input: 'minecraft:cod' })
          .replaceInput({ input: 'minecraft:salmon' }, 'minecraft:tropical_fish')
          .add({ input: 'minecraft:nautilus_shell', output: 'minecraft:emerald', level: 2 });
      });
      
      expect(mockJavaBuilder.remove).toHaveBeenCalled();
      expect(mockJavaBuilder.replaceInput).toHaveBeenCalled();
      expect(mockJavaBuilder.add).toHaveBeenCalled();
    });
    
    it('should handle priority parameter', () => {
      const gameplay = createGameplayAPI();
      
      gameplay.trades.modify('minecraft:villager/fisherman', (trades) => {
        // Empty builder
      }, -500);
      
      expect(mockTradeAPI.modify).toHaveBeenCalledWith(
        expect.any(Object),
        expect.any(Object),
        -500
      );
    });
    
    it('should call modify without priority when not specified', () => {
      const gameplay = createGameplayAPI();
      
      gameplay.trades.modify('minecraft:villager/fisherman', (trades) => {
        // Empty builder
      });
      
      expect(mockTradeAPI.modify).toHaveBeenCalledWith(
        expect.any(Object),
        expect.any(Object)
      );
      expect(mockTradeAPI.modify).not.toHaveBeenCalledWith(
        expect.any(Object),
        expect.any(Object),
        expect.any(Number)
      );
    });
  });
  
  describe('LootAPI Bridge', () => {
    it('should serialize loot pool filter spec correctly', () => {
      const gameplay = createGameplayAPI();
      const mockJavaBuilder = {
        removePool: jest.fn()
      };
      
      mockLootAPI.modify.mockImplementation((id: any, consumer: any) => {
        consumer.accept(mockJavaBuilder);
      });
      
      gameplay.loot.modify('minecraft:chests/simple_dungeon', (loot) => {
        loot.removePool({ name: 'main', rolls: 1 });
      });
      
      expect(mockJavaBuilder.removePool).toHaveBeenCalledWith({
        name: 'main',
        rolls: 1
      });
    });
    
    it('should serialize loot entry filter spec correctly', () => {
      const gameplay = createGameplayAPI();
      const mockJavaBuilder = {
        removeEntry: jest.fn()
      };
      
      mockLootAPI.modify.mockImplementation((id: any, consumer: any) => {
        consumer.accept(mockJavaBuilder);
      });
      
      gameplay.loot.modify('minecraft:chests/simple_dungeon', (loot) => {
        loot.removeEntry({ name: 'main' }, { item: 'minecraft:diamond' });
      });
      
      expect(mockJavaBuilder.removeEntry).toHaveBeenCalledWith(
        { name: 'main' },
        { item: 'minecraft:diamond' }
      );
    });
    
    it('should handle addPool with pool spec serialization', () => {
      const gameplay = createGameplayAPI();
      const mockJavaBuilder = {
        addPool: jest.fn()
      };
      
      mockLootAPI.modify.mockImplementation((id: any, consumer: any) => {
        consumer.accept(mockJavaBuilder);
      });
      
      gameplay.loot.modify('minecraft:chests/simple_dungeon', (loot) => {
        loot.addPool({
          name: 'custom',
          rolls: 1,
          bonusRolls: 0,
          entries: [{ item: 'minecraft:diamond', weight: 1 }]
        });
      });
      
      expect(mockJavaBuilder.addPool).toHaveBeenCalledWith({
        name: 'custom',
        rolls: 1,
        bonusRolls: 0,
        entries: [{ item: 'minecraft:diamond', weight: 1 }]
      });
    });
    
    it('should handle addEntry with entry spec serialization', () => {
      const gameplay = createGameplayAPI();
      const mockJavaBuilder = {
        addEntry: jest.fn()
      };
      
      mockLootAPI.modify.mockImplementation((id: any, consumer: any) => {
        consumer.accept(mockJavaBuilder);
      });
      
      gameplay.loot.modify('minecraft:chests/simple_dungeon', (loot) => {
        loot.addEntry(
          { name: 'main' },
          { item: 'minecraft:diamond', weight: 1, quality: 0 }
        );
      });
      
      expect(mockJavaBuilder.addEntry).toHaveBeenCalledWith(
        { name: 'main' },
        { item: 'minecraft:diamond', weight: 1, quality: 0 }
      );
    });
    
    it('should support method chaining for loot', () => {
      const gameplay = createGameplayAPI();
      const mockJavaBuilder = {
        removePool: jest.fn(),
        addEntry: jest.fn(),
        removeEntry: jest.fn()
      };
      
      mockLootAPI.modify.mockImplementation((id: any, consumer: any) => {
        consumer.accept(mockJavaBuilder);
      });
      
      gameplay.loot.modify('minecraft:chests/simple_dungeon', (loot) => {
        loot
          .removePool({ name: 'main' })
          .addEntry({ name: 'custom' }, { item: 'minecraft:diamond', weight: 1 })
          .removeEntry({}, { item: 'minecraft:dirt' });
      });
      
      expect(mockJavaBuilder.removePool).toHaveBeenCalled();
      expect(mockJavaBuilder.addEntry).toHaveBeenCalled();
      expect(mockJavaBuilder.removeEntry).toHaveBeenCalled();
    });
    
    it('should handle priority parameter for loot', () => {
      const gameplay = createGameplayAPI();
      
      gameplay.loot.modify('minecraft:chests/simple_dungeon', (loot) => {
        // Empty builder
      }, 500);
      
      expect(mockLootAPI.modify).toHaveBeenCalledWith(
        expect.any(Object),
        expect.any(Object),
        500
      );
    });
  });
  
  describe('Filter Serialization Edge Cases', () => {
    it('should handle empty filter spec', () => {
      const gameplay = createGameplayAPI();
      const mockJavaBuilder = {
        remove: jest.fn()
      };
      
      mockTradeAPI.modify.mockImplementation((id: any, consumer: any) => {
        consumer.accept(mockJavaBuilder);
      });
      
      gameplay.trades.modify('minecraft:villager/fisherman', (trades) => {
        trades.remove({});
      });
      
      expect(mockJavaBuilder.remove).toHaveBeenCalledWith({});
    });
    
    it('should handle all filter fields', () => {
      const gameplay = createGameplayAPI();
      const mockJavaBuilder = {
        remove: jest.fn()
      };
      
      mockTradeAPI.modify.mockImplementation((id: any, consumer: any) => {
        consumer.accept(mockJavaBuilder);
      });
      
      gameplay.trades.modify('minecraft:villager/fisherman', (trades) => {
        trades.remove({
          inputItem: 'minecraft:cod',
          inputTag: '#minecraft:fishes',
          outputItem: 'minecraft:emerald',
          outputTag: '#minecraft:gems',
          level: 1,
          maxUses: 16
        });
      });
      
      expect(mockJavaBuilder.remove).toHaveBeenCalledWith({
        inputItem: 'minecraft:cod',
        inputTag: '#minecraft:fishes',
        outputItem: 'minecraft:emerald',
        outputTag: '#minecraft:gems',
        level: 1,
        maxUses: 16
      });
    });
    
    it('should prefer explicit field names over aliases', () => {
      const gameplay = createGameplayAPI();
      const mockJavaBuilder = {
        remove: jest.fn()
      };
      
      mockTradeAPI.modify.mockImplementation((id: any, consumer: any) => {
        consumer.accept(mockJavaBuilder);
      });
      
      gameplay.trades.modify('minecraft:villager/fisherman', (trades) => {
        trades.remove({
          inputItem: 'minecraft:cod',
          input: 'minecraft:salmon' // Should be ignored
        });
      });
      
      expect(mockJavaBuilder.remove).toHaveBeenCalledWith({
        inputItem: 'minecraft:cod'
      });
    });
  });
});
