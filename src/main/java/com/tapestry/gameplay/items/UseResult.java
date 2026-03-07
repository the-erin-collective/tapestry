package com.tapestry.gameplay.items;

/**
 * Result of an item use action.
 * 
 * Specifies item replacement and success status.
 */
public class UseResult {
    private final String item;
    private final boolean success;
    
    /**
     * Creates a use result with default success.
     */
    public UseResult() {
        this(null, true);
    }
    
    /**
     * Creates a use result.
     * 
     * @param item the item to replace the used item with (optional)
     * @param success whether the action succeeded
     */
    public UseResult(String item, boolean success) {
        this.item = item;
        this.success = success;
    }
    
    /**
     * Gets the replacement item identifier.
     * 
     * @return the item ID, or null for no replacement
     */
    public String getItem() {
        return item;
    }
    
    /**
     * Checks if the action succeeded.
     * 
     * @return true if successful
     */
    public boolean isSuccess() {
        return success;
    }
    
    /**
     * Creates a successful result with no item replacement.
     * 
     * @return success result
     */
    public static UseResult success() {
        return new UseResult(null, true);
    }
    
    /**
     * Creates a failed result.
     * 
     * @return failure result
     */
    public static UseResult failure() {
        return new UseResult(null, false);
    }
    
    /**
     * Creates a successful result with item replacement.
     * 
     * @param item the replacement item
     * @return success result with replacement
     */
    public static UseResult replaceWith(String item) {
        return new UseResult(item, true);
    }
}
