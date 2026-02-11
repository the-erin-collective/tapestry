package com.tapestry.api;

import com.tapestry.lifecycle.PhaseController;
import com.tapestry.lifecycle.TapestryPhase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TapestryAPITest {
    
    private TapestryAPI api;
    private PhaseController phaseController;
    
    @BeforeEach
    void setUp() {
        // Reset phase controller for each test
        phaseController = PhaseController.getInstance();
        while (phaseController.getCurrentPhase() != TapestryPhase.BOOTSTRAP) {
            try {
                phaseController.advanceTo(TapestryPhase.BOOTSTRAP);
            } catch (IllegalStateException e) {
                break;
            }
        }
        
        api = new TapestryAPI();
        phaseController.advanceTo(TapestryPhase.REGISTRATION);
    }
    
    @Test
    void testInitialState() {
        assertFalse(api.isFrozen());
        
        // Check that all domains exist and are empty
        assertTrue(api.getWorlds().isEmpty());
        assertTrue(api.getWorldgen().isEmpty());
        assertTrue(api.getEvents().isEmpty());
        assertTrue(api.getMods().isEmpty());
        
        // Core should have phases and capabilities
        assertFalse(api.getCore().isEmpty());
        assertTrue(api.getCore().containsKey("phases"));
        assertTrue(api.getCore().containsKey("capabilities"));
    }
    
    @Test
    void testExtendDomainValid() {
        assertDoesNotThrow(() -> {
            api.extendDomain("worlds", "fog", Map.of("density", 0.5));
        });
        
        assertEquals(1, api.getWorlds().size());
        assertEquals(Map.of("density", 0.5), api.getWorlds().get("fog"));
    }
    
    @Test
    void testExtendDomainInvalidPhase() {
        phaseController.advanceTo(TapestryPhase.FREEZE);
        
        assertThrows(IllegalStateException.class, () -> {
            api.extendDomain("worlds", "fog", Map.of("density", 0.5));
        });
    }
    
    @Test
    void testExtendDomainAfterFreeze() {
        api.extendDomain("worlds", "fog", Map.of("density", 0.5));
        api.freeze();
        
        assertThrows(IllegalStateException.class, () -> {
            api.extendDomain("worlds", "weather", Map.of("rain", true));
        });
    }
    
    @Test
    void testExtendDomainDuplicateKey() {
        api.extendDomain("worlds", "fog", Map.of("density", 0.5));
        
        assertThrows(IllegalArgumentException.class, () -> {
            api.extendDomain("worlds", "fog", Map.of("density", 0.8));
        });
    }
    
    @Test
    void testExtendDomainInvalidDomain() {
        assertThrows(IllegalArgumentException.class, () -> {
            api.extendDomain("invalid", "key", "value");
        });
    }
    
    @Test
    void testRegisterModAPIValid() {
        assertDoesNotThrow(() -> {
            api.registerModAPI("testExtension", "config", Map.of("enabled", true));
        });
        
        assertEquals(1, api.getMods().size());
        assertTrue(api.getMods().containsKey("testExtension"));
        assertEquals(Map.of("enabled", true), api.getMods().get("testExtension").get("config"));
    }
    
    @Test
    void testRegisterModAPIInvalidPhase() {
        phaseController.advanceTo(TapestryPhase.FREEZE);
        
        assertThrows(IllegalStateException.class, () -> {
            api.registerModAPI("testExtension", "config", Map.of("enabled", true));
        });
    }
    
    @Test
    void testRegisterModAPIAfterFreeze() {
        api.registerModAPI("testExtension", "config", Map.of("enabled", true));
        api.freeze();
        
        assertThrows(IllegalStateException.class, () -> {
            api.registerModAPI("testExtension", "other", Map.of("value", 42));
        });
    }
    
    @Test
    void testRegisterModAPIDuplicateKey() {
        api.registerModAPI("testExtension", "config", Map.of("enabled", true));
        
        assertThrows(IllegalArgumentException.class, () -> {
            api.registerModAPI("testExtension", "config", Map.of("enabled", false));
        });
    }
    
    @Test
    void testRegisterModAPIDifferentExtensions() {
        api.registerModAPI("extension1", "config", Map.of("enabled", true));
        api.registerModAPI("extension2", "config", Map.of("enabled", false));
        
        assertEquals(2, api.getMods().size());
        assertTrue(api.getMods().containsKey("extension1"));
        assertTrue(api.getMods().containsKey("extension2"));
        // Same key in different extensions should be allowed
        assertEquals(Map.of("enabled", true), api.getMods().get("extension1").get("config"));
        assertEquals(Map.of("enabled", false), api.getMods().get("extension2").get("config"));
    }
    
    @Test
    void testFreeze() {
        // Add some data
        api.extendDomain("worlds", "fog", Map.of("density", 0.5));
        api.registerModAPI("testExtension", "config", Map.of("enabled", true));
        
        assertFalse(api.isFrozen());
        
        api.freeze();
        
        assertTrue(api.isFrozen());
        
        // Verify data is still accessible
        assertEquals(1, api.getWorlds().size());
        assertEquals(1, api.getMods().size());
    }
    
    @Test
    void testFreezeInvalidPhase() {
        phaseController.advanceTo(TapestryPhase.DISCOVERY);
        
        assertThrows(IllegalStateException.class, () -> {
            api.freeze();
        });
    }
    
    @Test
    void testDoubleFreeze() {
        api.freeze();
        
        // Should not throw, just log a warning
        assertDoesNotThrow(() -> {
            api.freeze();
        });
        
        assertTrue(api.isFrozen());
    }
    
    @Test
    void testDomainImmutability() {
        api.extendDomain("worlds", "fog", Map.of("density", 0.5));
        api.freeze();
        
        Map<String, Object> worlds = api.getWorlds();
        
        // The returned map should be unmodifiable
        assertThrows(UnsupportedOperationException.class, () -> {
            worlds.put("weather", Map.of("rain", true));
        });
        
        // But the original GuardedMap should still be frozen
        assertThrows(IllegalStateException.class, () -> {
            api.extendDomain("worlds", "weather", Map.of("rain", true));
        });
    }
    
    @Test
    void testModsImmutability() {
        api.registerModAPI("testExtension", "config", Map.of("enabled", true));
        api.freeze();
        
        Map<String, Map<String, Object>> mods = api.getMods();
        
        // The returned map should be unmodifiable
        assertThrows(UnsupportedOperationException.class, () -> {
            mods.put("otherExtension", Map.of("config", Map.of("value", 42)));
        });
        
        // The individual extension maps should also be unmodifiable
        Map<String, Object> extensionMap = mods.get("testExtension");
        assertThrows(UnsupportedOperationException.class, () -> {
            extensionMap.put("other", Map.of("value", 42));
        });
        
        // But the original GuardedMap should still be frozen
        assertThrows(IllegalStateException.class, () -> {
            api.registerModAPI("testExtension", "other", Map.of("value", 42));
        });
    }
    
    @Test
    void testCoreImmutability() {
        api.freeze();
        
        Map<String, Object> core = api.getCore();
        
        // The returned map should be unmodifiable
        assertThrows(UnsupportedOperationException.class, () -> {
            core.put("newKey", "newValue");
        });
    }
    
    @Test
    void testAllDomains() {
        // Test extending all valid domains
        api.extendDomain("worlds", "fog", Map.of("density", 0.5));
        api.extendDomain("worldgen", "custom", Map.of("type", "special"));
        api.extendDomain("events", "player", Map.of("join", true));
        api.extendDomain("core", "system", Map.of("version", "1.0"));
        
        assertEquals(1, api.getWorlds().size());
        assertEquals(1, api.getWorldgen().size());
        assertEquals(1, api.getEvents().size());
        
        // Core should have original data plus our addition
        assertTrue(api.getCore().size() >= 3); // phases, capabilities, and our addition
        assertTrue(api.getCore().containsKey("system"));
    }
}
