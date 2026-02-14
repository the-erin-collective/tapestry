package com.tapestry.overlay;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;

import java.util.List;
import java.util.Map;

/**
 * Base class for UI nodes in Phase 10.5 overlay system.
 * 
 * Uses Jackson polymorphic deserialization to handle different node types
 * with strict validation according to the Phase 10.5 specification.
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes({
    @Type(value = UINode.BoxNode.class, name = "box"),
    @Type(value = UINode.TextNode.class, name = "text"),
    @Type(value = UINode.ImageNode.class, name = "image")
})
public abstract class UINode {
    
    // Base properties for all nodes
    protected String type;
    protected Double x;
    protected Double y;
    protected Double width;
    protected Double height;
    
    protected UINode() {} // Jackson deserialization
    
    protected UINode(String type) {
        this.type = type;
    }
    
    // Getters
    public String getType() { return type; }
    public Double getX() { return x; }
    public Double getY() { return y; }
    public Double getWidth() { return width; }
    public Double getHeight() { return height; }
    
    // Setters (for validation)
    public void setType(String type) { this.type = type; }
    public void setX(Double x) { this.x = x; }
    public void setY(Double y) { this.y = y; }
    public void setWidth(Double width) { this.width = width; }
    public void setHeight(Double height) { this.height = height; }
    
    /**
 * Box node type for container elements.
     */
    public static class BoxNode extends UINode {
        private Boolean border;
        private Double padding;
        private List<UINode> children;
        
        public BoxNode() {
            super("box");
        }
        
        // Getters
        public Boolean getBorder() { return border; }
        public Double getPadding() { return padding; }
        public List<UINode> getChildren() { return children; }
        
        // Setters
        public void setBorder(Boolean border) { this.border = border; }
        public void setPadding(Double padding) { this.padding = padding; }
        public void setChildren(List<UINode> children) { this.children = children; }
    }
    
    /**
     * Text node type for displaying text content.
     */
    public static class TextNode extends UINode {
        private String content;
        private Double fontSize;
        
        public TextNode() {
            super("text");
        }
        
        // Getters
        public String getContent() { return content; }
        public Double getFontSize() { return fontSize; }
        
        // Setters
        public void setContent(String content) { this.content = content; }
        public void setFontSize(Double fontSize) { this.fontSize = fontSize; }
    }
    
    /**
     * Image node type for displaying images.
     */
    public static class ImageNode extends UINode {
        private String src;
        
        public ImageNode() {
            super("image");
        }
        
        // Getters
        public String getSrc() { return src; }
        
        // Setters
        public void setSrc(String src) { this.src = src; }
    }
}
