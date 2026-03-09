package com.tapestry.gameplay.model;

/**
 * Optional interface implemented by wrapper objects that encapsulate vanilla
 * gameplay data. Wrappers provide a more convenient API for modifying the
 * underlying data and can be used with {@link com.tapestry.gameplay.patch.PatchEngine#applyPatchesWithWrapper}.
 *
 * @param <T> the type of the vanilla gameplay data object
 */
public interface TapestryModel<T> {
    /**
     * Returns the underlying vanilla data object that this wrapper manages.
     *
     * @return the wrapped vanilla data
     */
    T unwrap();
}
