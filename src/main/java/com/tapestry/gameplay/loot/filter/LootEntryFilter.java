package com.tapestry.gameplay.loot.filter;

import com.tapestry.gameplay.patch.PatchContext;
import com.tapestry.gameplay.patch.filter.FilterValidationException;
import com.tapestry.gameplay.patch.filter.StructuredFilter;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.entry.LootPoolEntry;
import net.minecraft.util.Identifier;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * A structured filter for targeting specific loot entries in a loot pool.
 * <p>
 * This filter supports multiple optional criteria that are combined using logical AND.
 * A loot entry must match all specified criteria to be selected by the filter.
 * </p>
 * <p>
 * Supported criteria:
 * <ul>
 *   <li>{@code item} - The item identifier for the loot entry (only works with ItemEntry)</li>
 *   <li>{@code type} - The type of loot entry (e.g., "minecraft:item", "minecraft:tag")</li>
 * </ul>
 * </p>
 * 
 * @param item Optional item identifier for the loot entry
 * @param type Optional type identifier for the loot entry
 */
public record LootEntryFilter(
    Optional<Identifier> item,
    Optional<String> type
) implements StructuredFilter<LootPoolEntry> {
    
    /**
     * Compact constructor that validates non-null optionals.
     */
    public LootEntryFilter {
        if (item == null) item = Optional.empty();
        if (type == null) type = Optional.empty();
    }
    
    /**
     * Converts this structured filter into a predicate function.
     * <p>
     * The predicate combines all filter criteria using logical AND.
     * If a criterion is not specified (Optional.empty()), it is not included in the predicate.
     * </p>
     * 
     * @return A predicate that evaluates LootPoolEntry objects based on this filter's criteria
     */
    @Override
    public Predicate<LootPoolEntry> toPredicate() {
        return entry -> {
            // Check type criterion
            if (type.isPresent()) {
                String entryType = getEntryType(entry);
                if (entryType == null || !entryType.equals(type.get())) {
                    return false;
                }
            }
            
            // Check item criterion (only for ItemEntry)
            if (item.isPresent()) {
                if (!(entry instanceof ItemEntry)) {
                    return false;
                }
                Identifier entryItem = getItemFromEntry((ItemEntry) entry);
                if (entryItem == null || !entryItem.equals(item.get())) {
                    return false;
                }
            }
            
            // All criteria matched
            return true;
        };
    }
    
    /**
     * Validates this filter's criteria.
     * <p>
     * Validation checks that:
     * <ul>
     *   <li>Item identifier is not null if present</li>
     *   <li>Type is not null or empty if present</li>
     * </ul>
     * </p>
     * 
     * @throws FilterValidationException if any filter criteria are invalid
     */
    @Override
    public void validate() throws FilterValidationException {
        // Validate item
        if (item.isPresent() && item.get() == null) {
            throw new FilterValidationException(
                "Item identifier cannot be null"
            );
        }
        
        // Validate type
        if (type.isPresent() && 
            (type.get() == null || type.get().trim().isEmpty())) {
            throw new FilterValidationException(
                "Entry type cannot be null or empty"
            );
        }
    }
    
    @Override
    public void validate(PatchContext context) throws FilterValidationException {
        // perform normal checks first
        validate();
        
        // ensure the referenced item actually exists in the registry when provided
        if (item.isPresent() && !context.registryContains(item.get())) {
            throw new FilterValidationException(
                "Referenced loot item does not exist: " + item.get()
            );
        }
    }
    
    /**
     * Creates a LootEntryFilter from a specification map.
     * <p>
     * The specification map should contain string keys matching the filter criteria names
     * and values of appropriate types:
     * <ul>
     *   <li>"item" - String (will be converted to Identifier)</li>
     *   <li>"type" - String</li>
     * </ul>
     * </p>
     * 
     * @param spec The specification map containing filter criteria
     * @return A new LootEntryFilter instance
     * @throws IllegalArgumentException if the specification contains invalid values
     */
    public static LootEntryFilter fromSpec(Map<String, Object> spec) {
        if (spec == null) {
            throw new IllegalArgumentException("Filter specification cannot be null");
        }
        
        return new LootEntryFilter(
            extractIdentifier(spec, "item"),
            extractString(spec, "type")
        );
    }
    
    /**
     * Extracts an Identifier from the specification map.
     */
    private static Optional<Identifier> extractIdentifier(Map<String, Object> spec, String key) {
        Object value = spec.get(key);
        if (value == null) {
            return Optional.empty();
        }
        if (value instanceof String) {
            return Optional.of(Identifier.of((String) value));
        }
        if (value instanceof Identifier) {
            return Optional.of((Identifier) value);
        }
        throw new IllegalArgumentException(
            "Invalid type for " + key + ": expected String or Identifier, got " + 
            value.getClass().getName()
        );
    }
    
    /**
     * Extracts a String from the specification map.
     */
    private static Optional<String> extractString(Map<String, Object> spec, String key) {
        Object value = spec.get(key);
        if (value == null) {
            return Optional.empty();
        }
        if (value instanceof String) {
            return Optional.of((String) value);
        }
        throw new IllegalArgumentException(
            "Invalid type for " + key + ": expected String, got " + 
            value.getClass().getName()
        );
    }
    
    /**
     * Gets the type of a loot entry using reflection.
     * Returns null if the type cannot be determined.
     */
    private String getEntryType(LootPoolEntry entry) {
        try {
            // Try to get the type field
            Field typeField = entry.getClass().getDeclaredField("type");
            typeField.setAccessible(true);
            Object typeValue = typeField.get(entry);
            return typeValue != null ? typeValue.toString() : null;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            // Type field doesn't exist or can't be accessed
            // Fall back to class name
            return entry.getClass().getSimpleName();
        }
    }
    
    /**
     * Gets the item identifier from an ItemEntry using reflection.
     * Returns null if the item cannot be accessed.
     */
    private Identifier getItemFromEntry(ItemEntry entry) {
        try {
            Field itemField = ItemEntry.class.getDeclaredField("item");
            itemField.setAccessible(true);
            Object itemValue = itemField.get(entry);
            if (itemValue instanceof Identifier) {
                return (Identifier) itemValue;
            }
            return null;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            // Item field doesn't exist or can't be accessed
            return null;
        }
    }
}
