package com.tapestry.typescript;

import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * TypeScript API for tag modification.
 * 
 * Provides tapestry.tags namespace for modifying existing tags,
 * following the same patch-engine philosophy as trades and loot.
 */
public class TagsApi {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(TagsApi.class);
    
    // Minecraft identifier format: namespace:path
    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("^[a-z0-9_.-]+:[a-z0-9_/.-]+$");
    
    // Store tag modifications for batch processing
    private final List<TagModification> modifications = new ArrayList<>();
    
    /**
     * Represents a tag modification entry.
     */
    public static class TagModification {
        public enum OperationType {
            ADD,
            REMOVE,
            CLEAR
        }
        
        public final String tagId;
        public final OperationType operation;
        public final String itemId; // null for CLEAR operations
        
        public TagModification(String tagId, OperationType operation, String itemId) {
            this.tagId = tagId;
            this.operation = operation;
            this.itemId = itemId;
        }
    }
    
    /**
     * Creates the tags namespace for TypeScript.
     * 
     * @return ProxyObject containing tag APIs
     */
    public ProxyObject createNamespace() {
        Map<String, Object> tags = new HashMap<>();
        
        tags.put("modify", createModifyFunction());
        
        return ProxyObject.fromMap(tags);
    }
    
    /**
     * Creates the modify function that accepts builder callbacks.
     */
    private ProxyExecutable createModifyFunction() {
        return args -> {
            if (args.length != 2) {
                throw new IllegalArgumentException("tags.modify requires exactly 2 arguments: (tagId, builderFunction)");
            }
            
            String tagId = args[0].asString();
            Value builderFunction = args[1];
            
            // Validate tag identifier format
            if (tagId == null || tagId.isBlank()) {
                throw new IllegalArgumentException("tagId must be a non-empty string");
            }
            if (!IDENTIFIER_PATTERN.matcher(tagId).matches()) {
                throw new IllegalArgumentException(
                    String.format("Invalid tag identifier '%s'. Must follow format 'namespace:path'", tagId)
                );
            }
            
            // Validate builder function
            if (!builderFunction.canExecute()) {
                throw new IllegalArgumentException("Second argument must be a builder function");
            }
            
            // Create tag builder object
            TagBuilder tagBuilder = new TagBuilder(tagId);
            
            // Call builder function with builder object
            try {
                builderFunction.executeVoid(ProxyObject.fromMap(tagBuilder.toProxyMap()));
            } catch (Exception e) {
                LOGGER.error("Tag builder function threw exception for tag '{}': {}", tagId, e.getMessage(), e);
                throw new RuntimeException(
                    String.format("Tag builder function failed for '%s': %s", tagId, e.getMessage()), e
                );
            }
            
            // Store the modifications
            modifications.addAll(tagBuilder.getModifications());
            
            LOGGER.debug("Registered {} modifications for tag '{}'", 
                        tagBuilder.getModifications().size(), tagId);
            
            return null;
        };
    }
    
    /**
     * Gets all queued modifications (for server-side processing).
     * 
     * @return list of tag modifications
     */
    public List<TagModification> getModifications() {
        return new ArrayList<>(modifications);
    }
    
    /**
     * Clears all queued modifications.
     */
    public void clearModifications() {
        modifications.clear();
    }
    
    /**
     * Tag builder object for fluent API.
     */
    public static class TagBuilder {
        private final String tagId;
        private final List<TagModification> modifications = new ArrayList<>();
        
        public TagBuilder(String tagId) {
            this.tagId = tagId;
        }
        
        /**
         * Adds an item to the tag.
         */
        public void add(String itemId) {
            validateItemId(itemId);
            modifications.add(new TagModification(tagId, TagModification.OperationType.ADD, itemId));
        }
        
        /**
         * Removes an item from the tag.
         */
        public void remove(String itemId) {
            validateItemId(itemId);
            modifications.add(new TagModification(tagId, TagModification.OperationType.REMOVE, itemId));
        }
        
        /**
         * Clears all items from the tag.
         */
        public void clear() {
            modifications.add(new TagModification(tagId, TagModification.OperationType.CLEAR, null));
        }
        
        /**
         * Validates an item identifier.
         */
        private void validateItemId(String itemId) {
            if (itemId == null || itemId.isBlank()) {
                throw new IllegalArgumentException("itemId must be a non-empty string");
            }
            if (!IDENTIFIER_PATTERN.matcher(itemId).matches()) {
                throw new IllegalArgumentException(
                    String.format("Invalid item identifier '%s'. Must follow format 'namespace:path'", itemId)
                );
            }
        }
        
        /**
         * Converts to proxy map for JavaScript access.
         */
        private Map<String, Object> toProxyMap() {
            Map<String, Object> map = new HashMap<>();
            
            map.put("add", (ProxyExecutable) args -> {
                if (args.length != 1) {
                    throw new IllegalArgumentException("add() requires exactly 1 argument: (itemId)");
                }
                String itemId = args[0].asString();
                add(itemId);
                return null;
            });
            
            map.put("remove", (ProxyExecutable) args -> {
                if (args.length != 1) {
                    throw new IllegalArgumentException("remove() requires exactly 1 argument: (itemId)");
                }
                String itemId = args[0].asString();
                remove(itemId);
                return null;
            });
            
            map.put("clear", (ProxyExecutable) args -> {
                if (args.length != 0) {
                    throw new IllegalArgumentException("clear() requires no arguments");
                }
                clear();
                return null;
            });
            
            return map;
        }
        
        /**
         * Gets all modifications recorded by this builder.
         */
        public List<TagModification> getModifications() {
            return new ArrayList<>(modifications);
        }
    }
}
