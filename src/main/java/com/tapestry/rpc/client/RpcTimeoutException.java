package com.tapestry.rpc.client;

/**
 * Exception thrown when an RPC call times out.
 */
public class RpcTimeoutException extends RpcException {
    
    public RpcTimeoutException(String message) {
        super("TIMEOUT", message);
    }
    
    public RpcTimeoutException(String message, Throwable cause) {
        super("TIMEOUT", message, cause);
    }
}
