package com.tapestry.extensions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Version utility class.
 */
public class VersionTest {
    
    @Test
    void testParseValidVersion() {
        var version = Version.parse("1.2.3");
        assertEquals("1.2.3", version.toString());
    }
    
    @Test
    void testParseInvalidFormat() {
        assertThrows(IllegalArgumentException.class, () -> Version.parse("1.2"));
        assertThrows(IllegalArgumentException.class, () -> Version.parse("1.2.3.4"));
        assertThrows(IllegalArgumentException.class, () -> Version.parse("not.a.version"));
        assertThrows(IllegalArgumentException.class, () -> Version.parse("1.2.beta"));
    }
    
    @Test
    void testCompareTo() {
        var v1 = Version.parse("1.2.3");
        var v2 = Version.parse("1.2.4");
        var v3 = Version.parse("1.3.0");
        var v4 = Version.parse("2.0.0");
        var v5 = Version.parse("1.2.3");
        
        assertTrue(v1.compareTo(v2) < 0); // 1.2.3 < 1.2.4
        assertTrue(v1.compareTo(v3) < 0); // 1.2.3 < 1.3.0
        assertTrue(v1.compareTo(v4) < 0); // 1.2.3 < 2.0.0
        assertEquals(0, v1.compareTo(v5)); // 1.2.3 == 1.2.3
        assertTrue(v2.compareTo(v1) > 0); // 1.2.4 > 1.2.3
    }
    
    @Test
    void testIsAtLeast() {
        var base = Version.parse("1.2.3");
        
        assertTrue(base.isAtLeast(Version.parse("1.2.3"))); // equal
        assertTrue(base.isAtLeast(Version.parse("1.2.2"))); // higher
        assertTrue(base.isAtLeast(Version.parse("1.1.9"))); // higher
        assertTrue(base.isAtLeast(Version.parse("0.9.9"))); // higher
        
        assertFalse(base.isAtLeast(Version.parse("1.2.4"))); // lower
        assertFalse(base.isAtLeast(Version.parse("1.3.0"))); // lower
        assertFalse(base.isAtLeast(Version.parse("2.0.0"))); // lower
    }
    
    @Test
    void testEqualsAndHashCode() {
        var v1 = Version.parse("1.2.3");
        var v2 = Version.parse("1.2.3");
        var v3 = Version.parse("1.2.4");
        
        assertEquals(v1, v2);
        assertNotEquals(v1, v3);
        
        assertEquals(v1.hashCode(), v2.hashCode());
        // Note: hashCode might be equal even for different versions due to hash collisions
        // but equal versions must have equal hash codes
    }
}
