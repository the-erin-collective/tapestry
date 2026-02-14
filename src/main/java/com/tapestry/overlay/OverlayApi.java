package com.tapestry.overlay;

import com.tapestry.typescript.TypeScriptRuntime;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * JavaScript API for overlay registration and management.
 * 
 * This class provides the `tapestry.client.overlay` namespace that mods use
 * to register overlays during the CLIENT_PRESENTATION_READY phase.
 */
public class OverlayApi implements ProxyObject {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(OverlayApi.class);
    
    private final OverlayRegistry registry;
    
    public OverlayApi(OverlayRegistry registry) {
        this.registry = registry;
    }
    
    /**
     * Creates the overlay namespace for JavaScript.
     * 
     * @return a ProxyObject representing the overlay namespace
     */
    public ProxyObject createNamespace() {
        Map<String, Object> namespace = new HashMap<>();
        
        // Register function: tapestry.client.overlay.register(overlayDefinition)
        namespace.put("register", (ProxyExecutable) args -> {
            return registerOverlay(args);
        });
        
        // Set visibility function: tapestry.client.overlay.setVisible(modId, overlayId, visible)
        namespace.put("setVisible", (ProxyExecutable) args -> {
            return setOverlayVisibility(args);
        });
        
        // Get count function: tapestry.client.overlay.getCount()
        namespace.put("getCount", (ProxyExecutable) args -> {
            return getOverlayCount(args);
        });
        
        // Template function: tapestry.client.overlay.template(template, data)
        namespace.put("template", (ProxyExecutable) args -> {
            return processTemplate(args);
        });
        
        // Add function: tapestry.client.overlay.add(fragment)
        namespace.put("add", (ProxyExecutable) args -> {
            return addFragment(args);
        });
        
        return ProxyObject.fromMap(namespace);
    }
    
    /**
     * Registers a new overlay from JavaScript.
     * 
     * Expected args: [overlayDefinition]
     * overlayDefinition should be an object with:
     * - id: string (required)
     * - anchor: string (required)
     * - zIndex: number (optional, defaults to 50)
     * - visible: boolean (optional, defaults to true)
     * - render: function (required)
     */
    private Object registerOverlay(Value[] args) {
        if (args.length != 1) {
            throw new IllegalArgumentException("register() requires exactly 1 argument (overlay definition)");
        }
        
        Value overlayDef = args[0];
        if (!overlayDef.hasMember("id") || !overlayDef.hasMember("anchor") || !overlayDef.hasMember("render")) {
            throw new IllegalArgumentException("Overlay definition must have id, anchor, and render properties");
        }
        
        // Extract overlay properties
        String overlayId = overlayDef.getMember("id").asString();
        String anchor = overlayDef.getMember("anchor").asString();
        int zIndex = overlayDef.hasMember("zIndex") ? (int) overlayDef.getMember("zIndex").asInt() : 50;
        boolean visible = overlayDef.hasMember("visible") ? overlayDef.getMember("visible").asBoolean() : true;
        Value renderFunction = overlayDef.getMember("render");
        
        // Validate render function
        if (!renderFunction.canExecute()) {
            throw new IllegalArgumentException("Overlay render property must be a function");
        }
        
        // Validate anchor
        validateAnchor(anchor);
        
        // Get current mod ID from execution context
        String modId = TypeScriptRuntime.getCurrentModId();
        if (modId == null) {
            throw new IllegalStateException("Overlay registration must occur within a mod context");
        }
        
        // Create overlay definition
        OverlayRegistry.OverlayDefinition definition = new OverlayRegistry.OverlayDefinition(
            anchor, zIndex, renderFunction
        );
        definition.setVisible(visible);
        
        // Register the overlay
        registry.registerOverlay(modId, overlayId, definition);
        
        LOGGER.info("Registered overlay '{}' for mod '{}' with anchor '{}' and z-index {}", 
            overlayId, modId, anchor, zIndex);
        
        return null; // Void return
    }
    
