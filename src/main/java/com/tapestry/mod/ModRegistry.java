package com.tapestry.mod;

import com.tapestry.lifecycle.TapestryPhase;
import com.tapestry.lifecycle.PhaseController;
import com.tapestry.performance.PerformanceMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for managing mods through the two-pass loading process.
 * 
 * Handles registration (TS_REGISTER) and activation (TS_ACTIVATE) phases
 * with deterministic dependency resolution and strict lifecycle enforcement.
 */
public class ModRegistry {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ModRegistry.class);
    private static ModRegistry instance;
    
    // Registration state
    private final Map<String, ModDescriptor> registeredMods = new LinkedHashMap<>();
    private final Set<String> definedMods = ConcurrentHashMap.newKeySet();
    
    // Activation state
    private final List<ModDescriptor> activationOrder = new ArrayList<>();
    private final Map<String, Object> exportRegistry = new ConcurrentHashMap<>();
    
    // Capability registration state
    private boolean capabilityRegistrationComplete = false;
    
    // Performance monitoring
    private final PerformanceMonitor performanceMonitor = PerformanceMonitor.getInstance();
    
    // Performance tracking
    private long registrationStartTime;
    private long activationStartTime;
    
    private ModRegistry() {}
    
    public static synchronized ModRegistry getInstance() {
        if (instance == null) {
            instance = new ModRegistry();
        }
        return instance;
    }
    
    /**
     * Begins the registration phase. Clears any previous state.
     */
    public void beginRegistration() {
        PhaseController.getInstance().requirePhase(TapestryPhase.TS_REGISTER);
        
        LOGGER.info("Beginning mod registration phase");
        registrationStartTime = System.currentTimeMillis();
        
        // Clear previous state
        registeredMods.clear();
        definedMods.clear();
        activationOrder.clear();
        exportRegistry.clear();
    }
    
    /**
     * Registers a mod during TS_REGISTER phase.
     * 
     * @param descriptor the mod descriptor
     * @throws IllegalStateException if called outside TS_REGISTER
     * @throws IllegalArgumentException if mod ID is duplicate
     */
    public void registerMod(ModDescriptor descriptor) {
        PhaseController.getInstance().requirePhase(TapestryPhase.TS_REGISTER);
        
        // Check performance limits
        performanceMonitor.checkModCountLimit(registeredMods.size(), 1);
        
        String modId = descriptor.getId();
        
        if (registeredMods.containsKey(modId)) {
            throw new IllegalArgumentException("Duplicate mod ID: " + modId);
        }
        
        if (definedMods.contains(modId)) {
            throw new IllegalArgumentException("Mod already defined in this session: " + modId);
        }
        
        // Check for self-dependency
        List<String> dependsOn = descriptor.getDependsOn();
        if (dependsOn.contains(modId)) {
            throw new IllegalArgumentException("Mod '" + modId + "' cannot depend on itself");
        }
        
        registeredMods.put(modId, descriptor);
        definedMods.add(modId);
        
        LOGGER.debug("Registered mod: {} (version: {}, dependencies: {})", 
                    modId, descriptor.getVersion(), descriptor.getDependsOn());
    }
    
    /**
     * Marks that a mod has been defined via mod.define()
     * 
     * @param modId the mod ID
     * @return true if this is the first definition, false if already defined
     */
    public boolean defineMod(String modId) {
        PhaseController.getInstance().requirePhase(TapestryPhase.TS_REGISTER);
        
        return definedMods.add(modId);
    }
    
    /**
     * Checks if a mod has been defined.
     * 
     * @param modId the mod ID
     * @return true if defined
     */
    public boolean isModDefined(String modId) {
        return definedMods.contains(modId);
    }
    
    /**
     * Gets a registered mod descriptor.
     * 
     * @param modId the mod ID
     * @return the descriptor, or null if not found
     */
    public ModDescriptor getMod(String modId) {
        return registeredMods.get(modId);
    }
    
    /**
     * Gets all registered mods.
     * 
     * @return immutable map of mod ID to descriptor
     */
    public Map<String, ModDescriptor> getAllMods() {
        return Collections.unmodifiableMap(registeredMods);
    }
    
    /**
     * Validates the dependency graph and prepares for activation.
     * 
     * @throws IllegalStateException if dependency validation fails
     */
    public void validateDependencies() {
        PhaseController.getInstance().requirePhase(TapestryPhase.TS_REGISTER);
        
        LOGGER.info("Validating mod dependency graph");
        
        // Check for missing dependencies
        for (ModDescriptor mod : registeredMods.values()) {
            for (String dependency : mod.getDependsOn()) {
                if (!registeredMods.containsKey(dependency)) {
                    throw new IllegalStateException(
                        String.format("Mod '%s' depends on missing mod '%s'", mod.getId(), dependency));
                }
            }
        }
        
        // Check for circular dependencies
        detectCircularDependencies();
        
        LOGGER.info("Dependency validation passed for {} mods", registeredMods.size());
    }
    
    /**
     * Builds the activation order using topological sort.
     * 
     * @return list of mods in activation order
     */
    public List<ModDescriptor> buildActivationOrder() {
        PhaseController.getInstance().requirePhase(TapestryPhase.TS_REGISTER);
        
        LOGGER.info("Building mod activation order");
        
        // Kahn's algorithm for topological sort
        Map<String, Integer> inDegree = new HashMap<>();
        Queue<String> queue = new LinkedList<>();
        List<String> sorted = new ArrayList<>();
        
        // Calculate in-degrees
        for (ModDescriptor mod : registeredMods.values()) {
            inDegree.put(mod.getId(), 0);
        }
        
        for (ModDescriptor mod : registeredMods.values()) {
            for (String dependency : mod.getDependsOn()) {
                inDegree.put(mod.getId(), inDegree.get(mod.getId()) + 1);
            }
        }
        
        // Find nodes with no incoming edges
        for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.add(entry.getKey());
            }
        }
        
        // Process nodes
        while (!queue.isEmpty()) {
            String current = queue.poll();
            sorted.add(current);
            
            ModDescriptor currentMod = registeredMods.get(current);
            for (ModDescriptor mod : registeredMods.values()) {
                if (mod.getDependsOn().contains(current)) {
                    int newDegree = inDegree.get(mod.getId()) - 1;
                    inDegree.put(mod.getId(), newDegree);
                    if (newDegree == 0) {
                        queue.add(mod.getId());
                    }
                }
            }
        }
        
        // Check if all nodes were processed (no cycles)
        if (sorted.size() != registeredMods.size()) {
            throw new IllegalStateException("Circular dependency detected in mod graph");
        }
        
        // Convert to ModDescriptor list
        activationOrder.clear();
        for (String modId : sorted) {
            activationOrder.add(registeredMods.get(modId));
        }
        
        LOGGER.info("Activation order built: {}", 
                   activationOrder.stream().map(ModDescriptor::getId).toList());
        
        return Collections.unmodifiableList(activationOrder);
    }
    
    /**
     * Begins the activation phase.
     */
    public void beginActivation() {
        PhaseController.getInstance().requirePhase(TapestryPhase.TS_ACTIVATE);
        
        LOGGER.info("Beginning mod activation phase");
        activationStartTime = System.currentTimeMillis();
        
        if (activationOrder.isEmpty()) {
            throw new IllegalStateException("Activation order not built. Call buildActivationOrder() first.");
        }
    }
    
    /**
     * Gets the activation order.
     * 
     * @return immutable list of mods in activation order
     */
    public List<ModDescriptor> getActivationOrder() {
        return Collections.unmodifiableList(activationOrder);
    }
    
    /**
     * Registers an export from a mod.
     * 
     * @param modId the mod ID
     * @param key the export key
     * @param value the export value
     * @throws IllegalStateException if called outside TS_ACTIVATE
     */
    public void registerExport(String modId, String key, Object value) {
        PhaseController.getInstance().requirePhase(TapestryPhase.TS_ACTIVATE);
        
        String exportKey = modId + ":" + key;
        exportRegistry.put(exportKey, value);
        
        LOGGER.debug("Registered export: {} from mod {}", key, modId);
    }
    
    /**
     * Gets an export from a dependency mod.
     * 
     * @param modId the mod ID to get export from
     * @param key the export key
     * @return the export value
     * @throws IllegalStateException if called outside TS_ACTIVATE
     * @throws IllegalArgumentException if export not found
     */
    public Object requireExport(String modId, String key) {
        PhaseController.getInstance().requirePhase(TapestryPhase.TS_ACTIVATE);
        
        String exportKey = modId + ":" + key;
        Object export = exportRegistry.get(exportKey);
        
        if (export == null) {
            throw new IllegalArgumentException(
                String.format("Export '%s' not found from mod '%s'", key, modId));
        }
        
        return export;
    }
    
    /**
     * Marks capability registration phase as complete.
     * Called after all mods have registered their capabilities during TS_REGISTER phase.
     */
    public void completeCapabilityRegistration() {
        LOGGER.info("Capability registration phase completed");
        capabilityRegistrationComplete = true;
    }
    
    /**
     * Gets performance statistics.
     * 
     * @return performance stats
     */
    public ModRegistryStats getStats() {
        long registrationTime = registrationStartTime > 0 ? 
            System.currentTimeMillis() - registrationStartTime : 0;
        long activationTime = activationStartTime > 0 ? 
            System.currentTimeMillis() - activationStartTime : 0;
        
        return new ModRegistryStats(
            registeredMods.size(),
            activationOrder.size(),
            exportRegistry.size(),
            registrationTime,
            activationTime
        );
    }
    
    /**
     * Detects circular dependencies using DFS.
     */
    private void detectCircularDependencies() {
        Set<String> visiting = new HashSet<>();
        Set<String> visited = new HashSet<>();
        
        for (ModDescriptor mod : registeredMods.values()) {
            if (!visited.contains(mod.getId())) {
                if (hasCircularDependency(mod.getId(), visiting, visited)) {
                    throw new IllegalStateException("Circular dependency detected involving mod: " + mod.getId());
                }
            }
        }
    }
    
    private boolean hasCircularDependency(String modId, Set<String> visiting, Set<String> visited) {
        if (visiting.contains(modId)) {
            return true; // Found a cycle
        }
        
        if (visited.contains(modId)) {
            return false; // Already processed
        }
        
        visiting.add(modId);
        
        ModDescriptor mod = registeredMods.get(modId);
        if (mod != null) {
            for (String dependency : mod.getDependsOn()) {
                if (hasCircularDependency(dependency, visiting, visited)) {
                    return true;
                }
            }
        }
        
        visiting.remove(modId);
        visited.add(modId);
        return false;
    }
    
    /**
     * Performance statistics for the mod registry.
     */
    public record ModRegistryStats(
        int registeredCount,
        int activatedCount,
        int exportCount,
        long registrationTimeMs,
        long activationTimeMs
    ) {}
}
