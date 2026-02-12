package com.tapestry.extensions;

/**
 * Thrown when an operation is attempted in the wrong phase.
 */
public class WrongPhaseException extends ExtensionRegistrationException {
    
    public WrongPhaseException(String expectedPhase, String actualPhase) {
        super("Operation requires phase " + expectedPhase + " but current phase is " + actualPhase);
    }
}
