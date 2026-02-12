package com.tapestry.extensions;

/**
 * A validation message with severity and details.
 */
public record ValidationMessage(
    Severity severity,   // ERROR/WARN/INFO
    String code,         // e.g. "DUPLICATE_ID"
    String message,      // human-readable
    String extensionId   // optional
) {}
