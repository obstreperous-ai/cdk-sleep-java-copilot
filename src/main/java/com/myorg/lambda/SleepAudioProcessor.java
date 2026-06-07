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
        
        // Log the input for observability
        logger.log("SleepAudioProcessor invoked with input: " + input.toString());
        
        // Get environment variable
        String tableName = System.getenv(METADATA_TABLE_NAME_ENV);
        logger.log("Using DynamoDB table: " + tableName);
        
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
        
        logger.log("Processing audio file: s3://" + bucketName + "/" + objectKey);
        
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
        
        logger.log("File extension validated: " + fileExtension);
        
        // Create enriched response with validation details
        Map<String, Object> response = new HashMap<>();
        response.put("status", "VALIDATED");
        response.put("processorVersion", "1.0.0");
        response.put("timestamp", System.currentTimeMillis());
        response.put("message", "Audio metadata validated and enriched");
        response.put("fileExtension", fileExtension);
        response.put("bucketName", bucketName);
        response.put("objectKey", objectKey);
        
        // Future: Add DynamoDB read/write, metadata extraction
        
        logger.log("SleepAudioProcessor completed successfully");
        return response;
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
