package com.tapestry.gameplay.items;

/**
 * Functional interface for custom item use behavior.
 * 
 * Invoked when an item is used by a player.
 */
@FunctionalInterface
public interface UseHandler {
    /**
     * Handles item use.
     * 
     * @param context the use context containing player, world, stack, etc.
     * @return the use result (item replacement, success status)
     */
    UseResult use(UseContext context);
}
