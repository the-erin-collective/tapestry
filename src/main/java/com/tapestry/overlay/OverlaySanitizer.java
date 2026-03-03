package com.tapestry.overlay;

import org.graalvm.polyglot.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Sanitizes and validates UI nodes from JavaScript overlay render functions.
 */
public class OverlaySanitizer {

    private static final Logger LOGGER = LoggerFactory.getLogger(OverlaySanitizer.class);

    public OverlaySanitizer() {}

    /**
     * Sanitizes a UI node from JavaScript.
     *
     * @param value the JavaScript value to sanitize
     * @return a sanitized UINode object
     * @throws IllegalArgumentException if the value is invalid
     */
    public Object sanitizeUINode(Value value) {
        if (value == null || value.isNull()) {
            return null;
        }

        if (!value.hasMembers()) {
            throw new IllegalArgumentException("UI node must be an object");
        }

        // Reject explicit null prototype objects.
        if (hasNullPrototype(value)) {
            throw new IllegalArgumentException("UI node has null prototype (security risk)");
        }

        return convertValueToUINode(value);
    }

    /**
     * Checks if a JavaScript value has a null prototype.
     *
     * @param value the value to check
     * @return true if the value has a null prototype
     */
    private boolean hasNullPrototype(Value value) {
        try {
            if (!value.hasMember("__proto__")) {
                return false;
            }
            Value prototype = value.getMember("__proto__");
            return prototype == null || prototype.isNull();
        } catch (Exception e) {
            LOGGER.debug("Unable to inspect UI node prototype, treating as safe", e);
            return false;
        }
    }

    /**
     * Converts a polyglot Value to a UINode.
     */
    private UINode convertValueToUINode(Value value) {
        // Validate required type field
        if (!value.hasMember("type")) {
            throw new IllegalArgumentException("UI node must have a 'type' field");
        }

        Value typeValue = value.getMember("type");
        if (typeValue == null || typeValue.isNull() || !typeValue.isString()) {
            throw new IllegalArgumentException("UI node 'type' must be a string");
        }

        String type = typeValue.asString();

        // Create UINode based on type
        switch (type) {
            case "box":
                return createBoxNode(value);
            case "text":
                return createTextNode(value);
            default:
                throw new IllegalArgumentException("Unknown UI node type: " + type);
        }
    }

    /**
     * Creates a box UI node from Value.
     */
    private UINode createBoxNode(Value value) {
        UINode node = new UINode();
        node.type = "box";

        // Extract optional properties
        node.x = extractCoordinate(getMember(value, "x"));
        node.y = extractCoordinate(getMember(value, "y"));
        node.width = extractCoordinate(getMember(value, "width"));
        node.height = extractCoordinate(getMember(value, "height"));
        node.color = extractColor(getMember(value, "color"));

        // Extract children
        Value children = getMember(value, "children");
        if (children != null && !children.isNull() && children.hasArrayElements()) {
            node.children = new ArrayList<>();
            for (long i = 0; i < children.getArraySize(); i++) {
                try {
                    UINode child = convertValueToUINode(children.getArrayElement(i));
                    node.children.add(child);
                } catch (Exception e) {
                    LOGGER.warn("Skipping invalid child node: {}", e.getMessage());
                }
            }
        }
        
        return node;
    }

    /**
     * Creates a text UI node from Value.
     */
    private UINode createTextNode(Value value) {
        UINode node = new UINode();
        node.type = "text";

        // Extract required content
        Value contentValue = getMember(value, "content");
        if (contentValue == null || contentValue.isNull()) {
            throw new IllegalArgumentException("Text node must have 'content' field");
        }

        if (!contentValue.isString()) {
            throw new IllegalArgumentException("Text node 'content' must be a string");
        }
        node.content = contentValue.asString();

        // Extract optional properties
        node.x = extractCoordinate(getMember(value, "x"));
        node.y = extractCoordinate(getMember(value, "y"));
        node.color = extractColor(getMember(value, "color"));

        return node;
    }

    /**
     * Safely gets a member from a Value.
     */
    private Value getMember(Value parent, String key) {
        if (parent == null || parent.isNull() || !parent.hasMember(key)) {
            return null;
        }
        return parent.getMember(key);
    }

    /**
     * Extracts a coordinate value (integer or percentage string).
     */
    private Object extractCoordinate(Value value) {
        if (value == null || value.isNull()) {
            return null;
        }

        if (value.isNumber()) {
            if (value.fitsInInt()) {
                return value.asInt();
            }
            return value.asDouble();
        }

        if (value.isString()) {
            return value.asString();
        }

        return null;
    }

    /**
     * Extracts a color value (hex string).
     */
    private String extractColor(Value value) {
        if (value == null || value.isNull()) {
            return null;
        }

        if (!value.isString()) {
            return null;
        }

        String color = value.asString();
        if (color.startsWith("#")) {
            return color;
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
