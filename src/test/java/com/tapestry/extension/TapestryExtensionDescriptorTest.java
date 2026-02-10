package com.tapestry.extension;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TapestryExtensionDescriptorTest {
    
    @Test
    void testValidDescriptor() {
        // Should not throw
        assertDoesNotThrow(() -> {
            TapestryExtensionDescriptor descriptor = new TapestryExtensionDescriptor(
                "testExtension",
                List.of("worlds.fog", "worldgen.custom")
            );
            descriptor.validate();
        });
    }
    
    @Test
    void testNullId() {
        TapestryExtensionDescriptor descriptor = new TapestryExtensionDescriptor(
            null,
            List.of("worlds.fog")
        );
        
        assertThrows(IllegalArgumentException.class, descriptor::validate);
    }
    
    @Test
    void testEmptyId() {
        TapestryExtensionDescriptor descriptor = new TapestryExtensionDescriptor(
            "",
            List.of("worlds.fog")
        );
        
        assertThrows(IllegalArgumentException.class, descriptor::validate);
    }
    
    @Test
    void testInvalidIdPattern() {
        // Test various invalid patterns
        String[] invalidIds = {
            "123invalid",    // starts with number
            "invalid-name",  // contains hyphen
            "invalid.name",  // contains dot
            "invalid space", // contains space
            "invalid$",      // contains special char
            "_invalid"       // starts with underscore (but this is actually valid)
        };
        
        for (String invalidId : invalidIds) {
            if (!invalidId.equals("_invalid")) { // _invalid is actually valid
                TapestryExtensionDescriptor descriptor = new TapestryExtensionDescriptor(
                    invalidId,
                    List.of("worlds.fog")
                );
                
                assertThrows(IllegalArgumentException.class, 
                    invalidId + " should be invalid", 
                    descriptor::validate);
            }
        }
    }
    
    @Test
    void testValidIdPatterns() {
        String[] validIds = {
            "validExtension",
            "valid_extension",
            "validExtension123",
            "_valid",        // starts with underscore is valid
            "a",             // single letter
            "extension_123_test"
        };
        
        for (String validId : validIds) {
            TapestryExtensionDescriptor descriptor = new TapestryExtensionDescriptor(
                validId,
                List.of("worlds.fog")
            );
            
            assertDoesNotThrow(validId + " should be valid", descriptor::validate);
        }
    }
    
    @Test
    void testNullCapabilities() {
        TapestryExtensionDescriptor descriptor = new TapestryExtensionDescriptor(
            "testExtension",
            null
        );
        
        assertThrows(IllegalArgumentException.class, descriptor::validate);
    }
    
    @Test
    void testNullCapability() {
        TapestryExtensionDescriptor descriptor = new TapestryExtensionDescriptor(
            "testExtension",
            List.of("worlds.fog", null, "worldgen.custom")
        );
        
        assertThrows(IllegalArgumentException.class, descriptor::validate);
    }
    
    @Test
    void testEmptyCapability() {
        TapestryExtensionDescriptor descriptor = new TapestryExtensionDescriptor(
            "testExtension",
            List.of("worlds.fog", "", "worldgen.custom")
        );
        
        assertThrows(IllegalArgumentException.class, descriptor::validate);
    }
    
    @Test
    void testInvalidCapabilityPattern() {
        String[] invalidCapabilities = {
            "Worlds.fog",        // starts with uppercase
            "worlds.Fog",        // second part starts with uppercase
            "worlds_fog",        // underscore instead of dot
            "worlds.fog.extra",  // too many parts
            "worlds",            // no dot
            "worlds.",           // ends with dot
            ".fog",              // starts with dot
            "worlds.fog-name",   // hyphen in second part
            "worlds.123fog",     // starts with number in second part
            "123worlds.fog"      // starts with number in first part
        };
        
        for (String invalidCapability : invalidCapabilities) {
            TapestryExtensionDescriptor descriptor = new TapestryExtensionDescriptor(
                "testExtension",
                List.of(invalidCapability)
            );
            
            assertThrows(IllegalArgumentException.class, 
                invalidCapability + " should be invalid", 
                descriptor::validate);
        }
    }
    
    @Test
    void testValidCapabilityPatterns() {
        String[] validCapabilities = {
            "worlds.fog",
            "worldgen.custom",
            "events.player",
            "core.system",
            "a.b",               // minimal valid
            "worlds.fog_density", // underscore in second part
            "worlds123.fog456",   // numbers allowed
            "worlds_.fog",        // underscore at end of first part
            "worlds.fog_"         // underscore at end of second part
        };
        
        for (String validCapability : validCapabilities) {
            TapestryExtensionDescriptor descriptor = new TapestryExtensionDescriptor(
                "testExtension",
                List.of(validCapability)
            );
            
            assertDoesNotThrow(validCapability + " should be valid", descriptor::validate);
        }
    }
    
    @Test
    void testDuplicateCapabilities() {
        TapestryExtensionDescriptor descriptor = new TapestryExtensionDescriptor(
            "testExtension",
            List.of("worlds.fog", "worldgen.custom", "worlds.fog")
        );
        
        assertThrows(IllegalArgumentException.class, descriptor::validate);
    }
    
    @Test
    void testEmptyCapabilitiesList() {
        // Empty list should be valid
        TapestryExtensionDescriptor descriptor = new TapestryExtensionDescriptor(
            "testExtension",
            List.of()
        );
        
        assertDoesNotThrow(descriptor::validate);
    }
    
    @Test
    void testRecordMethods() {
        TapestryExtensionDescriptor descriptor1 = new TapestryExtensionDescriptor(
            "testExtension",
            List.of("worlds.fog", "worldgen.custom")
        );
        
        TapestryExtensionDescriptor descriptor2 = new TapestryExtensionDescriptor(
            "testExtension",
            List.of("worlds.fog", "worldgen.custom")
        );
        
        TapestryExtensionDescriptor descriptor3 = new TapestryExtensionDescriptor(
            "otherExtension",
            List.of("worlds.fog", "worldgen.custom")
        );
        
        // Test equals
        assertEquals(descriptor1, descriptor2);
        assertNotEquals(descriptor1, descriptor3);
        
        // Test hashCode
        assertEquals(descriptor1.hashCode(), descriptor2.hashCode());
        
        // Test toString
        String toString = descriptor1.toString();
        assertTrue(toString.contains("testExtension"));
        assertTrue(toString.contains("worlds.fog"));
        assertTrue(toString.contains("worldgen.custom"));
    }
}
