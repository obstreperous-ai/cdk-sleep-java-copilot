package com.myorg.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SleepAudioProcessor Lambda function.
 * Issue #8: Input Validation and Error Handling Tests
 */
class SleepAudioProcessorTest {

    private SleepAudioProcessor processor;

    @Mock
    private Context mockContext;

    @Mock
    private LambdaLogger mockLogger;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        processor = new SleepAudioProcessor();
        when(mockContext.getLogger()).thenReturn(mockLogger);
        // Issue #10: Mock getAwsRequestId for structured logging
        when(mockContext.getAwsRequestId()).thenReturn("test-request-id-12345");
    }

    // ===== Issue #8: Input Validation Tests (TDD - These should fail initially) =====

    @Test
    void rejectsMissingBucketName() {
        // Arrange: Input with missing bucket name
        Map<String, Object> input = new HashMap<>();
        Map<String, Object> detail = new HashMap<>();
        Map<String, Object> object = new HashMap<>();
        object.put("key", "test.mp3");
        detail.put("object", object);
        // bucket is missing
        input.put("detail", detail);

        // Act & Assert: Should throw exception for missing bucket
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            processor.handleRequest(input, mockContext);
        });

        assertTrue(exception.getMessage().contains("bucket") || 
                   exception.getMessage().contains("required"),
            "Error message should mention missing bucket");
    }

    @Test
    void rejectsMissingObjectKey() {
        // Arrange: Input with missing object key
        Map<String, Object> input = new HashMap<>();
        Map<String, Object> detail = new HashMap<>();
        Map<String, Object> bucket = new HashMap<>();
        bucket.put("name", "test-bucket");
        detail.put("bucket", bucket);
        // object is missing
        input.put("detail", detail);

        // Act & Assert: Should throw exception for missing object key
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            processor.handleRequest(input, mockContext);
        });

        assertTrue(exception.getMessage().contains("key") || 
                   exception.getMessage().contains("required"),
            "Error message should mention missing object key");
    }

    @Test
    void rejectsUnsupportedFileExtension() {
        // Arrange: Input with unsupported file extension (.txt)
        Map<String, Object> input = createValidInput("test-bucket", "audio/test.txt");

        // Act & Assert: Should throw exception for unsupported file type
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            processor.handleRequest(input, mockContext);
        });

        assertTrue(exception.getMessage().contains("extension") || 
                   exception.getMessage().contains("format") ||
                   exception.getMessage().contains("supported"),
            "Error message should mention unsupported file format");
    }

    @Test
    void acceptsMp3FileExtension() {
        // Arrange: Valid MP3 input
        Map<String, Object> input = createValidInput("test-bucket", "audio/test.mp3");

        // Act
        Map<String, Object> result = processor.handleRequest(input, mockContext);

        // Assert: Should return success status
        assertNotNull(result);
        assertEquals("VALIDATED", result.get("status"));
    }

    @Test
    void acceptsWavFileExtension() {
        // Arrange: Valid WAV input
        Map<String, Object> input = createValidInput("test-bucket", "audio/test.wav");

        // Act
        Map<String, Object> result = processor.handleRequest(input, mockContext);

        // Assert: Should return success status
        assertNotNull(result);
        assertEquals("VALIDATED", result.get("status"));
    }

    @Test
    void acceptsM4aFileExtension() {
        // Arrange: Valid M4A input
        Map<String, Object> input = createValidInput("test-bucket", "audio/test.m4a");

        // Act
        Map<String, Object> result = processor.handleRequest(input, mockContext);

        // Assert: Should return success status
        assertNotNull(result);
        assertEquals("VALIDATED", result.get("status"));
    }

    @Test
    void rejectsFileWithNoExtension() {
        // Arrange: File without extension
        Map<String, Object> input = createValidInput("test-bucket", "audio/testfile");

        // Act & Assert: Should throw IllegalArgumentException
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> processor.handleRequest(input, mockContext)
        );
        assertTrue(exception.getMessage().contains("File has no extension"));
    }

    @Test
    void returnsErrorStatusOnValidationFailure() {
        // Arrange: Invalid input
        Map<String, Object> input = createValidInput("test-bucket", "audio/test.exe");

        // Act & Assert: Should throw exception with proper error information
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            processor.handleRequest(input, mockContext);
        });

        assertNotNull(exception.getMessage());
    }

    @Test
    void includesFileExtensionInValidationResponse() {
        // Arrange: Valid input
        Map<String, Object> input = createValidInput("test-bucket", "audio/test.mp3");

        // Act
        Map<String, Object> result = processor.handleRequest(input, mockContext);

        // Assert: Response should include file extension or validation details
        assertNotNull(result);
        assertTrue(result.containsKey("status") || result.containsKey("fileExtension") || 
                   result.containsKey("validationDetails"),
            "Response should contain validation information");
    }

    // ===== Helper Methods =====

    private Map<String, Object> createValidInput(String bucketName, String objectKey) {
        Map<String, Object> input = new HashMap<>();
        Map<String, Object> detail = new HashMap<>();
        Map<String, Object> bucket = new HashMap<>();
        Map<String, Object> object = new HashMap<>();

        bucket.put("name", bucketName);
        object.put("key", objectKey);

        detail.put("bucket", bucket);
        detail.put("object", object);
        input.put("detail", detail);

        return input;
    }
}
