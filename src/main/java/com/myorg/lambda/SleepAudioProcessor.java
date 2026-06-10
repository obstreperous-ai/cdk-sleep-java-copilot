package com.myorg.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.LambdaLogger;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

/**
 * Lambda function for processing sleep audio metadata.
 * This validates inputs and enriches metadata before audio processing.
 * 
 * Issue #7: Basic Lambda Function Skeleton + Integration with State Machine
 * Issue #8: Input Validation for Pipeline Wiring
 * Issue #10: Enhanced with structured JSON logging for observability
 */
public class SleepAudioProcessor implements RequestHandler<Map<String, Object>, Map<String, Object>> {
    
    private static final String METADATA_TABLE_NAME_ENV = "METADATA_TABLE_NAME";
    
    // Supported audio file extensions (Issue #8)
    private static final Set<String> SUPPORTED_EXTENSIONS = new HashSet<>();
    static {
        SUPPORTED_EXTENSIONS.add(".mp3");
        SUPPORTED_EXTENSIONS.add(".wav");
        SUPPORTED_EXTENSIONS.add(".m4a");
    }
    
    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        LambdaLogger logger = context.getLogger();
        String requestId = context.getAwsRequestId();
        
        // Issue #10: Structured JSON logging for better observability
        logStructured(logger, "INFO", "Lambda invoked", requestId, null, null);
        
        // Get environment variable
        String tableName = System.getenv(METADATA_TABLE_NAME_ENV);
        Map<String, Object> tableContext = new HashMap<>();
        tableContext.put("tableName", tableName);
        logStructured(logger, "INFO", "Using DynamoDB table", requestId, null, tableContext);
        
        try {
            // Extract and validate S3 event details from input (Issue #8)
            Map<String, Object> detail = (Map<String, Object>) input.get("detail");
            if (detail == null) {
                throw new IllegalArgumentException("Missing required field: detail");
            }
            
            Map<String, Object> bucket = (Map<String, Object>) detail.get("bucket");
            Map<String, Object> object = (Map<String, Object>) detail.get("object");
            
            if (bucket == null) {
                throw new IllegalArgumentException("Missing required field: bucket");
            }
            
            if (object == null) {
                throw new IllegalArgumentException("Missing required field: object key");
            }
            
            String bucketName = (String) bucket.get("name");
            String objectKey = (String) object.get("key");
            
            if (bucketName == null || bucketName.isEmpty()) {
                throw new IllegalArgumentException("Missing required field: bucket name");
            }
            
            if (objectKey == null || objectKey.isEmpty()) {
                throw new IllegalArgumentException("Missing required field: object key");
            }
            
            // Issue #10: Structured logging with audio file context
            Map<String, Object> fileContext = new HashMap<>();
            fileContext.put("bucketName", bucketName);
            fileContext.put("objectKey", objectKey);
            logStructured(logger, "INFO", "Processing audio file", requestId, objectKey, fileContext);
            
            // Validate file extension (Issue #8)
            String fileExtension = getFileExtension(objectKey);
            if (fileExtension.isEmpty()) {
                throw new IllegalArgumentException(
                    "File has no extension. Supported formats: " + SUPPORTED_EXTENSIONS);
            }
            if (!SUPPORTED_EXTENSIONS.contains(fileExtension.toLowerCase())) {
                throw new IllegalArgumentException(
                    "Unsupported file extension: " + fileExtension + 
                    ". Supported formats: " + SUPPORTED_EXTENSIONS);
            }
            
            // Issue #10: Structured logging for validation success
            Map<String, Object> validationContext = new HashMap<>();
            validationContext.put("fileExtension", fileExtension);
            logStructured(logger, "INFO", "File extension validated", requestId, objectKey, validationContext);
            
            // Create enriched response with validation details
            Map<String, Object> response = new HashMap<>();
            response.put("status", "VALIDATED");
            response.put("processorVersion", "1.0.0");
            response.put("timestamp", System.currentTimeMillis());
            response.put("message", "Audio metadata validated and enriched");
            response.put("fileExtension", fileExtension);
            response.put("bucketName", bucketName);
            response.put("objectKey", objectKey);
            response.put("requestId", requestId);
            
            // Issue #10: Structured logging for successful completion
            Map<String, Object> completionContext = new HashMap<>();
            completionContext.put("status", "VALIDATED");
            logStructured(logger, "INFO", "Processing completed successfully", requestId, objectKey, completionContext);
            
            return response;
        } catch (Exception e) {
            // Issue #10: Structured error logging
            Map<String, Object> errorContext = new HashMap<>();
            errorContext.put("errorType", e.getClass().getSimpleName());
            errorContext.put("errorMessage", e.getMessage());
            logStructured(logger, "ERROR", "Processing failed: " + e.getMessage(), requestId, null, errorContext);
            throw e;
        }
    }
    
    /**
     * Issue #10: Helper method for structured JSON logging
     * Logs messages in JSON format for better parsing and analysis
     */
    private void logStructured(LambdaLogger logger, String level, String message, 
                               String requestId, String objectKey, Map<String, Object> additionalFields) {
        Map<String, Object> logEntry = new HashMap<>();
        logEntry.put("timestamp", System.currentTimeMillis());
        logEntry.put("level", level != null ? level : "INFO");
        logEntry.put("message", message != null ? message : "");
        
        if (requestId != null) {
            logEntry.put("requestId", requestId);
        }
        
        if (objectKey != null) {
            logEntry.put("objectKey", objectKey);
        }
        
        if (additionalFields != null) {
            logEntry.putAll(additionalFields);
        }
        
        // Simple JSON-like logging (in production, consider using a JSON library)
        logger.log(logEntry.toString());
    }
    
    /**
     * Extract file extension from object key.
     * Issue #8: Input validation helper
     */
    private String getFileExtension(String objectKey) {
        int lastDotIndex = objectKey.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < objectKey.length() - 1) {
            return objectKey.substring(lastDotIndex);
        }
        return "";
    }
}
