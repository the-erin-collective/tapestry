package com.tapestry.extensions;

/**
 * Policy configuration for extension validation behavior.
 */
public record ValidationPolicy(
    boolean failFast,              // abort whole startup on first error
    boolean disableInvalid,         // disable bad extensions and continue
    boolean warnOnOptionalMissing   // warn when optional deps missing
) {}