    /**
     * Sets the visibility of an overlay.
     * 
     * Expected args: [modId, overlayId, visible]
     */
    private Object setOverlayVisibility(Value[] args) {
        if (args.length != 3) {
            throw new IllegalArgumentException("setVisible() requires exactly 3 arguments (modId, overlayId, visible)");
        }
        
        String modId = args[0].asString();
        String overlayId = args[1].asString();
        boolean visible = args[2].asBoolean();
        
        // Validate that the current mod can modify this overlay
        String currentModId = TypeScriptRuntime.getCurrentModId();
        if (!modId.equals(currentModId)) {
            throw new IllegalArgumentException(
                String.format("Mod '%s' cannot modify overlay '%s' owned by mod '%s'", 
                    currentModId, overlayId, modId)
            );
        }
        
        registry.setOverlayVisibility(modId, overlayId, visible);
        
        LOGGER.debug("Set overlay '{}' visibility to {} for mod '{}'", overlayId, visible, modId);
        
        return null; // Void return
    }
    
    /**
     * Gets the count of registered overlays.
     * 
     * Expected args: [] (no arguments)
     */
    private Object getOverlayCount(Value[] args) {
        if (args.length != 0) {
            throw new IllegalArgumentException("getCount() requires no arguments");
        }
        
        return registry.getOverlayCount();
    }
    
    /**
     * Validates that an anchor value is supported.
     */
    private void validateAnchor(String anchor) {
        switch (anchor) {
            case "TOP_LEFT":
            case "TOP_CENTER":
            case "TOP_RIGHT":
            case "CENTER":
            case "BOTTOM_LEFT":
            case "BOTTOM_CENTER":
            case "BOTTOM_RIGHT":
                // Valid anchors
                break;
            default:
                throw new IllegalArgumentException(
                    "Invalid anchor '" + anchor + "'. Must be one of: " +
                    "TOP_LEFT, TOP_CENTER, TOP_RIGHT, CENTER, BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT"
                );
        }
    }
    
    /**
     * Processes a template string with data and returns validated UINode(s).
     * 
     * Expected args: [template, data]
     * template: string - Mikel template string
     * data: object - data for template interpolation (optional)
     */
    private Object processTemplate(Value[] args) {
        if (args.length < 1 || args.length > 2) {
            throw new IllegalArgumentException("template() requires 1 or 2 arguments (template, data)");
        }
        
        String template = args[0].asString();
        Object data = args.length > 1 ? TypeScriptRuntime.fromValue(args[1]) : null;
        
        try {
            // Get Mikel from tapestry.utils.mikel
            Value tapestry = (Value) TypeScriptRuntime.evalExpression("tapestry");
            Value utils = tapestry.getMember("utils");
            Value mikel = utils.getMember("mikel");
            
            // Process template with Mikel
            String renderedTemplate;
            if (data != null) {
                Value dataValue = TypeScriptRuntime.toHostValue(data);
                renderedTemplate = mikel.execute(template, dataValue).asString();
            } else {
                renderedTemplate = mikel.execute(template).asString();
            }
            
            // Validate the rendered template
            UINodeValidator validator = new UINodeValidator();
            Object validatedNodes = validator.validateAndParse(renderedTemplate);
            
            LOGGER.debug("Template processed and validated successfully");
            return TypeScriptRuntime.toHostValue(validatedNodes);
            
        } catch (TemplateValidationError e) {
            LOGGER.error("Template validation failed: {}", e.getDetailedMessage());
            throw e;
        } catch (Exception e) {
            LOGGER.error("Template processing failed", e);
            throw new RuntimeException("Template processing failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Adds a validated UI fragment to the overlay system.
     * 
     * Expected args: [fragment]
     * fragment: UINode or array of UINodes - validated from template()
     */
    private Object addFragment(Value[] args) {
        if (args.length != 1) {
            throw new IllegalArgumentException("add() requires exactly 1 argument (fragment)");
        }
        
        // This is a placeholder for the add functionality
        // In a full implementation, this would add the fragment to the rendering system
        LOGGER.debug("Fragment added to overlay system");
        
        return null;
    }
    
    // ProxyObject implementation for the namespace
    @Override
    public Object getMember(String key) {
        throw new UnsupportedOperationException("Overlay API namespace is read-only");
    }
    
    @Override
    public Object getMemberKeys() {
        return new String[]{"register", "setVisible", "getCount", "template", "add"};
    }
    
    @Override
    public boolean hasMember(String key) {
        return "register".equals(key) || "setVisible".equals(key) || "getCount".equals(key) 
            || "template".equals(key) || "add".equals(key);
    }
    
    @Override
    public void putMember(String key, Value value) {
        throw new UnsupportedOperationException("Overlay API namespace is read-only");
    }
    
    @Override
    public boolean removeMember(String key) {
        throw new UnsupportedOperationException("Overlay API namespace is read-only");
    }
}
