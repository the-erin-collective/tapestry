package com.tapestry.gameplay.patch;

import net.minecraft.util.Identifier;
import java.util.Objects;

/**
 * Identifies a specific gameplay object to be modified.
 * 
 * @param <T> The type of gameplay data this target represents
 */
public record PatchTarget<T>(
    Identifier id,
    Class<T> type
) {
    public PatchTarget {
        Objects.requireNonNull(id, "Target identifier cannot be null");
        Objects.requireNonNull(type, "Target type cannot be null");
    }
    
    /**
     * Checks if the given object is compatible with this target's type.
     * 
     * @param object The object to check
     * @return true if the object is an instance of this target's type
     */
    public boolean isCompatibleWith(Object object) {
        return type.isInstance(object);
    }
}
