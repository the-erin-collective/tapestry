package com.tapestry.gameplay.patch;

import net.minecraft.util.Identifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PatchSetComparator.
 */
class PatchSetComparatorTest {
    
    private ModLoadOrder modLoadOrder;
    private PatchSetComparator comparator;
    private PatchTarget<Object> target;
    
    @BeforeEach
    void setUp() {
        // Create a simple mod load order that compares by namespace alphabetically
        modLoadOrder = (modA, modB) -> modA.toString().compareTo(modB.toString());
        comparator = new PatchSetComparator(modLoadOrder);
        target = new PatchTarget<>(Identifier.of("test:target"), Object.class);
    }
    
    @Test
    void testConstructorThrowsOnNullModLoadOrder() {
        assertThrows(NullPointerException.class, () -> new PatchSetComparator(null));
    }
    
    // simple generic no-op operation used by multiple tests
    private static class MockOperation<T> implements PatchOperation<T> {
        @Override public void apply(T target) { }
    }

    @Test
    void testComparePriority() {
        PatchSet<Object> lowPriority = createPatchSet("mod:a", -100);
        PatchSet<Object> highPriority = createPatchSet("mod:b", 100);
        
        // Lower priority should come first
        assertTrue(comparator.compare(lowPriority, highPriority) < 0);
        assertTrue(comparator.compare(highPriority, lowPriority) > 0);
    }
    
    @Test
    void testCompareModLoadOrder() {
        PatchSet<Object> modA = createPatchSet("aaa:mod", 0);
        PatchSet<Object> modB = createPatchSet("zzz:mod", 0);
        
        // Same priority, so mod load order determines order
        // "aaa:mod" comes before "zzz:mod" alphabetically
        assertTrue(comparator.compare(modA, modB) < 0);
        assertTrue(comparator.compare(modB, modA) > 0);
    }
    
    @Test
    void testCompareRegistrationOrder() {
        PatchSet<Object> first = createPatchSet("mod:a", 0);
        PatchSet<Object> second = createPatchSet("mod:a", 0);
        
        // Same priority and mod ID, so registration order determines order
        // First comparison assigns registration order
        int firstComparison = comparator.compare(first, second);
        // first should be assigned order 0, second should be assigned order 1
        // So first < second, meaning firstComparison should be negative
        assertTrue(firstComparison < 0, "First patch set should come before second: " + firstComparison);
        
        // Subsequent comparisons should be consistent
        int secondComparison = comparator.compare(first, second);
        assertEquals(firstComparison, secondComparison, "Comparison should be stable");
    }
    
    @Test
    void testThreeLevelOrdering() {
        // Create patch sets with different priorities, mods, and registration order
        PatchSet<Object> p1 = createPatchSet("mod:a", -100);  // Lowest priority
        PatchSet<Object> p2 = createPatchSet("aaa:mod", 0);   // Normal priority, early mod
        PatchSet<Object> p3 = createPatchSet("zzz:mod", 0);   // Normal priority, late mod
        PatchSet<Object> p4 = createPatchSet("mod:a", 100);   // Highest priority
        
        List<PatchSet<Object>> patches = new ArrayList<>(List.of(p4, p3, p2, p1));
        patches.sort(comparator);
        
        // Expected order: p1 (priority -100), p2 (priority 0, mod aaa), 
        //                 p3 (priority 0, mod zzz), p4 (priority 100)
        assertEquals(p1, patches.get(0));
        assertEquals(p2, patches.get(1));
        assertEquals(p3, patches.get(2));
        assertEquals(p4, patches.get(3));
    }
    
    @Test
    void testSamePatchSetComparesEqual() {
        PatchSet<Object> patch = createPatchSet("mod:a", 0);
        
        assertEquals(0, comparator.compare(patch, patch));
    }
    
    @Test
    void testRegistrationOrderIsStable() {
        PatchSet<Object> a = createPatchSet("mod:a", 0);
        PatchSet<Object> b = createPatchSet("mod:a", 0);
        PatchSet<Object> c = createPatchSet("mod:a", 0);
        
        // Establish order: a < b < c
        assertTrue(comparator.compare(a, b) < 0);
        assertTrue(comparator.compare(b, c) < 0);
        assertTrue(comparator.compare(a, c) < 0);
        
        // Order should remain stable
        assertTrue(comparator.compare(a, b) < 0);
        assertTrue(comparator.compare(b, c) < 0);
        assertTrue(comparator.compare(a, c) < 0);
    }
    
    private PatchSet<Object> createPatchSet(String modId, int priority) {
        // include at least one operation to satisfy PatchSet validation
        return new PatchSet<>(
            Identifier.of(modId),
            target,
            priority,
            List.of(new MockOperation<>()),
            Optional.empty()
        );
    }
}
