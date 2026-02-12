package com.tapestry.extensions;

import com.tapestry.lifecycle.PhaseController;
import com.tapestry.lifecycle.TapestryPhase;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for ExtensionValidator functionality.
 */
public class ExtensionValidatorTest {
    
    private ExtensionValidator validator;
    private ValidationPolicy policy;
    private Version currentVersion;
    
    @BeforeEach
    void setUp() {
        // Reset phase controller for each test
        PhaseController.reset();
        // Advance through all phases to VALIDATION
        PhaseController.getInstance().advanceTo(TapestryPhase.BOOTSTRAP);
        PhaseController.getInstance().advanceTo(TapestryPhase.DISCOVERY);
        PhaseController.getInstance().advanceTo(TapestryPhase.VALIDATION);
        
        currentVersion = Version.parse("0.3.0");
        policy = new ValidationPolicy(false, true, true);
        validator = new ExtensionValidator(currentVersion, policy);
    }
    
    @Test
    void testValidExtension() {
        // Create a valid extension descriptor
        var descriptor = new TapestryExtensionDescriptor(
            "test_extension",
            "Test Extension",
            "1.0.0",
            "0.1.0", // lower than current, should be valid
            List.of(new CapabilityDecl("test.hook", CapabilityType.HOOK, false, Map.of())),
            List.of(),
            List.of()
        );
        
        var mockContainer = mock(ModContainer.class);
        when(mockContainer.getMetadata()).thenReturn(mock(ModMetadata.class));
        when(mockContainer.getMetadata().getId()).thenReturn("testmod");
        
        var provider = mock(TapestryExtensionProvider.class);
        when(provider.describe()).thenReturn(descriptor);
        
        var discoveredProvider = new DiscoveredExtensionProvider(provider, mockContainer, descriptor);
        
        // Validate
        var result = validator.validate(List.of(discoveredProvider));
        
        // Should be enabled
        assertTrue(result.enabled().containsKey("test_extension"));
        assertFalse(result.rejected().containsKey("test_extension"));
        assertTrue(result.warnings().isEmpty());
    }
    
    @Test
    void testInvalidExtensionId() {
        // Create extension with invalid ID (uppercase)
        var descriptor = new TapestryExtensionDescriptor(
            "Test_Extension", // invalid - contains uppercase
            "Test Extension",
            "1.0.0",
            "0.1.0",
            List.of(new CapabilityDecl("test.hook", CapabilityType.HOOK, false, Map.of())),
            List.of(),
            List.of()
        );
        
        var mockContainer = mock(ModContainer.class);
        when(mockContainer.getMetadata()).thenReturn(mock(ModMetadata.class));
        when(mockContainer.getMetadata().getId()).thenReturn("testmod");
        
        var provider = mock(TapestryExtensionProvider.class);
        when(provider.describe()).thenReturn(descriptor);
        
        var discoveredProvider = new DiscoveredExtensionProvider(provider, mockContainer, descriptor);
        
        // Validate
        var result = validator.validate(List.of(discoveredProvider));
        
        // Should be rejected
        assertFalse(result.enabled().containsKey("Test_Extension"));
        assertTrue(result.rejected().containsKey("Test_Extension"));
        
        var rejected = result.rejected().get("Test_Extension");
        assertTrue(rejected.errors().stream()
            .anyMatch(e -> e.code().equals("INVALID_ID")));
    }
    
    @Test
    void testVersionTooLow() {
        // Create extension requiring higher version than current
        var descriptor = new TapestryExtensionDescriptor(
            "test_extension",
            "Test Extension",
            "1.0.0",
            "1.0.0", // higher than current 0.3.0
            List.of(new CapabilityDecl("test.hook", CapabilityType.HOOK, false, Map.of())),
            List.of(),
            List.of()
        );
        
        var mockContainer = mock(ModContainer.class);
        when(mockContainer.getMetadata()).thenReturn(mock(ModMetadata.class));
        when(mockContainer.getMetadata().getId()).thenReturn("testmod");
        
        var provider = mock(TapestryExtensionProvider.class);
        when(provider.describe()).thenReturn(descriptor);
        
        var discoveredProvider = new DiscoveredExtensionProvider(provider, mockContainer, descriptor);
        
        // Validate
        var result = validator.validate(List.of(discoveredProvider));
        
        // Should be rejected
        assertFalse(result.enabled().containsKey("test_extension"));
        assertTrue(result.rejected().containsKey("test_extension"));
        
        var rejected = result.rejected().get("test_extension");
        assertTrue(rejected.errors().stream()
            .anyMatch(e -> e.code().equals("VERSION_TOO_LOW")));
    }
    
