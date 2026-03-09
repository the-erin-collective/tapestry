package com.tapestry.gameplay.patch.filter;

import java.util.function.Predicate;
import com.tapestry.gameplay.patch.PatchContext;

/**
 * A structured filter that provides a way to filter gameplay data using structured criteria
 * instead of serializing predicate functions.
 * <p>
 * Domain-specific implementations define their own filter schemas (e.g., TradeFilter, LootPoolFilter)
 * with named criteria fields. The filter can be validated and converted to a predicate function
 * for use in patch operations.
 * </p>
 * 
 * @param <T> The type of object this filter can evaluate
 */
public interface StructuredFilter<T> {
    /**
     * Converts this structured filter into a predicate function.
     * <p>
     * The predicate combines all filter criteria using logical AND.
     * If a criterion is not specified (e.g., Optional.empty()), it is not included in the predicate.
     * </p>
     * 
     * @return A predicate that evaluates objects of type T based on this filter's criteria
     */
    Predicate<T> toPredicate();
    
    /**
     * Validates this filter's criteria.
     * <p>
     * Validation checks that:
     * <ul>
     *   <li>Item identifiers reference valid registry entries</li>
     *   <li>Tag identifiers reference valid tags</li>
     *   <li>Field values are within acceptable ranges</li>
     *   <li>Required fields are present</li>
     * </ul>
     * </p>
     * 
     * @throws FilterValidationException if any filter criteria are invalid
     */
    void validate() throws FilterValidationException;

    /**
     * Validates this filter's criteria using the provided patch context.
     * 
     * <p>The default implementation simply delegates to {@link #validate()}.
     * Domain-specific filters may override this method to perform additional
     * checks that require access to registry data via the context (for example,
     * ensuring an item identifier actually exists).</p>
     *
     * @param context the patch context used for lookups
     * @throws FilterValidationException if validation fails
     */
    default void validate(PatchContext context) throws FilterValidationException {
        // by default preserve existing behaviour
        validate();
    }
}
