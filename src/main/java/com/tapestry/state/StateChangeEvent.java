package com.tapestry.state;

/**
 * Phase 12 State Change Event.
 * 
 * Represents a state mutation that occurred during event handling.
 * Emitted as a regular event through the EventBus.
 */
public class StateChangeEvent<T> {
    
    private final String type;
    private final String stateName;
    private final T oldValue;
    private final T newValue;
    private final long timestamp;
    
    /**
     * Creates a new StateChangeEvent.
     * 
     * @param stateName the name of the state that changed
     * @param oldValue the previous value
     * @param newValue the new value
     */
    public StateChangeEvent(String stateName, T oldValue, T newValue) {
        this.type = "state_change";
        this.stateName = stateName;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.timestamp = System.currentTimeMillis();
    }
    
    /**
     * Gets the event type (always "state_change").
     * 
     * @return the event type
     */
    public String getType() {
        return type;
    }
    
    /**
     * Gets the name of the state that changed.
     * 
     * @return the state name
     */
    public String getStateName() {
        return stateName;
    }
    
    /**
     * Gets the previous state value.
     * 
     * @return the old value
     */
    public T getOldValue() {
        return oldValue;
    }
    
    /**
     * Gets the new state value.
     * 
     * @return the new value
     */
    public T getNewValue() {
        return newValue;
    }
    
    /**
     * Gets the timestamp when the change occurred.
     * 
     * @return the timestamp in milliseconds
     */
    public long getTimestamp() {
        return timestamp;
    }
    
    @Override
    public String toString() {
        return String.format("StateChangeEvent{type='%s', stateName='%s', oldValue=%s, newValue=%s, timestamp=%d}",
                           type, stateName, oldValue, newValue, timestamp);
    }
}
