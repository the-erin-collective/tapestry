/**
 * TypeScript to Java bridge for the Gameplay Patch Engine.
 * 
 * This module provides the runtime implementation that connects TypeScript
 * mod code to the Java TradeAPI and LootAPI. It handles:
 * - Capturing builder method calls during mod execution
 * - Translating TypeScript filter specs to Java Map<String, Object>
 * - Delegating to Java TradeTableBuilder and LootTableBuilder
 * - Parameter validation at compile time where possible
 * 
 * @module tapestry/gameplay-bridge
 */

import type {
  Identifier,
  TradeFilterSpec,
  TradeSpec,
  TradeModificationBuilder,
  LootPoolFilterSpec,
  LootEntryFilterSpec,
  LootPoolSpec,
  LootEntrySpec,
  LootModificationBuilder,
  TradeAPI,
  LootAPI,
  GameplayAPI
} from './gameplay';

/**
 * Converts a TypeScript filter spec to a Java Map<String, Object>.
 * 
 * This function handles the serialization of filter specifications from
 * TypeScript objects to Java Map format. It normalizes field names
 * (e.g., "input" -> "inputItem") and validates types.
 * 
 * @param filterSpec The TypeScript filter specification
 * @returns A plain object that will be converted to Map<String, Object> by GraalVM
 */
function serializeTradeFilter(filterSpec: TradeFilterSpec): Record<string, any> {
  const result: Record<string, any> = {};
  
  // Handle input item (normalize "input" to "inputItem")
  if (filterSpec.inputItem !== undefined) {
    result.inputItem = filterSpec.inputItem;
  } else if (filterSpec.input !== undefined) {
    result.inputItem = filterSpec.input;
  }
  
  // Handle input tag
  if (filterSpec.inputTag !== undefined) {
    result.inputTag = filterSpec.inputTag;
  }
  
  // Handle output item (normalize "output" to "outputItem")
  if (filterSpec.outputItem !== undefined) {
    result.outputItem = filterSpec.outputItem;
  } else if (filterSpec.output !== undefined) {
    result.outputItem = filterSpec.output;
  }
  
  // Handle output tag
  if (filterSpec.outputTag !== undefined) {
    result.outputTag = filterSpec.outputTag;
  }
  
  // Handle level
  if (filterSpec.level !== undefined) {
    result.level = filterSpec.level;
  }
  
  // Handle maxUses
  if (filterSpec.maxUses !== undefined) {
    result.maxUses = filterSpec.maxUses;
  }
  
  return result;
}

/**
 * Converts a TypeScript trade spec to a Java Map<String, Object>.
 * 
 * @param tradeSpec The TypeScript trade specification
 * @returns A plain object that will be converted to Map<String, Object> by GraalVM
 */
function serializeTradeSpec(tradeSpec: TradeSpec): Record<string, any> {
  const result: Record<string, any> = {
    input: tradeSpec.input,
    output: tradeSpec.output,
    level: tradeSpec.level
  };
  
  if (tradeSpec.maxUses !== undefined) {
    result.maxUses = tradeSpec.maxUses;
  }
  
  if (tradeSpec.inputCount !== undefined) {
    result.inputCount = tradeSpec.inputCount;
  }
  
  if (tradeSpec.outputCount !== undefined) {
    result.outputCount = tradeSpec.outputCount;
  }
  
  return result;
}

/**
 * Converts a TypeScript loot pool filter spec to a Java Map<String, Object>.
 * 
 * @param filterSpec The TypeScript loot pool filter specification
 * @returns A plain object that will be converted to Map<String, Object> by GraalVM
 */
function serializeLootPoolFilter(filterSpec: LootPoolFilterSpec): Record<string, any> {
  const result: Record<string, any> = {};
  
  if (filterSpec.name !== undefined) {
    result.name = filterSpec.name;
  }
  
  if (filterSpec.rolls !== undefined) {
    result.rolls = filterSpec.rolls;
  }
  
  if (filterSpec.bonusRolls !== undefined) {
    result.bonusRolls = filterSpec.bonusRolls;
  }
  
  return result;
}

/**
 * Converts a TypeScript loot entry filter spec to a Java Map<String, Object>.
 * 
 * @param filterSpec The TypeScript loot entry filter specification
 * @returns A plain object that will be converted to Map<String, Object> by GraalVM
 */
