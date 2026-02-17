package com.tapestry.extensions.types;

/**
 * Error codes for Phase 14 type validation.
 */
public enum TypeValidationError {
    
    // Validation Errors
    DEPENDENCY_NOT_FOUND("DEPENDENCY_NOT_FOUND"),
    DEPENDENCY_NOT_VALIDATED("DEPENDENCY_NOT_VALIDATED"),
    TYPE_IMPORT_NOT_REQUIRED_DEPENDENCY("TYPE_IMPORT_NOT_REQUIRED_DEPENDENCY"),
    TARGET_DOES_NOT_EXPORT_TYPES("TARGET_DOES_NOT_EXPORT_TYPES"),
    TYPE_EXPORT_FILE_NOT_FOUND("TYPE_EXPORT_FILE_NOT_FOUND"),
    TYPE_EXPORT_FILE_TOO_LARGE("TYPE_EXPORT_FILE_TOO_LARGE"),
    AMBIENT_DECLARATION_FORBIDDEN("AMBIENT_DECLARATION_FORBIDDEN"),
    DEPENDENCY_CYCLE_DETECTED("DEPENDENCY_CYCLE_DETECTED"),
    DUPLICATE_EXTENSION_ID("DUPLICATE_EXTENSION_ID"),
    
    // Runtime Errors
    UNDECLARED_TYPE_IMPORT("UNDECLARED_TYPE_IMPORT"),
    RUNTIME_IMPORT_FORBIDDEN("RUNTIME_IMPORT_FORBIDDEN"),
    INVALID_TAPESTRY_NAMESPACE("INVALID_TAPESTRY_NAMESPACE");
    
    private final String code;
    
    TypeValidationError(String code) {
        this.code = code;
    }
    
    public String getCode() {
        return code;
    }
    
    @Override
    public String toString() {
        return code;
    }
}
