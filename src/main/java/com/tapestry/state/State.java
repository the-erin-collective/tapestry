package com.tapestry.state;

import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Phase 12 State primitive implementation.
 * 
 * Provides transactional state mutation with deferred event emission.
 * State changes are immediate in memory but emitted as events
 * only after the outermost handler completes.
 */
public class State<T> {
    
    private final String stateName;
    private final StateCoordinator stateCoordinator;
    private T value;
    
    /**
     * Creates a new State instance with explicit name.
     * 
     * @param stateName human-readable name for debugging and diagnostics
     * @param initialValue initial value
     * @param stateCoordinator the global state coordinator for transaction management
     */
    public State(String stateName, T initialValue, StateCoordinator stateCoordinator) {
        this.stateName = stateName;
        this.stateCoordinator = stateCoordinator;
        this.value = initialValue;
    }
    
    /**
     * Gets the current state value.
     * Always reflects the most recent set() in the same handler.
     * 
     * @return current value
     */
    public T get() {
        return value;
    }
    
    /**
     * Sets a new state value immediately.
     * Enqueues a state change event for deferred emission.
     * 
     * @param newValue the new value to set
     */
    public void set(T newValue) {
        T oldValue = this.value;
        this.value = newValue;
        
        // Queue the state change for deferred emission
        stateCoordinator.queueChange(this, oldValue, newValue);
    }
    
    /**
     * Updates state using a mutator function.
     * Equivalent to: set(mutator(get()))
     * 
     * @param mutator function that takes current value and returns new value
     */
    public void update(Value mutator) {
        if (!mutator.canExecute()) {
            throw new IllegalArgumentException("Mutator must be an executable function");
        }
        
        T currentValue = this.value;
        T newValue;
        
        try {
            // Execute mutator with current value
            Value result = mutator.execute(currentValue);
            
            if (result.fitsInInt()) {
                newValue = (T) Integer.valueOf(result.asInt());
            } else if (result.fitsInDouble()) {
                newValue = (T) Double.valueOf(result.asDouble());
            } else if (result.fitsInLong()) {
                newValue = (T) Long.valueOf(result.asLong());
            } else if (result.isBoolean()) {
                newValue = (T) Boolean.valueOf(result.asBoolean());
            } else if (result.isString()) {
                // For generic T, we need to handle this differently
                throw new IllegalArgumentException("State mutator must return a value of compatible type");
            } else {
                throw new IllegalArgumentException("Mutator returned unsupported type");
            }
        } catch (Exception e) {
            throw new RuntimeException("State mutator execution failed", e);
        }
        
        set(newValue);
    }
    
    /**
     * Subscribes to state change events for this state instance.
     * 
     * @param handler event handler function
     * @return unsubscribe function
     */
    public ProxyObject subscribe(Value handler) {
        if (!handler.canExecute()) {
            throw new IllegalArgumentException("Handler must be an executable function");
        }
        
        return stateCoordinator.subscribeToState(stateName, handler);
    }
    
    /**
     * Creates a ProxyObject representing this State for JavaScript consumption.
     * 
     * @return proxy object with get, set, update, subscribe methods
     */
    public ProxyObject createProxy() {
        Map<String, Object> stateApi = new HashMap<>();
        
        // get() method
        stateApi.put("get", (ProxyExecutable) args -> {
            if (args.length != 0) {
                throw new IllegalArgumentException("state.get() requires no arguments");
            }
            return value;
        });
        
        // set(value) method
        stateApi.put("set", (ProxyExecutable) args -> {
            if (args.length != 1) {
                throw new IllegalArgumentException("state.set() requires exactly 1 argument: (value)");
            }
            
            Object newValue = args[0].asHostObject();
            set((T) newValue);
            return null;
        });
        
        // update(mutator) method
        stateApi.put("update", (ProxyExecutable) args -> {
            if (args.length != 1) {
                throw new IllegalArgumentException("state.update() requires exactly 1 argument: (mutator)");
            }
            
            Value mutator = args[0];
            update(mutator);
            return null;
        });
        
        // subscribe(handler) method
        stateApi.put("subscribe", (ProxyExecutable) args -> {
            if (args.length != 1) {
                throw new IllegalArgumentException("state.subscribe() requires exactly 1 argument: (handler)");
            }
            
            Value handler = args[0];
            return subscribe(handler);
        });
        
        return ProxyObject.fromMap(stateApi);
    }
    
    /**
     * Gets the state name for debugging purposes.
     * 
     * @return the state name
     */
    public String getStateName() {
        return stateName;
    }
}
