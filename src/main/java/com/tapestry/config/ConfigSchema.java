package com.tapestry.config;

import java.util.Map;

/**
 * Configuration schema definition for TypeScript mods.
 * 
 * Defines the expected structure, types, and constraints for mod configuration.
 */
public class ConfigSchema {
    
    private final int version;
    private final Map<String, FieldDefinition> fields;
    
    public ConfigSchema(int version, Map<String, FieldDefinition> fields) {
        this.version = version;
        this.fields = Map.copyOf(fields);
    }
    
    /**
     * Validates a configuration value against the schema.
     * 
     * @param config the configuration to validate
     * @throws ConfigValidationException if validation fails
     */
    public void validate(Map<String, Object> config) throws ConfigValidationException {
        for (Map.Entry<String, FieldDefinition> entry : fields.entrySet()) {
            String fieldName = entry.getKey();
            FieldDefinition fieldDef = entry.getValue();
            
            Object value = config.get(fieldName);
            
            // Apply default if missing
            if (value == null) {
                if (fieldDef.defaultValue() != null) {
                    config.put(fieldName, fieldDef.defaultValue());
                } else if (fieldDef.required()) {
                    throw new ConfigValidationException(
                        String.format("Required field '%s' is missing", fieldName)
                    );
                }
                continue;
            }
            
            // Validate type and constraints
            fieldDef.validate(fieldName, value);
        }
    }
    
    /**
     * Gets the schema version.
     */
    public int getVersion() {
        return version;
    }
    
    /**
     * Gets all field definitions.
     */
    public Map<String, FieldDefinition> getFields() {
        return fields;
    }
    
    /**
     * Definition of a configuration field.
     */
    public static class FieldDefinition {
        private final FieldType type;
        private final boolean required;
        private final Object defaultValue;
        private final Object minValue;
        private final Object maxValue;
        private final String pattern;
        
        private FieldDefinition(Builder builder) {
            this.type = builder.type;
            this.required = builder.required;
            this.defaultValue = builder.defaultValue;
            this.minValue = builder.minValue;
            this.maxValue = builder.maxValue;
            this.pattern = builder.pattern;
        }
        
        /**
         * Validates a field value.
         */
        public void validate(String fieldName, Object value) throws ConfigValidationException {
            // Type validation
            if (!type.isValid(value)) {
                throw new ConfigValidationException(
                    String.format("Field '%s' expected %s, got %s", 
                        fieldName, type.name(), value.getClass().getSimpleName())
                );
            }
            
            // Numeric constraints
            if (value instanceof Number) {
                double numValue = ((Number) value).doubleValue();
                if (minValue instanceof Number && numValue < ((Number) minValue).doubleValue()) {
                    throw new ConfigValidationException(
                        String.format("Field '%s' value %s is below minimum %s", 
                            fieldName, numValue, minValue)
                    );
                }
                if (maxValue instanceof Number && numValue > ((Number) maxValue).doubleValue()) {
                    throw new ConfigValidationException(
                        String.format("Field '%s' value %s is above maximum %s", 
                            fieldName, numValue, maxValue)
                    );
                }
            }
            
            // String pattern validation
            if (value instanceof String && pattern != null) {
                if (!((String) value).matches(pattern)) {
                    throw new ConfigValidationException(
                        String.format("Field '%s' value '%s' does not match pattern '%s'", 
                            fieldName, value, pattern)
                    );
                }
            }
        }
        
        // Getters
        public FieldType type() { return type; }
        public boolean required() { return required; }
        public Object defaultValue() { return defaultValue; }
        public Object minValue() { return minValue; }
        public Object maxValue() { return maxValue; }
        public String pattern() { return pattern; }
        
        /**
         * Builder for field definitions.
         */
        public static class Builder {
            private FieldType type;
            private boolean required = false;
            private Object defaultValue;
            private Object minValue;
            private Object maxValue;
            private String pattern;
            
            public Builder type(FieldType type) {
                this.type = type;
                return this;
            }
            
            public Builder required(boolean required) {
                this.required = required;
                return this;
            }
            
            public Builder defaultValue(Object defaultValue) {
                this.defaultValue = defaultValue;
                return this;
            }
            
            public Builder min(Object minValue) {
                this.minValue = minValue;
                return this;
            }
            
            public Builder max(Object maxValue) {
                this.maxValue = maxValue;
                return this;
            }
            
            public Builder pattern(String pattern) {
                this.pattern = pattern;
                return this;
            }
            
            public FieldDefinition build() {
                if (type == null) {
                    throw new IllegalStateException("Field type is required");
                }
                return new FieldDefinition(this);
            }
        }
    }
    
    /**
     * Supported field types.
     */
    public enum FieldType {
        STRING(String.class),
        NUMBER(Number.class),
        BOOLEAN(Boolean.class);
        
        private final Class<?> javaType;
        
        FieldType(Class<?> javaType) {
            this.javaType = javaType;
        }
        
        public boolean isValid(Object value) {
            if (value == null) return false;
            return javaType.isAssignableFrom(value.getClass());
        }
    }
}
