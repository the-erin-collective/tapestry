package com.tapestry.gameplay.loot.filter;

import com.tapestry.gameplay.patch.PatchContext;
import com.tapestry.gameplay.patch.filter.FilterValidationException;
import com.tapestry.gameplay.patch.filter.StructuredFilter;
import net.minecraft.loot.LootPool;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * A structured filter for targeting specific loot pools in a loot table.
 * <p>
 * This filter supports multiple optional criteria that are combined using logical AND.
 * A loot pool must match all specified criteria to be selected by the filter.
 * </p>
 * <p>
 * Supported criteria:
 * <ul>
 *   <li>{@code name} - The name identifier for the loot pool</li>
 *   <li>{@code rolls} - The number of rolls for the loot pool</li>
 *   <li>{@code bonusRolls} - The number of bonus rolls for the loot pool</li>
 * </ul>
 * </p>
 * 
 * @param name Optional name identifier for the loot pool
 * @param rolls Optional number of rolls for the loot pool
 * @param bonusRolls Optional number of bonus rolls for the loot pool
 */
public record LootPoolFilter(
    Optional<String> name,
    Optional<Integer> rolls,
    Optional<Integer> bonusRolls
) implements StructuredFilter<LootPool> {
    
    /**
     * Compact constructor that validates non-null optionals.
     */
    public LootPoolFilter {
        if (name == null) name = Optional.empty();
        if (rolls == null) rolls = Optional.empty();
        if (bonusRolls == null) bonusRolls = Optional.empty();
    }
    
    /**
     * Converts this structured filter into a predicate function.
     * <p>
     * The predicate combines all filter criteria using logical AND.
     * If a criterion is not specified (Optional.empty()), it is not included in the predicate.
     * </p>
     * 
     * @return A predicate that evaluates LootPool objects based on this filter's criteria
     */
    @Override
    public Predicate<LootPool> toPredicate() {
        return pool -> {
            // Check name criterion
            if (name.isPresent()) {
                String poolName = getPoolName(pool);
                if (poolName == null || !poolName.equals(name.get())) {
                    return false;
                }
            }
            
            // Check rolls criterion
            if (rolls.isPresent()) {
                int poolRolls = getPoolRolls(pool);
                if (poolRolls != rolls.get()) {
                    return false;
                }
            }
            
            // Check bonusRolls criterion
            if (bonusRolls.isPresent()) {
                int poolBonusRolls = getPoolBonusRolls(pool);
                if (poolBonusRolls != bonusRolls.get()) {
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
     *   <li>Name is not null or empty if present</li>
     *   <li>Rolls value is non-negative if present</li>
     *   <li>BonusRolls value is non-negative if present</li>
     * </ul>
     * </p>
     * 
     * @throws FilterValidationException if any filter criteria are invalid
     */
    @Override
    public void validate() throws FilterValidationException {
        // Validate name
        if (name.isPresent() && 
            (name.get() == null || name.get().trim().isEmpty())) {
            throw new FilterValidationException(
                "Pool name cannot be null or empty"
            );
        }
        
        // Validate rolls value
        if (rolls.isPresent() && rolls.get() < 0) {
            throw new FilterValidationException(
                "Rolls must be non-negative, got: " + rolls.get()
            );
        }
        
        // Validate bonusRolls value
        if (bonusRolls.isPresent() && bonusRolls.get() < 0) {
            throw new FilterValidationException(
                "BonusRolls must be non-negative, got: " + bonusRolls.get()
            );
        }
    }
    
    @Override
    public void validate(PatchContext context) throws FilterValidationException {
        // nothing context-sensitive for loot pools; just run normal checks
        validate();
    }
    
    /**
     * Creates a LootPoolFilter from a specification map.
     * <p>
     * The specification map should contain string keys matching the filter criteria names
     * and values of appropriate types:
     * <ul>
     *   <li>"name" - String</li>
     *   <li>"rolls" - Integer</li>
     *   <li>"bonusRolls" - Integer</li>
     * </ul>
     * </p>
     * 
     * @param spec The specification map containing filter criteria
     * @return A new LootPoolFilter instance
     * @throws IllegalArgumentException if the specification contains invalid values
     */
    public static LootPoolFilter fromSpec(Map<String, Object> spec) {
        if (spec == null) {
            throw new IllegalArgumentException("Filter specification cannot be null");
        }
        
        return new LootPoolFilter(
            extractString(spec, "name"),
            extractInteger(spec, "rolls"),
            extractInteger(spec, "bonusRolls")
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
    
    /**
     * Gets the name of a loot pool using reflection.
     * Returns null if the name field cannot be accessed.
     */
    private String getPoolName(LootPool pool) {
        try {
            Field nameField = LootPool.class.getDeclaredField("name");
            nameField.setAccessible(true);
            Object nameValue = nameField.get(pool);
            return nameValue != null ? nameValue.toString() : null;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            // Field doesn't exist or can't be accessed
            return null;
        }
    }
    
    /**
     * Gets the number of rolls for a loot pool using reflection.
     * Returns 0 if the rolls field cannot be accessed.
     */
    private int getPoolRolls(LootPool pool) {
        try {
            Field rollsField = LootPool.class.getDeclaredField("rolls");
            rollsField.setAccessible(true);
            Object rollsValue = rollsField.get(pool);
            if (rollsValue instanceof Number) {
                return ((Number) rollsValue).intValue();
            }
            return 0;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            // Field doesn't exist or can't be accessed
            return 0;
        }
    }
    
    /**
     * Gets the number of bonus rolls for a loot pool using reflection.
     * Returns 0 if the bonusRolls field cannot be accessed.
     */
    private int getPoolBonusRolls(LootPool pool) {
        try {
            Field bonusRollsField = LootPool.class.getDeclaredField("bonusRolls");
            bonusRollsField.setAccessible(true);
            Object bonusRollsValue = bonusRollsField.get(pool);
            if (bonusRollsValue instanceof Number) {
                return ((Number) bonusRollsValue).intValue();
            }
            return 0;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            // Field doesn't exist or can't be accessed
            return 0;
        }
    }
}
