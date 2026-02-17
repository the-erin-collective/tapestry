package com.tapestry.mod;

import org.graalvm.polyglot.Value;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Describes a mod's metadata and lifecycle state for Phase 10.5.
 * 
 * This class captures all information collected during TS_REGISTER
 * and maintains the mod's state through activation and runtime.
 */
public class ModDescriptor {
    
    private final String id;
    private final String version;
    private final List<String> dependsOn;
    private final String entryPath;
    private final String sourcePath;
    
    // Lifecycle functions stored during registration
    private Value activateFunction;
    private Value deactivateFunction;
    
    // Runtime state
    private ModState state = ModState.REGISTERED;
    private Map<String, Object> exports = new ConcurrentHashMap<>();
    
    /**
     * Represents the current state of a mod in the lifecycle.
     */
    public enum ModState {
        REGISTERED,   // Metadata collected during TS_REGISTER
        ACTIVATING,   // Currently executing activate function
        ACTIVE,      // Successfully activated
        FAILED,      // Activation failed
        DEACTIVATED  // Deactivated (if supported)
    }
    
    public ModDescriptor(String id, String version, List<String> dependsOn, String entryPath, String sourcePath) {
        this.id = id;
        this.version = version;
        this.dependsOn = dependsOn;
        this.entryPath = entryPath;
        this.sourcePath = sourcePath;
    }
    
    // Getters
    public String getId() { return id; }
    public String getVersion() { return version; }
    public List<String> getDependsOn() { return dependsOn; }
    public List<String> dependencies() { return dependsOn; } // Alias for compatibility
    public String getEntryPath() { return entryPath; }
    public String getSourcePath() { return sourcePath; }
    public Value getActivateFunction() { return activateFunction; }
    public Value getDeactivateFunction() { return deactivateFunction; }
    public ModState getState() { return state; }
    public Map<String, Object> getExports() { return exports; }
    
    // Setters for lifecycle functions
    public void setActivateFunction(Value activateFunction) {
        this.activateFunction = activateFunction;
    }
    
    public void setDeactivateFunction(Value deactivateFunction) {
        this.deactivateFunction = deactivateFunction;
    }
    
    // State management
    public void setState(ModState state) {
        this.state = state;
    }
    
    public void setExports(Map<String, Object> exports) {
        this.exports = new ConcurrentHashMap<>(exports);
    }
    
    // Utility methods
    public boolean hasActivateFunction() {
        return activateFunction != null && activateFunction.canExecute();
    }
    
    public boolean hasDeactivateFunction() {
        return deactivateFunction != null && deactivateFunction.canExecute();
    }
    
    public boolean isActive() {
        return state == ModState.ACTIVE;
    }
    
    public boolean hasDependency(String modId) {
        return dependsOn.contains(modId);
    }
    
    @Override
    public String toString() {
        return String.format("ModDescriptor{id='%s', version='%s', state=%s, dependencies=%s}", 
                           id, version, state, dependsOn);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        ModDescriptor that = (ModDescriptor) obj;
        return id.equals(that.id);
    }
    
    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
