package com.tapestry.extensions;

import java.util.List;
import java.util.Map;

/**
 * Result of capability validation.
 */
public record CapabilityValidationResult(
    Map<String, String> capabilityProviders,    // capability name -> providing mod ID
    Map<String, List<String>> capabilityGraph,     // mod ID -> list of dependency mod IDs
    List<ValidationMessage> errors,              // validation errors
    List<ValidationMessage> warnings               // validation warnings
) {
    public boolean isSuccess() {
        return errors.isEmpty();
    }
}
