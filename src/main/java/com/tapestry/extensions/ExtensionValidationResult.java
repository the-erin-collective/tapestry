package com.tapestry.extensions;

import java.util.List;
import java.util.Map;

/**
 * Result of extension validation containing enabled and rejected extensions.
 */
public record ExtensionValidationResult(
    Map<String, ValidatedExtension> enabled,           // id -> validated extension
    List<RejectedExtension> rejected,                  // all rejected extensions
    List<ValidationMessage> warnings                   // global warnings
) {}
