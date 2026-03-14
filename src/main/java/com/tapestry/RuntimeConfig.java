package com.tapestry;

import com.tapestry.events.EventBus;

/**
 * Simple global runtime configuration container.
 * Primarily used by TypeScript-facing APIs for system-wide tuning.
 */
public class RuntimeConfig {
    private static volatile int maxEventQueueSize = 10_000;

    public static int getMaxEventQueueSize() {
        return maxEventQueueSize;
    }

    public static void setMaxEventQueueSize(int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("maxEventQueueSize must be positive");
        }
        maxEventQueueSize = size;
        // propagate to existing event bus if available
        EventBus bus = TapestryMod.getEventBus();
        if (bus != null) {
            bus.setMaxQueueSize(size);
        }
    }
}