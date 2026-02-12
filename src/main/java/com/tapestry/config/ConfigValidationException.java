package com.tapestry.config;

/**
 * Exception thrown when configuration validation fails.
 */
public class ConfigValidationException extends Exception {
    
    public ConfigValidationException(String message) {
        super(message);
    }
    
    public ConfigValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
