package com.tapestry.gameplay.validation;

import com.tapestry.gameplay.lifecycle.LifecyclePhase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

/**
 * Utility class for formatting error messages in the Tapestry Gameplay API.
 * 
 * Provides consistent, descriptive error messages for registration failures,
 * phase violations, validation errors, and trait reference errors.
 */
public class ErrorMessages {
    private static final Logger LOGGER = LoggerFactory.getLogger(ErrorMessages.class);
    
    /**
     * Formats a registration error message with mod name, API name, and property details.
     * 
     * @param modName the name of the mod attempting registration
     * @param apiName the API being used (e.g., "ItemRegistration", "TraitSystem")
     * @param propertyName the property that failed validation
     * @param reason the reason for the failure
     * @return formatted error message
     */
    public static String formatRegistrationError(String modName, String apiName, String propertyName, String reason) {
        String message = String.format(
            "Registration failed in mod '%s' using %s API: Property '%s' is invalid. %s",
            modName, apiName, propertyName, reason
        );
        LOGGER.error(message);
        return message;
    }
    
    /**
     * Formats a registration error message without mod name.
     * 
     * @param apiName the API being used
     * @param propertyName the property that failed validation
     * @param reason the reason for the failure
     * @return formatted error message
     */
    public static String formatRegistrationError(String apiName, String propertyName, String reason) {
        String message = String.format(
            "Registration failed using %s API: Property '%s' is invalid. %s",
            apiName, propertyName, reason
        );
        LOGGER.error(message);
        return message;
    }
    
    /**
     * Formats a phase violation error message with expected and actual phases.
     * 
     * @param expectedPhase the expected phase
     * @param actualPhase the actual phase
     * @param operation the operation that was attempted
     * @return formatted error message
     */
    public static String formatPhaseViolationError(LifecyclePhase expectedPhase, LifecyclePhase actualPhase, String operation) {
        String message = String.format(
            "Phase violation: Cannot perform '%s' during %s phase. " +
            "This operation requires %s phase. " +
            "Current phase: %s, Expected phase: %s",
            operation, actualPhase, expectedPhase, actualPhase, expectedPhase
        );
        LOGGER.error(message);
        return message;
    }
    
    /**
     * Formats a trait reference error message with invalid trait and valid trait list.
     * 
     * @param invalidTrait the invalid trait name that was referenced
     * @param validTraits collection of valid trait names
     * @return formatted error message
     */
    public static String formatTraitReferenceError(String invalidTrait, Collection<String> validTraits) {
        StringBuilder validTraitsStr = new StringBuilder();
        int count = 0;
        for (String trait : validTraits) {
            if (count > 0) {
                validTraitsStr.append(", ");
            }
            validTraitsStr.append("'").append(trait).append("'");
            count++;
            
            // Limit to first 10 traits to avoid overly long messages
            if (count >= 10) {
                validTraitsStr.append(", ... (").append(validTraits.size() - 10).append(" more)");
                break;
            }
        }
        
        String message = String.format(
            "Invalid trait reference: Trait '%s' is not registered. " +
            "Valid traits are: %s. " +
            "Traits must be registered using TraitSystem.register() before being referenced.",
            invalidTrait, validTraitsStr.toString()
        );
        LOGGER.error(message);
        return message;
    }
    
    /**
     * Formats a validation error message for item properties.
     * 
     * @param itemId the item identifier
     * @param propertyName the property that failed validation
     * @param value the invalid value
     * @param constraint the constraint that was violated
     * @return formatted error message
     */
    public static String formatItemValidationError(String itemId, String propertyName, Object value, String constraint) {
        String message = String.format(
            "Item validation failed for '%s': Property '%s' has invalid value '%s'. %s",
            itemId, propertyName, value, constraint
        );
        LOGGER.error(message);
        return message;
    }
    
    /**
     * Formats a validation error message for brewing recipes.
     * 
     * @param propertyName the property that failed validation
     * @param value the invalid value
     * @param reason the reason for the failure
     * @return formatted error message
     */
    public static String formatBrewingValidationError(String propertyName, String value, String reason) {
        String message = String.format(
            "Brewing recipe validation failed: Property '%s' has invalid value '%s'. %s",
            propertyName, value, reason
        );
        LOGGER.error(message);
        return message;
    }
    
    /**
     * Formats a validation error message for loot modifications.
     * 
     * @param tableId the loot table identifier
     * @param reason the reason for the failure
     * @return formatted error message
     */
    public static String formatLootValidationError(String tableId, String reason) {
        String message = String.format(
            "Loot modification failed for table '%s': %s",
            tableId, reason
        );
        LOGGER.error(message);
        return message;
    }
    
    /**
     * Formats a duplicate registration error message.
     * 
     * @param type the type of thing being registered (e.g., "trait", "item")
     * @param identifier the identifier that is duplicated
     * @return formatted error message
     */
    public static String formatDuplicateError(String type, String identifier) {
        String message = String.format(
            "Duplicate %s registration: '%s' is already registered. " +
            "Each %s must have a unique identifier.",
            type, identifier, type
        );
        LOGGER.error(message);
        return message;
    }
    
    /**
     * Formats a frozen registry error message.
     * 
     * @param registryName the name of the registry
     * @param operation the operation that was attempted
     * @return formatted error message
     */
    public static String formatFrozenRegistryError(String registryName, String operation) {
        String message = String.format(
            "Cannot perform '%s' on %s: Registry is frozen after COMPOSITION phase. " +
            "All registrations must occur during TS_REGISTER phase.",
            operation, registryName
        );
        LOGGER.error(message);
        return message;
    }
    
    /**
     * Formats a Fabric API error message.
     * 
     * @param operation the operation that failed
     * @param fabricError the original Fabric error message
     * @return formatted error message
     */
    public static String formatFabricError(String operation, String fabricError) {
        String message = String.format(
            "Fabric API error during '%s': %s. " +
            "This may indicate a Minecraft version incompatibility or internal error.",
            operation, fabricError
        );
        LOGGER.error(message);
        return message;
    }
    
    /**
     * Logs a successful registration at INFO level.
     * 
     * @param type the type of thing registered
     * @param identifier the identifier
     */
    public static void logSuccessfulRegistration(String type, String identifier) {
        LOGGER.info("Successfully registered {}: '{}'", type, identifier);
    }
    
    /**
     * Logs a warning message at WARN level.
     * 
     * @param message the warning message
     */
    public static void logWarning(String message) {
        LOGGER.warn(message);
    }
    
    /**
     * Logs a deprecation warning at WARN level.
     * 
     * @param feature the deprecated feature
     * @param alternative the recommended alternative
     */
    public static void logDeprecation(String feature, String alternative) {
        LOGGER.warn("DEPRECATION: '{}' is deprecated. Use '{}' instead.", feature, alternative);
    }
}
