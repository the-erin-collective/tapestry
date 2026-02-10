package com.tapestry.extension;

import com.tapestry.api.TapestryAPI;
import com.tapestry.lifecycle.PhaseController;
import com.tapestry.lifecycle.TapestryPhase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ExtensionRegistryTest {
    
    private TapestryAPI api;
    private ExtensionRegistry registry;
    private PhaseController phaseController;
    
    @BeforeEach
    void setUp() {
        // Reset phase controller
        phaseController = PhaseController.getInstance();
        while (phaseController.getCurrentPhase() != TapestryPhase.BOOTSTRAP) {
            try {
                phaseController.advanceTo(TapestryPhase.BOOTSTRAP);
            } catch (IllegalStateException e) {
                break;
            }
        }
        
        api = new TapestryAPI();
        registry = new ExtensionRegistry(api);
        phaseController.advanceTo(TapestryPhase.DISCOVERY);
    }
    
    @Test
    void testDiscoveryValidExtension() {
        // Create a mock extension provider
        TapestryExtensionProvider provider = new TapestryExtensionProvider() {
            @Override
            public TapestryExtensionDescriptor describe() {
                return new TapestryExtensionDescriptor(
                    "testExtension",
                    List.of("worlds.fog", "worldgen.custom")
                );
            }
            
            @Override
            public void register(TapestryExtensionContext context) {
                context.extendDomain("worlds", "fog", Map.of("density", 0.5));
                context.registerModAPI("config", Map.of("enabled", true));
            }
        };
        
        // Simulate discovery
        registry.discoverExtensions();
        
        assertEquals(0, registry.getExtensionCount()); // No actual Fabric entrypoints in test
        assertTrue(registry.getExtensionIds().isEmpty());
        assertTrue(registry.getRegisteredCapabilities().isEmpty());
    }
    
    @Test
    void testDiscoveryInvalidPhase() {
        phaseController.advanceTo(TapestryPhase.REGISTRATION);
        
        assertThrows(IllegalStateException.class, () -> {
            registry.discoverExtensions();
        });
    }
    
    @Test
    void testRegistrationInvalidPhase() {
        phaseController.advanceTo(TapestryPhase.DISCOVERY);
        
        assertThrows(IllegalStateException.class, () -> {
            registry.registerExtensions();
        });
    }
    
    @Test
    void testRegistrationWithoutDiscovery() {
        phaseController.advanceTo(TapestryPhase.REGISTRATION);
        
        assertThrows(IllegalStateException.class, () -> {
            registry.registerExtensions();
        });
    }
    
    @Test
    void testDoubleDiscovery() {
        registry.discoverExtensions();
        
        // Should not throw, just log a warning
        assertDoesNotThrow(() -> {
            registry.discoverExtensions();
        });
        
        assertTrue(registry.isDiscoveryComplete());
    }
    
    @Test
    void testDoubleRegistration() {
        registry.discoverExtensions();
        phaseController.advanceTo(TapestryPhase.REGISTRATION);
        
        registry.registerExtensions();
        
        // Should not throw, just log a warning
        assertDoesNotThrow(() -> {
            registry.registerExtensions();
        });
        
        assertTrue(registry.isRegistrationComplete());
    }
    
    @Test
    void testExtensionDescriptorValidation() {
        // Test invalid descriptor
        TapestryExtensionProvider invalidProvider = new TapestryExtensionProvider() {
            @Override
            public TapestryExtensionDescriptor describe() {
                return new TapestryExtensionDescriptor(
                    "123invalid", // Invalid ID
                    List.of("worlds.fog")
                );
            }
            
            @Override
            public void register(TapestryExtensionContext context) {
                // Implementation not needed for this test
            }
        };
        
        // The validation would happen during discovery, but since we can't easily
        // mock FabricLoader in unit tests, we'll test the descriptor validation directly
        TapestryExtensionDescriptor invalidDescriptor = invalidProvider.describe();
        
        assertThrows(IllegalArgumentException.class, invalidDescriptor::validate);
    }
    
    @Test
    void testValidExtensionDescriptor() {
        TapestryExtensionProvider validProvider = new TestExtension();
        TapestryExtensionDescriptor descriptor = validProvider.describe();
        
        assertDoesNotThrow(descriptor::validate);
        assertEquals("testExtension", descriptor.id());
        assertEquals(List.of("worlds.fog", "worldgen.custom"), descriptor.capabilities());
    }
    
    @Test
    void testExtensionContext() {
        phaseController.advanceTo(TapestryPhase.REGISTRATION);
        
        TapestryExtensionContext context = new TapestryExtensionContext("testExtension", api);
        
        assertEquals("testExtension", context.getExtensionId());
        
        // Test valid operations
        assertDoesNotThrow(() -> {
            context.extendDomain("worlds", "fog", Map.of("density", 0.5));
            context.registerModAPI("config", Map.of("enabled", true));
        });
        
        assertEquals(1, api.getWorlds().size());
        assertEquals(1, api.getMods().size());
    }
    
    @Test
    void testExtensionContextInvalidPhase() {
        // Context should fail if not in REGISTRATION phase
        assertThrows(IllegalStateException.class, () -> {
            new TapestryExtensionContext("testExtension", api);
        });
    }
    
    @Test
    void testExtensionContextDuplicateDomainKey() {
        phaseController.advanceTo(TapestryPhase.REGISTRATION);
        
        TapestryExtensionContext context = new TapestryExtensionContext("testExtension", api);
        
        context.extendDomain("worlds", "fog", Map.of("density", 0.5));
        
        assertThrows(IllegalArgumentException.class, () -> {
            context.extendDomain("worlds", "fog", Map.of("density", 0.8));
        });
    }
    
    @Test
    void testExtensionContextDuplicateModAPIKey() {
        phaseController.advanceTo(TapestryPhase.REGISTRATION);
        
        TapestryExtensionContext context = new TapestryExtensionContext("testExtension", api);
        
        context.registerModAPI("config", Map.of("enabled", true));
        
        assertThrows(IllegalArgumentException.class, () -> {
            context.registerModAPI("config", Map.of("enabled", false));
        });
    }
    
    @Test
    void testExtensionContextInvalidDomain() {
        phaseController.advanceTo(TapestryPhase.REGISTRATION);
        
        TapestryExtensionContext context = new TapestryExtensionContext("testExtension", api);
        
        assertThrows(IllegalArgumentException.class, () -> {
            context.extendDomain("invalid", "key", "value");
        });
    }
    
    @Test
    void testCompleteFlow() {
        // Test the complete flow from discovery to registration
        registry.discoverExtensions();
        assertTrue(registry.isDiscoveryComplete());
        assertFalse(registry.isRegistrationComplete());
        
        phaseController.advanceTo(TapestryPhase.REGISTRATION);
        registry.registerExtensions();
        assertTrue(registry.isRegistrationComplete());
        
        // API should be frozen after registration in real flow
        // (but we don't test that here since we have no real extensions)
    }
}
