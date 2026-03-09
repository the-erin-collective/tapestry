package com.tapestry.gameplay.patch;

/**
 * Exception thrown when attempting to apply a patch to an object of incompatible type.
 * This indicates a programming error where the target type doesn't match the actual object type.
 */
public class PatchTypeMismatchException extends PatchApplicationException {
    /**
     * Creates a new PatchTypeMismatchException for a type mismatch.
     * 
     * @param target The patch target with the expected type
     * @param actual The actual object that was incompatible
     */
    public PatchTypeMismatchException(PatchTarget<?> target, Object actual) {
        super(String.format(
            "Type mismatch: expected %s but got %s for target %s",
            target.type().getName(),
            actual.getClass().getName(),
            target.id()
        ));
    }
}
