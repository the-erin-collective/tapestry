package com.tapestry.rpc;

/**
 * Exception thrown when serialization validation fails.
 */
public class SerializationException extends Exception {
    
    private final String code;
    
    public SerializationException(String code, String message) {
        super(message);
        this.code = code;
    }
    
    public SerializationException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }
    
    public String getCode() {
        return code;
    }
    
    @Override
    public String toString() {
        return "SerializationException{" +
               "code='" + code + '\'' +
               ", message='" + getMessage() + '\'' +
               '}';
    }
}
