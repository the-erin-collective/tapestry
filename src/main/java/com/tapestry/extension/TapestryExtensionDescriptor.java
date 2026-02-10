package com.tapestry.extension;

import java.util.List;
import java.util.Objects;

/**
 * Descriptor for a Tapestry extension.
 * 
 * This record contains metadata about an extension, including its unique ID
 * and declared capabilities. The descriptor is returned by the describe() method
 * of TapestryExtensionProvider and is validated during the DISCOVERY phase.
 * 
 * @param id the unique identifier for this extension
 * @param capabilities the list of capabilities this extension provides
 */
public record TapestryExtensionDescriptor(
    String id,
    List<String> capabilities
) {
    
    /**
     * Validates that this descriptor follows the required format.
     * 
     * @throws IllegalArgumentException if the descriptor is invalid
     */
    public void validate() {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Extension ID cannot be null or empty");
        }
        
        if (!id.matches("[A-Za-z][A-Za-z0-9_]*")) {
            throw new IllegalArgumentException(
                String.format("Extension ID '%s' must match pattern [A-Za-z][A-Za-z0-9_]*", id)
            );
        }
        
        if (capabilities == null) {
            throw new IllegalArgumentException("Capabilities list cannot be null");
        }
        
        // Validate each capability
        for (String capability : capabilities) {
            if (capability == null || capability.trim().isEmpty()) {
                throw new IllegalArgumentException("Capability cannot be null or empty");
            }
            
            if (!capability.matches("^[a-z][a-z0-9_]*\\.[a-z][a-z0-9_]*$")) {
                throw new IllegalArgumentException(
                    String.format("Capability '%s' must match pattern ^[a-z][a-z0-9_]*\\.[a-z][a-z0-9_]*$", capability)
                );
            }
        }
        
        // Check for duplicate capabilities within this extension
        long distinctCount = capabilities.stream().distinct().count();
        if (distinctCount != capabilities.size()) {
            throw new IllegalArgumentException("Extension contains duplicate capabilities");
        }
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        TapestryExtensionDescriptor that = (TapestryExtensionDescriptor) obj;
        return Objects.equals(id, that.id) && Objects.equals(capabilities, that.capabilities);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id, capabilities);
    }
    
    @Override
    public String toString() {
        return String.format("TapestryExtensionDescriptor{id='%s', capabilities=%s}", id, capabilities);
    }
}
