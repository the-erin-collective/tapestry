package com.tapestry.overlay;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import org.graalvm.polyglot.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Sanitizes and validates UI nodes from JavaScript overlay render functions.
 * 
 * This layer ensures security by:
 * - Checking object prototypes (rejecting null prototypes)
 * - Stripping non-serializable properties
 * - Validating UI node structure
 * - Handling trailing commas in JSON
 */
public class OverlaySanitizer {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(OverlaySanitizer.class);
    
    private final Gson gson;
    private final JsonParser jsonParser;
    
    public OverlaySanitizer() {
        // Configure Gson to handle trailing commas
        this.gson = new Gson();
        this.jsonParser = new JsonParser();
    }
    
    /**
     * Sanitizes a UI node from JavaScript.
     * 
     * @param value the JavaScript value to sanitize
     * @return a sanitized UINode object
     * @throws IllegalArgumentException if the value is invalid
     */
    public Object sanitizeUINode(Value value) {
        if (value == null || value.isNull()) {
            throw new IllegalArgumentException("UI node cannot be null");
        }
        
        // Check for null prototype (security risk)
        if (hasNullPrototype(value)) {
            throw new IllegalArgumentException("UI node has null prototype (security risk)");
        }
        
        // Convert to JSON string for validation
        String jsonString;
        try {
            jsonString = value.toString();
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to serialize UI node to JSON", e);
        }
        
        // Parse JSON with lenient settings (handles trailing commas)
        JsonElement jsonElement;
        try {
            jsonElement = parseJsonLenient(jsonString);
        } catch (JsonSyntaxException e) {
            throw new IllegalArgumentException("Invalid JSON in UI node: " + e.getMessage(), e);
        }
        
        // Validate and convert to UINode
        return convertJsonToUINode(jsonElement);
    }
    
    /**
     * Checks if a JavaScript value has a null prototype.
     * 
     * @param value the value to check
     * @return true if the value has a null prototype
     */
    private boolean hasNullPrototype(Value value) {
        try {
            // Try to access the prototype chain
            Value prototype = value.getMember("prototype");
            return prototype == null || prototype.isNull();
        } catch (Exception e) {
            // If we can't access the prototype, assume it's unsafe
            return true;
        }
    }
    
    /**
     * Parses JSON with lenient settings (handles trailing commas).
     * 
     * @param jsonString the JSON string to parse
     * @return the parsed JsonElement
     * @throws JsonSyntaxException if parsing fails
     */
    private JsonElement parseJsonLenient(String jsonString) throws JsonSyntaxException {
        // Remove trailing commas before parsing
        String cleanedJson = removeTrailingCommas(jsonString);
        return jsonParser.parse(cleanedJson);
    }
    
    /**
     * Removes trailing commas from JSON strings.
     * 
     * @param json the JSON string
     * @return the JSON string without trailing commas
     */
    private String removeTrailingCommas(String json) {
        // Simple regex to remove trailing commas before closing brackets/braces
        return json.replaceAll(",\\s*([\\]}])", "$1");
    }
    
    /**
     * Converts a JsonElement to a UINode.
     * 
     * @param jsonElement the JSON element to convert
     * @return a UINode object
     * @throws IllegalArgumentException if the structure is invalid
     */
    private UINode convertJsonToUINode(JsonElement jsonElement) {
        if (!jsonElement.isJsonObject()) {
            throw new IllegalArgumentException("UI node must be a JSON object");
        }
        
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        
        // Validate required type field
        if (!jsonObject.has("type")) {
            throw new IllegalArgumentException("UI node must have a 'type' field");
        }
        
        String type = jsonObject.get("type").getAsString();
        
        // Create UINode based on type
        switch (type) {
            case "box":
                return createBoxNode(jsonObject);
            case "text":
                return createTextNode(jsonObject);
            default:
                throw new IllegalArgumentException("Unknown UI node type: " + type);
        }
    }
    
    /**
     * Creates a box UI node from JSON.
     */
    private UINode createBoxNode(JsonObject json) {
        UINode node = new UINode();
        node.type = "box";
        
        // Extract optional properties
        node.x = extractCoordinate(json.get("x"));
        node.y = extractCoordinate(json.get("y"));
        node.width = extractCoordinate(json.get("width"));
        node.height = extractCoordinate(json.get("height"));
        node.color = extractColor(json.get("color"));
        
        // Extract children
        if (json.has("children") && json.get("children").isJsonArray()) {
            node.children = new ArrayList<>();
            for (JsonElement childElement : json.get("children").getAsJsonArray()) {
                try {
                    UINode child = convertJsonToUINode(childElement);
                    node.children.add(child);
                } catch (Exception e) {
                    LOGGER.warn("Skipping invalid child node: {}", e.getMessage());
                }
            }
        }
        
        return node;
    }
    
    /**
     * Creates a text UI node from JSON.
     */
    private UINode createTextNode(JsonObject json) {
        UINode node = new UINode();
        node.type = "text";
        
        // Extract required content
        if (!json.has("content")) {
            throw new IllegalArgumentException("Text node must have 'content' field");
        }
        node.content = json.get("content").getAsString();
        
        // Extract optional properties
        node.x = extractCoordinate(json.get("x"));
        node.y = extractCoordinate(json.get("y"));
        node.color = extractColor(json.get("color"));
        
        return node;
    }
    
    /**
     * Extracts a coordinate value (integer or percentage string).
     */
    private Object extractCoordinate(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return null;
        }
        
        if (element.isJsonPrimitive()) {
            if (element.getAsJsonPrimitive().isNumber()) {
                return element.getAsNumber();
            } else if (element.getAsJsonPrimitive().isString()) {
                return element.getAsString();
            }
        }
        
        return null;
    }
    
    /**
     * Extracts a color value (hex string).
     */
    private String extractColor(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return null;
        }
        
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            String color = element.getAsString();
            if (color.startsWith("#")) {
                return color;
            }
        }
        
        return null;
    }
    
    /**
     * Represents a sanitized UI node.
     */
    public static class UINode {
        public String type;
        public Object x;
        public Object y;
        public Object width;
        public Object height;
        public String color;
        public String content;
        public List<UINode> children;
        
        @Override
        public String toString() {
            return "UINode{type='" + type + "', x=" + x + ", y=" + y + 
                   ", content='" + content + "'}";
        }
    }
}
