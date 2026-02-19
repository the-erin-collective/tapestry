package com.tapestry.rpc;

/**
 * Exception thrown when RPC validation fails.
 * Used for security enforcement in Phase 16.5.
 */
public class RpcValidationException extends Exception {
    
    public RpcValidationException(String message) {
        super(message);
    }
    
    public RpcValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