function serializeLootEntryFilter(filterSpec: LootEntryFilterSpec): Record<string, any> {
  const result: Record<string, any> = {};
  
  if (filterSpec.item !== undefined) {
    result.item = filterSpec.item;
  }
  
  if (filterSpec.type !== undefined) {
    result.type = filterSpec.type;
  }
  
  return result;
}

/**
 * Converts a TypeScript loot pool spec to a Java Map<String, Object>.
 * 
 * @param poolSpec The TypeScript loot pool specification
 * @returns A plain object that will be converted to Map<String, Object> by GraalVM
 */
function serializeLootPoolSpec(poolSpec: LootPoolSpec): Record<string, any> {
  const result: Record<string, any> = {};
  
  if (poolSpec.pool !== undefined) {
    result.pool = poolSpec.pool;
  }
  
  if (poolSpec.name !== undefined) {
    result.name = poolSpec.name;
  }
  
  if (poolSpec.rolls !== undefined) {
    result.rolls = poolSpec.rolls;
  }
  
  if (poolSpec.bonusRolls !== undefined) {
    result.bonusRolls = poolSpec.bonusRolls;
  }
  
  if (poolSpec.entries !== undefined) {
    result.entries = poolSpec.entries;
  }
  
  return result;
}

/**
 * Converts a TypeScript loot entry spec to a Java Map<String, Object>.
 * 
 * @param entrySpec The TypeScript loot entry specification
 * @returns A plain object that will be converted to Map<String, Object> by GraalVM
 */
function serializeLootEntrySpec(entrySpec: LootEntrySpec): Record<string, any> {
  const result: Record<string, any> = {};
  
  if (entrySpec.entry !== undefined) {
    result.entry = entrySpec.entry;
  }
  
  if (entrySpec.item !== undefined) {
    result.item = entrySpec.item;
  }
  
  if (entrySpec.weight !== undefined) {
    result.weight = entrySpec.weight;
  }
  
  if (entrySpec.quality !== undefined) {
    result.quality = entrySpec.quality;
  }
  
  return result;
}

/**
 * Creates a TradeModificationBuilder that wraps a Java TradeTableBuilder.
 * 
 * This function creates a TypeScript builder object that delegates to the
 * Java TradeTableBuilder. All method calls are translated to Java method
 * calls with properly serialized parameters.
 * 
 * @param javaBuilder The Java TradeTableBuilder instance
 * @returns A TypeScript TradeModificationBuilder
 */
function createTradeModificationBuilder(javaBuilder: any): TradeModificationBuilder {
  return {
    remove(filterSpec: TradeFilterSpec): TradeModificationBuilder {
      const serialized = serializeTradeFilter(filterSpec);
      javaBuilder.remove(serialized);
      return this;
    },
    
    replaceInput(filterSpec: TradeFilterSpec, newInput: Identifier): TradeModificationBuilder {
      const serialized = serializeTradeFilter(filterSpec);
      // Convert string identifier to Java Identifier object
      const javaIdentifier = Java.type('net.minecraft.util.Identifier').of(newInput);
      javaBuilder.replaceInput(serialized, javaIdentifier);
      return this;
    },
    
    replaceOutput(filterSpec: TradeFilterSpec, newOutput: Identifier): TradeModificationBuilder {
      const serialized = serializeTradeFilter(filterSpec);
      // Convert string identifier to Java Identifier object
      const javaIdentifier = Java.type('net.minecraft.util.Identifier').of(newOutput);
      javaBuilder.replaceOutput(serialized, javaIdentifier);
      return this;
    },
    
    add(tradeSpec: TradeSpec): TradeModificationBuilder {
      const serialized = serializeTradeSpec(tradeSpec);
      javaBuilder.add(serialized);
      return this;
    }
  };
}

/**
 * Creates a LootModificationBuilder that wraps a Java LootTableBuilder.
 * 
 * This function creates a TypeScript builder object that delegates to the
 * Java LootTableBuilder. All method calls are translated to Java method
 * calls with properly serialized parameters.
 * 
 * @param javaBuilder The Java LootTableBuilder instance
 * @returns A TypeScript LootModificationBuilder
 */