    @Test
    void testCapabilityConflict() {
        // Create two extensions with same exclusive capability
        var capability = new CapabilityDecl("test.service", CapabilityType.SERVICE, true, Map.of());
        
        var descriptor1 = new TapestryExtensionDescriptor(
            "extension1",
            "Extension 1",
            "1.0.0",
            "0.1.0",
            List.of(capability),
            List.of(),
            List.of()
        );
        
        var descriptor2 = new TapestryExtensionDescriptor(
            "extension2",
            "Extension 2",
            "1.0.0",
            "0.1.0",
            List.of(capability),
            List.of(),
            List.of()
        );
        
        var mockContainer1 = mock(ModContainer.class);
        when(mockContainer1.getMetadata()).thenReturn(mock(ModMetadata.class));
        when(mockContainer1.getMetadata().getId()).thenReturn("testmod1");
        
        var mockContainer2 = mock(ModContainer.class);
        when(mockContainer2.getMetadata()).thenReturn(mock(ModMetadata.class));
        when(mockContainer2.getMetadata().getId()).thenReturn("testmod2");
        
        var provider1 = mock(TapestryExtensionProvider.class);
        when(provider1.describe()).thenReturn(descriptor1);
        
        var provider2 = mock(TapestryExtensionProvider.class);
        when(provider2.describe()).thenReturn(descriptor2);
        
        var discoveredProvider1 = new DiscoveredExtensionProvider(provider1, mockContainer1, descriptor1);
        var discoveredProvider2 = new DiscoveredExtensionProvider(provider2, mockContainer2, descriptor2);
        
        // Validate
        var result = validator.validate(List.of(discoveredProvider1, discoveredProvider2));
        
        // Both should be rejected due to conflict
        assertTrue(result.enabled().isEmpty());
        assertTrue(result.rejected().containsKey("extension1"));
        assertTrue(result.rejected().containsKey("extension2"));
        
        var rejected1 = result.rejected().get("extension1");
        var rejected2 = result.rejected().get("extension2");
        
        assertTrue(rejected1.errors().stream()
            .anyMatch(e -> e.code().equals("CAPABILITY_CONFLICT")));
        assertTrue(rejected2.errors().stream()
            .anyMatch(e -> e.code().equals("CAPABILITY_CONFLICT")));
    }
    
    @Test
    void testDependencyCycle() {
        // Create two extensions with circular dependency
        var descriptor1 = new TapestryExtensionDescriptor(
            "extension1",
            "Extension 1",
            "1.0.0",
            "0.1.0",
            List.of(new CapabilityDecl("test.service", CapabilityType.SERVICE, true, Map.of())),
            List.of("extension2"), // depends on extension2
            List.of()
        );
        
        var descriptor2 = new TapestryExtensionDescriptor(
            "extension2",
            "Extension 2",
            "1.0.0",
            "0.1.0",
            List.of(new CapabilityDecl("test2.service", CapabilityType.SERVICE, true, Map.of())),
            List.of("extension1"), // depends on extension1
            List.of()
        );
        
        var mockContainer1 = mock(ModContainer.class);
        when(mockContainer1.getMetadata()).thenReturn(mock(ModMetadata.class));
        when(mockContainer1.getMetadata().getId()).thenReturn("testmod1");
        
        var mockContainer2 = mock(ModContainer.class);
        when(mockContainer2.getMetadata()).thenReturn(mock(ModMetadata.class));
        when(mockContainer2.getMetadata().getId()).thenReturn("testmod2");
        
        var provider1 = mock(TapestryExtensionProvider.class);
        when(provider1.describe()).thenReturn(descriptor1);
        
        var provider2 = mock(TapestryExtensionProvider.class);
        when(provider2.describe()).thenReturn(descriptor2);
        
        var discoveredProvider1 = new DiscoveredExtensionProvider(provider1, mockContainer1, descriptor1);
        var discoveredProvider2 = new DiscoveredExtensionProvider(provider2, mockContainer2, descriptor2);
        
        // Validate
        var result = validator.validate(List.of(discoveredProvider1, discoveredProvider2));
        
        // Both should be rejected due to cycle
        assertTrue(result.enabled().isEmpty());
        assertTrue(result.rejected().containsKey("extension1"));
        assertTrue(result.rejected().containsKey("extension2"));
        
        var rejected1 = result.rejected().get("extension1");
        var rejected2 = result.rejected().get("extension2");
        
        assertTrue(rejected1.errors().stream()
            .anyMatch(e -> e.code().equals("DEPENDENCY_CYCLE")));
        assertTrue(rejected2.errors().stream()
            .anyMatch(e -> e.code().equals("DEPENDENCY_CYCLE")));
    }
}
