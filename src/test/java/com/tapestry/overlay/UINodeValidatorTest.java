package com.tapestry.overlay;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for UINodeValidator Phase 10.5 functionality.
 */
public class UINodeValidatorTest {
    
    private final UINodeValidator validator = new UINodeValidator();
    
    @Test
    void testValidTextNode() throws TemplateValidationError {
        String json = """
            {
                "type": "text",
                "content": "Hello World",
                "x": 10,
                "y": 20,
                "fontSize": 16
            }
            """;
        
        Object result = validator.validateAndParse(json);
        assertNotNull(result);
    }
    
    @Test
    void testValidBoxNode() throws TemplateValidationError {
        String json = """
            {
                "type": "box",
                "x": 0,
                "y": 0,
                "width": 100,
                "height": 50,
                "border": true,
                "padding": 5,
                "children": []
            }
            """;
        
        Object result = validator.validateAndParse(json);
        assertNotNull(result);
    }
    
    @Test
    void testValidImageNode() throws TemplateValidationError {
        String json = """
            {
                "type": "image",
                "src": "test.png",
                "x": 10,
                "y": 10,
                "width": 64,
                "height": 64
            }
            """;
        
        Object result = validator.validateAndParse(json);
        assertNotNull(result);
    }
    
    @Test
    void testMissingRequiredType() {
        String json = """
            {
                "content": "Hello World"
            }
            """;
        
        assertThrows(TemplateValidationError.class, () -> {
            validator.validateAndParse(json);
        });
    }
    
    @Test
    void testUnknownNodeType() {
        String json = """
            {
                "type": "unknown",
                "content": "Hello"
            }
            """;
        
        assertThrows(TemplateValidationError.class, () -> {
            validator.validateAndParse(json);
        });
    }
    
    @Test
    void testMissingRequiredContentForText() {
        String json = """
            {
                "type": "text",
                "x": 10,
                "y": 10
            }
            """;
        
        assertThrows(TemplateValidationError.class, () -> {
            validator.validateAndParse(json);
        });
    }
    
    @Test
    void testMissingRequiredSrcForImage() {
        String json = """
            {
                "type": "image",
                "x": 10,
                "y": 10
            }
            """;
        
        assertThrows(TemplateValidationError.class, () -> {
            validator.validateAndParse(json);
        });
    }
    
    @Test
    void testInvalidPropertyType() {
        String json = """
            {
                "type": "text",
                "content": "Hello",
                "x": "invalid"
            }
            """;
        
        assertThrows(TemplateValidationError.class, () -> {
            validator.validateAndParse(json);
        });
    }
    
    @Test
    void testUnknownProperty() {
        String json = """
            {
                "type": "text",
                "content": "Hello",
                "unknownProperty": "value"
            }
            """;
        
        assertThrows(TemplateValidationError.class, () -> {
            validator.validateAndParse(json);
        });
    }
    
    @Test
    void testNegativeFontSize() {
        String json = """
            {
                "type": "text",
                "content": "Hello",
                "fontSize": -5
            }
            """;
        
        assertThrows(TemplateValidationError.class, () -> {
            validator.validateAndParse(json);
        });
    }
    
    @Test
    void testNegativePadding() {
        String json = """
            {
                "type": "box",
                "padding": -1
            }
            """;
        
        assertThrows(TemplateValidationError.class, () -> {
            validator.validateAndParse(json);
        });
    }
    
    @Test
    void testEmptyTemplate() {
        assertThrows(TemplateValidationError.class, () -> {
            validator.validateAndParse("");
        });
        
        assertThrows(TemplateValidationError.class, () -> {
            validator.validateAndParse(null);
        });
    }
    
    @Test
    void testArrayValidation() throws TemplateValidationError {
        String json = """
            [
                {
                    "type": "text",
                    "content": "Hello"
                },
                {
                    "type": "box",
                    "width": 100,
                    "height": 50
                }
            ]
            """;
        
        Object result = validator.validateAndParse(json);
        assertNotNull(result);
    }
}
