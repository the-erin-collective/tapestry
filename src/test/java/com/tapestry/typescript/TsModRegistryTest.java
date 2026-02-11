package com.tapestry.typescript;

import com.tapestry.lifecycle.PhaseController;
import com.tapestry.lifecycle.TapestryPhase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TsModRegistry.
 */
public class TsModRegistryTest {
    
    private TsModRegistry registry;
    
    @BeforeEach
    void setUp() {
        PhaseController.reset();
        registry = new TsModRegistry();
    }
    
    @Test
    void testRegisterMod() {
        PhaseController.getInstance().advanceTo(TapestryPhase.DISCOVERY);
        PhaseController.getInstance().advanceTo(TapestryPhase.REGISTRATION);
        PhaseController.getInstance().advanceTo(TapestryPhase.FREEZE);
        PhaseController.getInstance().advanceTo(TapestryPhase.TS_LOAD);
        
        Object mockOnLoad = createMockFunction("onLoad");
        Object mockOnEnable = createMockFunction("onEnable");
        
        TsModDefinition mod = new TsModDefinition("test_mod", mockOnLoad, mockOnEnable, "test_source");
        
        assertDoesNotThrow(() -> registry.registerMod(mod));
        assertEquals(1, registry.getMods().size());
        assertEquals("test_mod", registry.getMods().get(0).id());
    }
    
    @Test
    void testRegisterModInvalidId() {
        PhaseController.getInstance().advanceTo(TapestryPhase.DISCOVERY);
        PhaseController.getInstance().advanceTo(TapestryPhase.REGISTRATION);
        PhaseController.getInstance().advanceTo(TapestryPhase.FREEZE);
        PhaseController.getInstance().advanceTo(TapestryPhase.TS_LOAD);
        
        Object mockOnLoad = createMockFunction("onLoad");
        
        // Test invalid IDs that pass TsModDefinition constructor but fail registry validation
        assertThrows(IllegalArgumentException.class, () -> {
            TsModDefinition mod = new TsModDefinition("123invalid", mockOnLoad, null, "test_source");
            registry.registerMod(mod);
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            TsModDefinition mod = new TsModDefinition("invalid-id", mockOnLoad, null, "test_source");
            registry.registerMod(mod);
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            TsModDefinition mod = new TsModDefinition("very_long_name_that_is_actually_over_sixty_four_characters_limit_1234567890", 
                mockOnLoad, null, "test_source");
            registry.registerMod(mod);
        });
    }
    
    @Test
    void testRegisterModDuplicateId() {
        PhaseController.getInstance().advanceTo(TapestryPhase.DISCOVERY);
        PhaseController.getInstance().advanceTo(TapestryPhase.REGISTRATION);
        PhaseController.getInstance().advanceTo(TapestryPhase.FREEZE);
        PhaseController.getInstance().advanceTo(TapestryPhase.TS_LOAD);
        
        Object mockOnLoad1 = createMockFunction("onLoad1");
        Object mockOnLoad2 = createMockFunction("onLoad2");
        
        TsModDefinition mod1 = new TsModDefinition("test_mod", mockOnLoad1, null, "test_source1");
        TsModDefinition mod2 = new TsModDefinition("test_mod", mockOnLoad2, null, "test_source2");
        
        registry.registerMod(mod1);
        
        assertThrows(IllegalArgumentException.class, () -> registry.registerMod(mod2));
        assertEquals(1, registry.getMods().size());
    }
    
    @Test
    void testRegisterModPhaseEnforcement() {
        Object mockOnLoad = createMockFunction("onLoad");
        TsModDefinition mod = new TsModDefinition("test_mod", mockOnLoad, null, "test_source");
        
        // Should work before discovery is complete (regardless of phase)
        assertDoesNotThrow(() -> registry.registerMod(mod));
        
        // Should fail after discovery is complete
        registry.completeDiscovery();
        assertThrows(IllegalStateException.class, () -> registry.registerMod(mod));
    }
    
    @Test
    void testGetModsDeterministicOrder() {
        PhaseController.getInstance().advanceTo(TapestryPhase.DISCOVERY);
        PhaseController.getInstance().advanceTo(TapestryPhase.REGISTRATION);
        PhaseController.getInstance().advanceTo(TapestryPhase.FREEZE);
        PhaseController.getInstance().advanceTo(TapestryPhase.TS_LOAD);
        
        // Register mods in non-alphabetical order
        Object mockOnLoad = createMockFunction("onLoad");
        
        registry.registerMod(new TsModDefinition("z_mod", mockOnLoad, null, "source_z"));
        registry.registerMod(new TsModDefinition("a_mod", mockOnLoad, null, "source_a"));
        registry.registerMod(new TsModDefinition("m_mod", mockOnLoad, null, "source_m"));
        
        var mods = registry.getMods();
        assertEquals(3, mods.size());
        
        // Should be in alphabetical order
        assertEquals("a_mod", mods.get(0).id());
        assertEquals("m_mod", mods.get(1).id());
        assertEquals("z_mod", mods.get(2).id());
    }
    
    @Test
    void testTsModDefinition() {
        Object mockOnLoad = createMockFunction("onLoad");
        Object mockOnEnable = createMockFunction("onEnable");
        
        TsModDefinition mod = new TsModDefinition("test_mod", mockOnLoad, mockOnEnable, "test_source");
        
        assertEquals("test_mod", mod.id());
        assertEquals(mockOnLoad, mod.getOnLoad());
        assertEquals(mockOnEnable, mod.getOnEnable());
        assertEquals("test_source", mod.source());
        assertTrue(mod.hasOnEnable());
        
        // Test with null onEnable
        TsModDefinition modWithoutEnable = new TsModDefinition("test_mod2", mockOnLoad, null, "test_source2");
        assertEquals("test_mod2", modWithoutEnable.id());
        assertEquals(mockOnLoad, modWithoutEnable.getOnLoad());
        assertNull(modWithoutEnable.getOnEnable());
        assertFalse(modWithoutEnable.hasOnEnable());
    }
    
    /**
     * Creates a mock JavaScript function value.
     * Since Value is final, we use a mock object instead.
     * 
     * @param name function name
     * @return mock Value that can execute
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
