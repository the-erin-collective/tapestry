package com.tapestry.overlay;

/**
 * Exception thrown when template validation fails during Phase 10.5.
 * 
 * Provides detailed error information including the specific path within
 * the template structure and optional snippet for debugging.
 */
public class TemplateValidationError extends RuntimeException {
    
    private final String templateSnippet;
    private final String errorPath;
    
    /**
     * Creates a new template validation error.
     * 
     * @param message human-readable error message
     */
    public TemplateValidationError(String message) {
        this(message, null, null);
    }
    
    /**
     * Creates a new template validation error with path information.
     * 
     * @param message human-readable error message
     * @param errorPath path within the JSON structure where error occurred
     */
    public TemplateValidationError(String message, String errorPath) {
        this(message, errorPath, null);
    }
    
    /**
     * Creates a new template validation error with full context.
     * 
     * @param message human-readable error message
     * @param errorPath path within the JSON structure where error occurred
     * @param templateSnippet optional snippet of the template for debugging
     */
    public TemplateValidationError(String message, String errorPath, String templateSnippet) {
        super(message);
        this.errorPath = errorPath;
        this.templateSnippet = templateSnippet;
    }
    
    /**
     * Creates a new template validation error with cause.
     * 
     * @param message human-readable error message
     * @param cause the underlying cause
     */
    public TemplateValidationError(String message, Throwable cause) {
        this(message, null, null, cause);
    }
    
    /**
     * Creates a new template validation error with full context and cause.
     * 
     * @param message human-readable error message
     * @param errorPath path within the JSON structure where error occurred
     * @param templateSnippet optional snippet of the template for debugging
     * @param cause the underlying cause
     */
    public TemplateValidationError(String message, String errorPath, String templateSnippet, Throwable cause) {
        super(message, cause);
        this.errorPath = errorPath;
        this.templateSnippet = templateSnippet;
    }
    
    /**
     * Gets the error path within the template structure.
     * 
     * @return the error path, or null if not available
     */
    public String getErrorPath() {
        return errorPath;
    }
    
    /**
     * Gets the template snippet for debugging.
     * 
     * @return the template snippet, or null if not available
     */
    public String getTemplateSnippet() {
        return templateSnippet;
    }
    
    /**
     * Returns a detailed error message suitable for logging.
     * 
     * @return detailed error message
     */
    public String getDetailedMessage() {
        StringBuilder sb = new StringBuilder(getMessage());
        
        if (errorPath != null) {
            sb.append(" at path: ").append(errorPath);
        }
        
        if (templateSnippet != null) {
            sb.append(" | Snippet: ").append(templateSnippet);
        }
        
        return sb.toString();
    }
    
    @Override
    public String toString() {
        return "TemplateValidationError: " + getDetailedMessage();
    }
}
