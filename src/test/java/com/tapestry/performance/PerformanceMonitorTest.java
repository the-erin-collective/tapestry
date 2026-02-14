package com.tapestry.performance;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for PerformanceMonitor Phase 10.5 functionality.
 */
public class PerformanceMonitorTest {
    
    private PerformanceMonitor monitor;
    
    @BeforeEach
    void setUp() {
        monitor = PerformanceMonitor.getInstance();
        monitor.reset();
    }
    
    @Test
    void testModCountLimit() {
        // Should not throw when under limit
        assertDoesNotThrow(() -> {
            monitor.checkModCountLimit(199, 1); // 199 + 1 = 200 (limit)
        });
        
        // Should throw when over limit
        assertThrows(PerformanceMonitor.PerformanceLimitException.class, () -> {
            monitor.checkModCountLimit(200, 1); // 200 + 1 = 201 (over limit)
        });
    }
    
    @Test
    void testDependencyDepthLimit() {
        // Should not throw when under limit
        assertDoesNotThrow(() -> {
            monitor.checkDependencyDepthLimit(50); // At limit
        });
        
        // Should throw when over limit
        assertThrows(PerformanceMonitor.PerformanceLimitException.class, () -> {
            monitor.checkDependencyDepthLimit(51); // Over limit
        });
    }
    
    @Test
    void testTemplateSizeLimit() {
        // Should not throw when under limit (100 KB)
        assertDoesNotThrow(() -> {
            monitor.checkTemplateSizeLimit(100 * 1024); // Exactly 100 KB
        });
        
        // Should throw when over limit
        assertThrows(PerformanceMonitor.PerformanceLimitException.class, () -> {
            monitor.checkTemplateSizeLimit(100 * 1024 + 1); // Over 100 KB
        });
    }
    
    @Test
    void testTemplateNodeCountLimit() {
        // Should not throw when under limit
        assertDoesNotThrow(() -> {
            monitor.checkTemplateNodeCountLimit(1000); // At limit
        });
        
        // Should throw when over limit
        assertThrows(PerformanceMonitor.PerformanceLimitException.class, () -> {
            monitor.checkTemplateNodeCountLimit(1001); // Over limit
        });
    }
    
    @Test
    void testActivationTimer() {
        PerformanceMonitor.ActivationTimer timer = monitor.startModActivationTiming("test-mod");
        
        // Should not throw when under time limit (5 seconds)
        assertDoesNotThrow(() -> {
            timer.stop(); // Will be very fast, well under 5 seconds
        });
        
        // Verify metrics were recorded
        PerformanceMonitor.PerformanceMetrics metrics = monitor.getModMetrics("test-mod");
        assertNotNull(metrics);
        assertEquals(1, metrics.getActivationCount());
        assertTrue(metrics.getActivationTime() >= 0);
    }
    
    @Test
    void testTemplateProcessingMetrics() {
        monitor.recordTemplateProcessing(100, 50);
        monitor.recordTemplateProcessing(200, 75);
        
        PerformanceMonitor.PerformanceStats stats = monitor.getStats();
        
        assertEquals(2, stats.totalTemplateCount());
        assertEquals(150, stats.avgTemplateTimeMs()); // (100 + 200) / 2
        assertEquals(300, stats.totalTemplateTimeMs()); // 100 + 200
    }
    
    @Test
    void testPerformanceStats() {
        monitor.recordTemplateProcessing(50, 25);
        
        PerformanceMonitor.PerformanceStats stats = monitor.getStats();
        
        assertNotNull(stats);
        assertEquals(1, stats.totalTemplateCount());
        assertEquals(50, stats.avgTemplateTimeMs());
        assertEquals(50, stats.totalTemplateTimeMs());
    }
    
    @Test
    void testReset() {
        monitor.recordTemplateProcessing(100, 50);
        PerformanceMonitor.ActivationTimer timer = monitor.startModActivationTiming("test-mod");
        timer.stop();
        
        // Verify data exists
        assertEquals(1, monitor.getStats().totalTemplateCount());
        assertNotNull(monitor.getModMetrics("test-mod"));
        
        // Reset and verify data is cleared
        monitor.reset();
        assertEquals(0, monitor.getStats().totalTemplateCount());
        assertNull(monitor.getModMetrics("test-mod"));
    }
}
