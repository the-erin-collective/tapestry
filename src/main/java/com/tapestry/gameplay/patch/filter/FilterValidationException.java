package com.tapestry.gameplay.patch.filter;

/**
 * Exception thrown when a structured filter fails validation.
 * This is a checked exception to force explicit handling of validation failures.
 */
public class FilterValidationException extends Exception {
    /**
     * Creates a new FilterValidationException with an error message.
     * 
     * @param message The validation error message
     */
    public FilterValidationException(String message) {
        super(message);
    }
    
    /**
     * Creates a new FilterValidationException with a message and cause.
     * 
     * @param message The validation error message
     * @param cause The underlying exception that caused the validation failure
     */
    public FilterValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
