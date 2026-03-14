package com.tapestry.typescript;

import com.tapestry.gameplay.loot.LootModifier;
import com.tapestry.gameplay.loot.LootTable;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

/**
 * TypeScript API for loot table modification.
 *
 * Provides tapestry.loot namespace for modifying loot tables
 * programmatically during datapack reload.
 */
public class LootApi {

    private static final Logger LOGGER = LoggerFactory.getLogger(LootApi.class);

    // Minecraft identifier format: namespace:path
    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("^[a-z0-9_.-]+:[a-z0-9_/.-]+$");

    private final LootModifier lootModifier;

    /**
     * Creates a loot API with the global loot modifier instance.
     */
    public LootApi() {
        this.lootModifier = LootModifier.getInstance();
    }

    /**
     * Creates the loot namespace for TypeScript.
     *
     * @return ProxyObject containing loot APIs
     */
    public ProxyObject createNamespace() {
        java.util.Map<String, Object> loot = new java.util.HashMap<>();

        loot.put("modify", createModifyFunction());

        return ProxyObject.fromMap(loot);
    }

    /**
     * Creates the modify function.
     */
    private ProxyExecutable createModifyFunction() {
        return args -> {
            if (args.length != 2) {
                throw new IllegalArgumentException("loot.modify requires exactly 2 arguments: (lootTableId, modifierFunction)");
            }

            String lootTableId = args[0].asString();
            Value modifierFunction = args[1];

            // Validate loot table identifier format
            if (lootTableId == null || lootTableId.isBlank()) {
                throw new IllegalArgumentException("lootTableId must be a non-empty string");
            }
            if (!IDENTIFIER_PATTERN.matcher(lootTableId).matches()) {
                throw new IllegalArgumentException(
                    String.format("Invalid loot table identifier '%s'. Must follow format 'namespace:path'", lootTableId)
                );
            }

            // Validate modifier is a function
            if (!modifierFunction.canExecute()) {
                throw new IllegalArgumentException("modifierFunction must be a callable function");
            }

            // Register the modifier with the loot modifier
            lootModifier.modify(lootTableId, table -> {
                try {
                    // Create a proxy for the loot table that exposes modification methods
                    LootTableProxy tableProxy = new LootTableProxy(table);
                    modifierFunction.execute(tableProxy);
                    LOGGER.debug("Applied loot modifier to table: {}", lootTableId);
                } catch (Exception e) {
                    LOGGER.error("Failed to apply loot modifier for table {}: {}", lootTableId, e.getMessage(), e);
                }
            });

            LOGGER.debug("Registered loot modifier for table: {}", lootTableId);

            return null;
        };
    }

    /**
     * Proxy object that exposes loot table modification methods to TypeScript.
     */
    public static class LootTableProxy {
        private final LootTable lootTable;

        public LootTableProxy(LootTable lootTable) {
            this.lootTable = lootTable;
        }

        /**
         * Replaces all occurrences of one item with another in the loot table.
         *
         * @param fromItem the item to replace
         * @param toItem the replacement item
         */
        public void replace(String fromItem, String toItem) {
            lootTable.replace(fromItem, toItem);
        }
    }
}