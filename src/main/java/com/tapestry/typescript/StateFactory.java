package com.tapestry.typescript;

import com.tapestry.events.EventBus;
import com.tapestry.state.State;
import com.tapestry.state.StateCoordinator;
import com.tapestry.lifecycle.TapestryPhase;
import com.tapestry.lifecycle.PhaseController;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Phase 12 State Factory for creating State instances in mods.
 * 
 * Provides the createState() function that mods use to create
 * transactional state instances tied to the global EventBus.
 */
public class StateFactory {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(StateFactory.class);
    
    private final EventBus eventBus;
    private final StateCoordinator stateCoordinator;
    
    /**
     * Creates a new StateFactory.
     * 
     * @param eventBus the EventBus for state coordination
     */
    public StateFactory(EventBus eventBus) {
        this.eventBus = eventBus;
        this.stateCoordinator = eventBus.getStateCoordinator();
    }
    
    /**
     * Creates the state namespace object for JavaScript mods.
     * 
     * @return ProxyObject with createState function
     */
    public ProxyObject createStateNamespace() {
        Map<String, Object> stateApi = new HashMap<>();
        
        // createState(stateName, initialValue) function
        stateApi.put("createState", (ProxyExecutable) args -> {
            if (args.length != 2) {
                throw new IllegalArgumentException("createState requires exactly 2 arguments: (stateName, initialValue)");
            }
            
            String stateName = args[0].asString();
            Value initialValue = args[1];
            
            // Validate phase - state creation allowed during TS_ACTIVATE, CLIENT_PRESENTATION_READY, RUNTIME
            TapestryPhase currentPhase = PhaseController.getInstance().getCurrentPhase();
            if (currentPhase != TapestryPhase.TS_ACTIVATE && 
                currentPhase != TapestryPhase.CLIENT_PRESENTATION_READY && 
                currentPhase != TapestryPhase.RUNTIME) {
                throw new IllegalStateException(
                    String.format("createState is only available during TS_ACTIVATE, CLIENT_PRESENTATION_READY, or RUNTIME phases. Current phase: %s", 
                                 currentPhase));
            }
            
            // Get current mod ID from execution context
            String modId = com.tapestry.typescript.TypeScriptRuntime.getCurrentModId();
            if (modId == null) {
                throw new IllegalStateException("createState must be called from within a mod context");
            }
            
            // Create State instance
            State<Object> state = new State<>(stateName, initialValue.asHostObject(), stateCoordinator);
            
            // Return the State proxy object
            return state.createProxy();
        });
        
        return ProxyObject.fromMap(stateApi);
    }
}
