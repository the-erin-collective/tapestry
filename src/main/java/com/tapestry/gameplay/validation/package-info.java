/**
 * Validation and error handling utilities for Tapestry Gameplay API.
 * 
 * <p>This package provides comprehensive error handling and validation utilities
 * for the gameplay API. All error messages are descriptive, consistent, and logged
 * at appropriate severity levels.
 * 
 * <h2>Error Message Formatting</h2>
 * <p>The {@link com.tapestry.gameplay.validation.ErrorMessages} class provides
 * static methods for formatting error messages:
 * <ul>
 *   <li>{@code formatRegistrationError()} - Registration failures with mod, API, and property details</li>
 *   <li>{@code formatPhaseViolationError()} - Phase violations with expected vs actual phase</li>
 *   <li>{@code formatTraitReferenceError()} - Invalid trait references with valid trait list</li>
 *   <li>{@code formatItemValidationError()} - Item property validation failures</li>
 *   <li>{@code formatBrewingValidationError()} - Brewing recipe validation failures</li>
 *   <li>{@code formatLootValidationError()} - Loot modification failures</li>
 *   <li>{@code formatDuplicateError()} - Duplicate registration attempts</li>
 *   <li>{@code formatFrozenRegistryError()} - Operations on frozen registries</li>
 *   <li>{@code formatFabricError()} - Fabric API errors with context</li>
 * </ul>
 * 
 * <h2>Fail-Fast Validation</h2>
 * <p>All registration APIs perform fail-fast validation:
 * <ul>
 *   <li>Validation occurs during TS_REGISTER phase before storing</li>
 *   <li>Fatal errors thrown immediately on validation failure</li>
 *   <li>No deferred validation to runtime</li>
 *   <li>Descriptive error messages indicate which property failed and why</li>
 * </ul>
 * 
 * <h2>Logging Integration</h2>
 * <p>All errors are logged to the Minecraft log with appropriate severity levels:
 * <ul>
 *   <li><b>ERROR</b>: Fatal errors that prevent mod loading</li>
 *   <li><b>WARN</b>: Deprecation warnings and non-fatal issues</li>
 *   <li><b>INFO</b>: Successful registrations and phase transitions</li>
 *   <li><b>DEBUG</b>: Detailed diagnostic information</li>
 * </ul>
 * 
 * <h2>Error Message Examples</h2>
 * 
 * <h3>Registration Error</h3>
 * <pre>{@code
 * Registration failed in mod 'mymod' using ItemRegistration API: 
 * Property 'stackSize' is invalid. Stack size must be between 1 and 64, got 128.
 * }</pre>
 * 
 * <h3>Phase Violation Error</h3>
 * <pre>{@code
 * Phase violation: Cannot perform 'trait registration' during RUNTIME phase. 
 * This operation requires TS_REGISTER phase. 
 * Current phase: RUNTIME, Expected phase: TS_REGISTER
 * }</pre>
 * 
 * <h3>Trait Reference Error</h3>
 * <pre>{@code
 * Invalid trait reference: Trait 'unknown_trait' is not registered. 
 * Valid traits are: 'fish_food', 'milk_like', 'egg_like', 'honey_like', 'plant_fiber'. 
 * Traits must be registered using TraitSystem.register() before being referenced.
 * }</pre>
 * 
 * <h3>Item Validation Error</h3>
 * <pre>{@code
 * Item validation failed for 'mymod:custom_item': 
 * Property 'durability' has invalid value '-10'. 
 * Durability must be greater than or equal to 0.
 * }</pre>
 * 
 * <h2>Usage</h2>
 * <p>Error formatting utilities are used throughout the gameplay API:
 * 
 * <pre>{@code
 * // In ItemRegistration.java
 * if (stackSize < 1 || stackSize > 64) {
 *     throw new IllegalArgumentException(
 *         ErrorMessages.formatItemValidationError(
 *             itemId, "stackSize", stackSize,
 *             "Stack size must be between 1 and 64"
 *         )
 *     );
 * }
 * 
 * // In TraitSystem.java
 * if (!traits.containsKey(traitName)) {
 *     throw new IllegalArgumentException(
 *         ErrorMessages.formatTraitReferenceError(
 *             traitName, traits.keySet()
 *         )
 *     );
 * }
 * }</pre>
 * 
 * @see com.tapestry.gameplay.validation.ErrorMessages
 * @see com.tapestry.gameplay.lifecycle.PhaseEnforcement
 */
package com.tapestry.gameplay.validation;