function createLootModificationBuilder(javaBuilder: any): LootModificationBuilder {
  return {
    addPool(poolSpec: LootPoolSpec): LootModificationBuilder {
      const serialized = serializeLootPoolSpec(poolSpec);
      javaBuilder.addPool(serialized);
      return this;
    },
    
    removePool(filterSpec: LootPoolFilterSpec): LootModificationBuilder {
      const serialized = serializeLootPoolFilter(filterSpec);
      javaBuilder.removePool(serialized);
      return this;
    },
    
    addEntry(poolFilterSpec: LootPoolFilterSpec, entrySpec: LootEntrySpec): LootModificationBuilder {
      const serializedPool = serializeLootPoolFilter(poolFilterSpec);
      const serializedEntry = serializeLootEntrySpec(entrySpec);
      javaBuilder.addEntry(serializedPool, serializedEntry);
      return this;
    },
    
    removeEntry(poolFilterSpec: LootPoolFilterSpec, entryFilterSpec: LootEntryFilterSpec): LootModificationBuilder {
      const serializedPool = serializeLootPoolFilter(poolFilterSpec);
      const serializedEntry = serializeLootEntryFilter(entryFilterSpec);
      javaBuilder.removeEntry(serializedPool, serializedEntry);
      return this;
    }
  };
}

/**
 * Creates the TradeAPI implementation that bridges to Java.
 * 
 * This function creates a TypeScript API object that delegates to the
 * Java TradeAPI. It handles both overloads of the modify method.
 * 
 * @returns A TypeScript TradeAPI implementation
 */
function createTradeAPI(): TradeAPI {
  // Get the Java TradeAPI class
  const JavaTradeAPI = Java.type('com.tapestry.gameplay.trades.TradeAPI');
  
  return {
    modify(
      professionId: Identifier,
      builderFunction: (builder: TradeModificationBuilder) => void,
      priority?: number
    ): void {
      // Convert string identifier to Java Identifier object
      const javaIdentifier = Java.type('net.minecraft.util.Identifier').of(professionId);
      
      // Create a Java Consumer that wraps the TypeScript builder function
      const JavaConsumer = Java.type('java.util.function.Consumer');
      const consumer = new JavaConsumer({
        accept: (javaBuilder: any) => {
          // Wrap the Java builder in a TypeScript builder
          const tsBuilder = createTradeModificationBuilder(javaBuilder);
          // Execute the TypeScript builder function
          builderFunction(tsBuilder);
        }
      });
      
      // Call the appropriate Java method
      if (priority !== undefined) {
        JavaTradeAPI.modify(javaIdentifier, consumer, priority);
      } else {
        JavaTradeAPI.modify(javaIdentifier, consumer);
      }
    }
  };
}

/**
 * Creates the LootAPI implementation that bridges to Java.
 * 
 * This function creates a TypeScript API object that delegates to the
 * Java LootAPI. It handles both overloads of the modify method.
 * 
 * @returns A TypeScript LootAPI implementation
 */
function createLootAPI(): LootAPI {
  // Get the Java LootAPI class
  const JavaLootAPI = Java.type('com.tapestry.gameplay.loot.LootAPI');
  
  return {
    modify(
      lootTableId: Identifier,
      builderFunction: (builder: LootModificationBuilder) => void,
      priority?: number
    ): void {
      // Convert string identifier to Java Identifier object
      const javaIdentifier = Java.type('net.minecraft.util.Identifier').of(lootTableId);
      
      // Create a Java Consumer that wraps the TypeScript builder function
      const JavaConsumer = Java.type('java.util.function.Consumer');
      const consumer = new JavaConsumer({
        accept: (javaBuilder: any) => {
          // Wrap the Java builder in a TypeScript builder
          const tsBuilder = createLootModificationBuilder(javaBuilder);
          // Execute the TypeScript builder function
          builderFunction(tsBuilder);
        }
      });
      
      // Call the appropriate Java method
      if (priority !== undefined) {
        JavaLootAPI.modify(javaIdentifier, consumer, priority);
      } else {
        JavaLootAPI.modify(javaIdentifier, consumer);
      }
    }
  };
}

/**
 * Creates the GameplayAPI implementation that bridges to Java.
 * 
 * This is the main entry point for the gameplay patch engine from TypeScript.
 * It provides access to the trades and loot APIs.
 * 
 * @returns A TypeScript GameplayAPI implementation
 */
export function createGameplayAPI(): GameplayAPI {
  return {
    trades: createTradeAPI(),
    loot: createLootAPI()
  };
}

/**
 * Global gameplay API instance.
 * 
 * This is automatically created and exposed to TypeScript mods.
 */
export const gameplay: GameplayAPI = createGameplayAPI();
