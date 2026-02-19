package com.tapestry.rpc.client;

/**
 * Exception thrown when an RPC call fails on the server side.
 */
public class RpcException extends Exception {
    
    private final String code;
    
    public RpcException(String code, String message) {
        super(message);
        this.code = code;
    }
    
    public RpcException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }
    
    public String getCode() {
        return code;
    }
    
    @Override
    public String toString() {
        return "RpcException{" +
               "code='" + code + '\'' +
               ", message='" + getMessage() + '\'' +
               '}';
    }
}
