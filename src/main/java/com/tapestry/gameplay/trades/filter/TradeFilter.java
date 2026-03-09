package com.tapestry.gameplay.trades.filter;

import com.tapestry.gameplay.patch.PatchContext;
import com.tapestry.gameplay.patch.filter.FilterValidationException;
import com.tapestry.gameplay.patch.filter.StructuredFilter;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * A structured filter for targeting specific trades in a villager's trade table.
 * <p>
 * This filter supports multiple optional criteria that are combined using logical AND.
 * A trade must match all specified criteria to be selected by the filter.
 * </p>
 * <p>
 * Supported criteria:
 * <ul>
 *   <li>{@code inputItem} - The item identifier for the trade's input item</li>
 *   <li>{@code inputTag} - The tag identifier for the trade's input item</li>
 *   <li>{@code outputItem} - The item identifier for the trade's output item</li>
 *   <li>{@code outputTag} - The tag identifier for the trade's output item</li>
 *   <li>{@code level} - The villager level required for this trade</li>
 *   <li>{@code maxUses} - The maximum number of times this trade can be used</li>
 * </ul>
 * </p>
 * 
 * @param inputItem Optional item identifier for the trade's input
 * @param inputTag Optional tag identifier for the trade's input
 * @param outputItem Optional item identifier for the trade's output
 * @param outputTag Optional tag identifier for the trade's output
 * @param level Optional villager level for the trade
 * @param maxUses Optional maximum uses for the trade
 */
public record TradeFilter(
    Optional<Identifier> inputItem,
    Optional<String> inputTag,
    Optional<Identifier> outputItem,
    Optional<String> outputTag,
    Optional<Integer> level,
    Optional<Integer> maxUses
) implements StructuredFilter<TradeEntry> {
    
    /**
     * Compact constructor that validates non-null optionals.
     */
    public TradeFilter {
        if (inputItem == null) inputItem = Optional.empty();
        if (inputTag == null) inputTag = Optional.empty();
        if (outputItem == null) outputItem = Optional.empty();
        if (outputTag == null) outputTag = Optional.empty();
        if (level == null) level = Optional.empty();
        if (maxUses == null) maxUses = Optional.empty();
    }
    
    /**
     * Converts this structured filter into a predicate function.
     * <p>
     * The predicate combines all filter criteria using logical AND.
     * If a criterion is not specified (Optional.empty()), it is not included in the predicate.
     * </p>
     * 
     * @return A predicate that evaluates TradeEntry objects based on this filter's criteria
     */
    @Override
    public Predicate<TradeEntry> toPredicate() {
        return entry -> {
            // Check input item criterion
            if (inputItem.isPresent() && 
                !entry.getInputItem().equals(inputItem.get())) {
                return false;
            }
            
            // Check input tag criterion
            if (inputTag.isPresent() && 
                !entry.hasInputTag(inputTag.get())) {
                return false;
            }
            
            // Check output item criterion
            if (outputItem.isPresent() && 
                !entry.getOutputItem().equals(outputItem.get())) {
                return false;
            }
            
            // Check output tag criterion
            if (outputTag.isPresent() && 
                !entry.hasOutputTag(outputTag.get())) {
                return false;
            }
            
            // Check level criterion
            if (level.isPresent() && entry.getLevel() != level.get()) {
                return false;
            }
            
            // Check maxUses criterion
            if (maxUses.isPresent() && entry.getMaxUses() != maxUses.get()) {
                return false;
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
     *   <li>Item identifiers reference valid registry entries</li>
     *   <li>Tag identifiers are valid (non-empty strings)</li>
     *   <li>Level values are positive</li>
     *   <li>MaxUses values are positive</li>
     * </ul>
     * </p>
     * 
     * @throws FilterValidationException if any filter criteria are invalid
     */
    @Override
    public void validate() throws FilterValidationException {
        // Basic syntactic checks that don't require a context
        // Validate input tag identifier
        if (inputTag.isPresent() && 
            (inputTag.get() == null || inputTag.get().trim().isEmpty())) {
            throw new FilterValidationException(
                "Input tag cannot be null or empty"
            );
        }
        
        // Validate output tag identifier
        if (outputTag.isPresent() && 
            (outputTag.get() == null || outputTag.get().trim().isEmpty())) {
            throw new FilterValidationException(
                "Output tag cannot be null or empty"
            );
        }
        
        // Validate level value
        if (level.isPresent() && level.get() <= 0) {
            throw new FilterValidationException(
                "Level must be positive, got: " + level.get()
            );
        }
        
        // Validate maxUses value
        if (maxUses.isPresent() && maxUses.get() <= 0) {
            throw new FilterValidationException(
                "MaxUses must be positive, got: " + maxUses.get()
            );
        }
    }
    
    @Override
    public void validate(PatchContext context) throws FilterValidationException {
        // run basic checks first
        validate();
        
        // ensure referenced item identifiers actually exist in the registry
        if (inputItem.isPresent() && !context.registryContains(inputItem.get())) {
            throw new FilterValidationException("Unknown input item: " + inputItem.get());
        }
        if (outputItem.isPresent() && !context.registryContains(outputItem.get())) {
            throw new FilterValidationException("Unknown output item: " + outputItem.get());
        }
    }
    
    /**
     * Creates a TradeFilter from a specification map.
     * <p>
     * The specification map should contain string keys matching the filter criteria names
     * and values of appropriate types:
     * <ul>
     *   <li>"inputItem" or "input" - String (converted to Identifier)</li>
     *   <li>"inputTag" - String</li>
     *   <li>"outputItem" or "output" - String (converted to Identifier)</li>
     *   <li>"outputTag" - String</li>
     *   <li>"level" - Integer</li>
     *   <li>"maxUses" - Integer</li>
     * </ul>
     * </p>
     * 
     * @param spec The specification map containing filter criteria
     * @return A new TradeFilter instance
     * @throws IllegalArgumentException if the specification contains invalid values
     */
    public static TradeFilter fromSpec(Map<String, Object> spec) {
        if (spec == null) {
            throw new IllegalArgumentException("Filter specification cannot be null");
        }
        
        return new TradeFilter(
            extractIdentifier(spec, "inputItem", "input"),
            extractString(spec, "inputTag"),
            extractIdentifier(spec, "outputItem", "output"),
            extractString(spec, "outputTag"),
            extractInteger(spec, "level"),
            extractInteger(spec, "maxUses")
        );
    }
    
    /**
     * Extracts an Identifier from the specification map.
     * Tries multiple key names in order.
     */
    private static Optional<Identifier> extractIdentifier(Map<String, Object> spec, String... keys) {
        for (String key : keys) {
            Object value = spec.get(key);
            if (value != null) {
                if (value instanceof String) {
                    return Optional.of(Identifier.of((String) value));
                } else if (value instanceof Identifier) {
                    return Optional.of((Identifier) value);
                } else {
                    throw new IllegalArgumentException(
                        "Invalid type for " + key + ": expected String or Identifier, got " + 
                        value.getClass().getName()
                    );
                }
            }
        }
        return Optional.empty();
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
     * Extracts an Integer from the specification map.
     */
    private static Optional<Integer> extractInteger(Map<String, Object> spec, String key) {
        Object value = spec.get(key);
        if (value == null) {
            return Optional.empty();
        }
        if (value instanceof Integer) {
            return Optional.of((Integer) value);
        }
        if (value instanceof Number) {
            return Optional.of(((Number) value).intValue());
        }
        throw new IllegalArgumentException(
            "Invalid type for " + key + ": expected Integer, got " + 
            value.getClass().getName()
        );
    }
}
