package com.tapestry.mod;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ModActivationException Phase 10.5 functionality.
 */
public class ModActivationExceptionTest {
    
    @Test
    void testModActivationExceptionWithMessage() {
        String modId = "test-mod";
        String message = "Activation failed";
        
        ModActivationException exception = new ModActivationException(modId, message);
        
        assertEquals(modId, exception.getModId());
        assertTrue(exception.getMessage().contains(modId));
        assertTrue(exception.getMessage().contains(message));
        assertNull(exception.getOriginalCause()); // No cause provided
    }
    
    @Test
    void testModActivationExceptionWithCause() {
        String modId = "test-mod";
        RuntimeException cause = new RuntimeException("Root cause");
        
        ModActivationException exception = new ModActivationException(modId, cause);
        
        assertEquals(modId, exception.getModId());
        assertTrue(exception.getMessage().contains(modId));
        assertTrue(exception.getMessage().contains("Root cause"));
        assertEquals(cause, exception.getOriginalCause());
    }
    
    @Test
    void testModActivationExceptionWithMessageAndCause() {
        String modId = "test-mod";
        String message = "Custom message";
        RuntimeException cause = new RuntimeException("Root cause");
        
        ModActivationException exception = new ModActivationException(modId, message, cause);
        
        assertEquals(modId, exception.getModId());
        assertTrue(exception.getMessage().contains(modId));
        assertTrue(exception.getMessage().contains(message));
        assertEquals(cause, exception.getOriginalCause());
    }
}
