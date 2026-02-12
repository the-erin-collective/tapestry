package com.tapestry.extensions;

import java.util.List;

/**
 * Function proxy that bridges Java to JavaScript execution.
 * Return value is capability-specific and not enforced by Tapestry.
 */
@FunctionalInterface
public interface ProxyExecutable {
    
    /**
     * Executes the function with given arguments.
     * 
     * @param args function arguments from JavaScript
     * @return capability-specific return value (can be null)
     */
    Object execute(List<Object> args);
}
