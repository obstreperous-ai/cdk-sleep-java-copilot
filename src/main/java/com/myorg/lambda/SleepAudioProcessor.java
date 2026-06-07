package com.myorg.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.LambdaLogger;

import java.util.Map;
import java.util.HashMap;

/**
 * Lambda function for processing sleep audio metadata.
 * This is a minimal skeleton that logs input and returns enriched metadata.
 * Future enhancements: validation, metadata enrichment, audio format detection.
 * 
 * Issue #7: Basic Lambda Function Skeleton + Integration with State Machine
 */
public class SleepAudioProcessor implements RequestHandler<Map<String, Object>, Map<String, Object>> {
    
    private static final String METADATA_TABLE_NAME_ENV = "METADATA_TABLE_NAME";
    
    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        LambdaLogger logger = context.getLogger();
        
        // Log the input for observability
        logger.log("SleepAudioProcessor invoked with input: " + input.toString());
        
        // Get environment variable
        String tableName = System.getenv(METADATA_TABLE_NAME_ENV);
        logger.log("Using DynamoDB table: " + tableName);
        
        // Extract S3 event details from input (passed from state machine)
        Map<String, Object> detail = (Map<String, Object>) input.get("detail");
        if (detail != null) {
            Map<String, Object> bucket = (Map<String, Object>) detail.get("bucket");
            Map<String, Object> object = (Map<String, Object>) detail.get("object");
            
            if (bucket != null && object != null) {
                String bucketName = (String) bucket.get("name");
                String objectKey = (String) object.get("key");
                
                logger.log("Processing audio file: s3://" + bucketName + "/" + objectKey);
            }
        }
        
        // Create enriched response (minimal for now)
        Map<String, Object> response = new HashMap<>();
        response.put("status", "VALIDATED");
        response.put("processorVersion", "1.0.0");
        response.put("timestamp", System.currentTimeMillis());
        response.put("message", "Audio metadata validated and enriched");
        
        // Future: Add DynamoDB read/write, validation logic, metadata extraction
        
        logger.log("SleepAudioProcessor completed successfully");
        return response;
    }
}
