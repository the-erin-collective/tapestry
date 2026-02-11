package com.tapestry.typescript;

import com.tapestry.hooks.HookRegistry;
import com.tapestry.lifecycle.PhaseController;
import com.tapestry.lifecycle.TapestryPhase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TsWorldgenApi.
 * 
 * Tests worldgen hook registration and phase enforcement.
 */
public class TsWorldgenApiTest {
    
    private HookRegistry hookRegistry;
    private TsWorldgenApi worldgenApi;
    private Object mockHandler;
    
    @BeforeEach
    void setUp() {
        // Reset phase controller for each test
        PhaseController.reset();
        
        hookRegistry = new HookRegistry();
        hookRegistry.allowRegistration();
        
        worldgenApi = new TsWorldgenApi(hookRegistry);
        mockHandler = createMockFunction("testHandler");
    }
    
    @Test
    void testOnResolveBlockInReadyPhase() {
        PhaseController.getInstance().advanceTo(TapestryPhase.DISCOVERY);
        PhaseController.getInstance().advanceTo(TapestryPhase.REGISTRATION);
        PhaseController.getInstance().advanceTo(TapestryPhase.FREEZE);
        PhaseController.getInstance().advanceTo(TapestryPhase.TS_LOAD);
        PhaseController.getInstance().advanceTo(TapestryPhase.TS_READY);
        
        assertDoesNotThrow(() -> 
            worldgenApi.onResolveBlock(mockHandler)
        );
    }
    
    @Test
    void testOnResolveBlockPhaseEnforcement() {
        // Should fail before TS_READY (due to phase check)
        assertThrows(IllegalStateException.class, () -> 
            worldgenApi.onResolveBlock(mockHandler)
        );
        
        // Advance through phases to TS_READY
        PhaseController.getInstance().advanceTo(TapestryPhase.DISCOVERY);
        PhaseController.getInstance().advanceTo(TapestryPhase.REGISTRATION);
        PhaseController.getInstance().advanceTo(TapestryPhase.FREEZE);
        PhaseController.getInstance().advanceTo(TapestryPhase.TS_LOAD);
        PhaseController.getInstance().advanceTo(TapestryPhase.TS_READY);
        
        // Should work during TS_READY (just logs, doesn't check hook registry)
        assertDoesNotThrow(() -> 
            worldgenApi.onResolveBlock(mockHandler)
        );
        
        // Note: The actual implementation doesn't check hookRegistry.allowRegistration()
        // It only checks the phase. The hookRegistry is not used in the current implementation.
    }
    
    @Test
    void testOnResolveBlockNullHandler() {
        PhaseController.getInstance().advanceTo(TapestryPhase.DISCOVERY);
        PhaseController.getInstance().advanceTo(TapestryPhase.REGISTRATION);
        PhaseController.getInstance().advanceTo(TapestryPhase.FREEZE);
        PhaseController.getInstance().advanceTo(TapestryPhase.TS_LOAD);
        PhaseController.getInstance().advanceTo(TapestryPhase.TS_READY);
        
        assertThrows(IllegalArgumentException.class, () -> 
            worldgenApi.onResolveBlock(null)
        );
    }
    
    @Test
    void testCreateApiObject() {
        Map<String, Object> api = worldgenApi.createApiObject();
        
        assertNotNull(api);
        assertTrue(api.containsKey("onResolveBlock"));
        assertNotNull(api.get("onResolveBlock"));
    }
    
    /**
     * Creates a mock JavaScript function value.
     * Since Value is final, we use a mock object instead.
     * 
     * @param name the function name
     * @return mock function object
     */
    private Object createMockFunction(String name) {
        return new Object() {
            public boolean canExecute() {
                return true;
            }
            
            public void execute(Object... arguments) {
                // Mock function does nothing
            }
            
            public boolean isNull() {
                return false;
            }
            
            public String asString() {
                return "mock:" + name;
            }
            
            public java.util.Set<String> getMemberKeys() {
                return java.util.Collections.emptySet();
            }
            
            public boolean hasMember(String member) {
                return false;
            }
        };
    }
}
