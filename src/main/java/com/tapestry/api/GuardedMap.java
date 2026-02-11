package com.tapestry.api;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * A map wrapper that prevents mutations after the API is frozen.
 * 
 * This solves the reference escape problem where code that captured
 * references to mutable maps during REGISTRATION could still mutate
 * them after FREEZE by calling methods on the original map.
 * 
 * The GuardedMap checks the frozen state on EVERY mutation operation,
 * not just on the returned unmodifiable view.
 */
public class GuardedMap<K, V> implements Map<K, V> {
    
    private final Map<K, V> delegate;
    private volatile boolean frozen = false;
    
    public GuardedMap() {
        this.delegate = new HashMap<>();
    }
    
    /**
     * Sets the frozen state. Once frozen, all mutations are blocked.
     * 
     * @param frozen whether to freeze this map
     */
    public void setFrozen(boolean frozen) {
        this.frozen = frozen;
    }
    
    /**
     * Checks if this map is frozen.
     * 
     * @return true if frozen
     */
    public boolean isFrozen() {
        return frozen;
    }
    
    // Mutation operations - all check frozen state
    @Override
    public V put(K key, V value) {
        if (frozen) {
            throw new IllegalStateException("Cannot modify frozen map");
        }
        return delegate.put(key, value);
    }
    
    @Override
    public V remove(Object key) {
        if (frozen) {
            throw new IllegalStateException("Cannot modify frozen map");
        }
        return delegate.remove(key);
    }
    
    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        if (frozen) {
            throw new IllegalStateException("Cannot modify frozen map");
        }
        delegate.putAll(m);
    }
    
    @Override
    public void clear() {
        if (frozen) {
            throw new IllegalStateException("Cannot modify frozen map");
        }
        delegate.clear();
    }
    
    // Read operations - always allowed
    @Override
    public int size() {
        return delegate.size();
    }
    
    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }
    
    @Override
    public boolean containsKey(Object key) {
        return delegate.containsKey(key);
    }
    
    @Override
    public boolean containsValue(Object value) {
        return delegate.containsValue(value);
    }
    
    @Override
    public V get(Object key) {
        return delegate.get(key);
    }
    
    @Override
    public K[] toArray() {
        return delegate.keySet().toArray(new K[0]);
    }
    
    @Override
    public <T> T[] toArray(T[] a) {
        return delegate.keySet().toArray(a);
    }
    
    @Override
    public boolean equals(Object o) {
        return delegate.equals(o);
    }
    
    @Override
    public int hashCode() {
        return delegate.hashCode();
    }
    
    @Override
    public String toString() {
        return delegate.toString();
    }
    
    @Override
    public V getOrDefault(Object key, V defaultValue) {
        return delegate.getOrDefault(key, defaultValue);
    }
    
    @Override
    public void forEach(BiConsumer<? super K, ? super V> action) {
        delegate.forEach(action);
    }
    
    @Override
    public java.util.Set<K> keySet() {
        return Collections.unmodifiableSet(delegate.keySet());
    }
    
    @Override
    public java.util.Collection<V> values() {
        return Collections.unmodifiableCollection(delegate.values());
    }
    
    @Override
    public java.util.Set<java.util.Map.Entry<K, V>> entrySet() {
        return Collections.unmodifiableSet(delegate.entrySet());
    }
    
    /**
     * Returns an unmodifiable view of this map.
     * This is safe because the GuardedMap itself prevents mutations.
     * 
     * @return unmodifiable view
     */
    public Map<K, V> unmodifiableView() {
        return Collections.unmodifiableMap(delegate);
    }
}
