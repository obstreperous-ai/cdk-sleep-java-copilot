package com.myorg.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

import java.time.Instant;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

/**
 * Lambda function for processing sleep audio metadata and performing audio processing.
 * This validates inputs, downloads audio from S3, processes it, uploads to output bucket,
 * and updates DynamoDB metadata.
 * 
 * Issue #7: Basic Lambda Function Skeleton + Integration with State Machine
 * Issue #8: Input Validation for Pipeline Wiring
 * Issue #10: Enhanced with structured JSON logging for observability
 * Issue #11: Full audio processing implementation with S3 download/upload and DynamoDB updates
 */
public class SleepAudioProcessor implements RequestHandler<Map<String, Object>, Map<String, Object>> {
    
    private static final String METADATA_TABLE_NAME_ENV = "METADATA_TABLE_NAME";
    private static final String OUTPUT_BUCKET_NAME_ENV = "OUTPUT_BUCKET_NAME";
    
    // Supported audio file extensions (Issue #8)
    private static final Set<String> SUPPORTED_EXTENSIONS = new HashSet<>();
    static {
        SUPPORTED_EXTENSIONS.add(".mp3");
        SUPPORTED_EXTENSIONS.add(".wav");
        SUPPORTED_EXTENSIONS.add(".m4a");
    }
    
    // AWS SDK clients (Issue #11) - initialized lazily to support testing
    private S3Client s3Client;
    private DynamoDbClient dynamoDbClient;
    
    public SleepAudioProcessor() {
        // Default constructor for Lambda
    }
    
    // Constructor for testing with mock clients
    public SleepAudioProcessor(S3Client s3Client, DynamoDbClient dynamoDbClient) {
        this.s3Client = s3Client;
        this.dynamoDbClient = dynamoDbClient;
    }
    
    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        LambdaLogger logger = context.getLogger();
        String requestId = context.getAwsRequestId();
        
        // Issue #10: Structured JSON logging for better observability
        logStructured(logger, "INFO", "Lambda invoked", requestId, null, null);
        
        // Get environment variables
        String tableName = System.getenv(METADATA_TABLE_NAME_ENV);
        String outputBucketName = System.getenv(OUTPUT_BUCKET_NAME_ENV);
        
        // Initialize AWS SDK clients if not already set (Issue #11)
        if (s3Client == null) {
            s3Client = S3Client.builder()
                    .region(Region.US_EAST_1)  // Default region; can be configured via env var
                    .build();
        }
        if (dynamoDbClient == null) {
            dynamoDbClient = DynamoDbClient.builder()
                    .region(Region.US_EAST_1)
                    .build();
        }
        
        Map<String, Object> tableContext = new HashMap<>();
        tableContext.put("tableName", tableName);
        tableContext.put("outputBucket", outputBucketName);
        logStructured(logger, "INFO", "Using DynamoDB table and output bucket", requestId, null, tableContext);
        
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
            
            // Issue #11: Full audio processing implementation
            long startTime = System.currentTimeMillis();
            
            // Step 1: Download the input audio file from S3
            logStructured(logger, "INFO", "Downloading audio from S3", requestId, objectKey, null);
            byte[] audioData = downloadFromS3(bucketName, objectKey);
            long downloadSize = audioData.length;
            
            Map<String, Object> downloadContext = new HashMap<>();
            downloadContext.put("downloadSize", downloadSize);
            logStructured(logger, "INFO", "Audio downloaded successfully", requestId, objectKey, downloadContext);
            
            // Step 2: Process the audio (basic processing for now)
            // In a real implementation, this would involve audio enhancement, normalization, etc.
            // For this MVP, we'll simulate processing by copying the audio data
            logStructured(logger, "INFO", "Processing audio", requestId, objectKey, null);
            byte[] processedAudio = processAudio(audioData, fileExtension);
            
            // Step 3: Generate unique output key with timestamp
            String timestamp = String.valueOf(Instant.now().toEpochMilli());
            String outputKey = generateOutputKey(objectKey, timestamp);
            
            Map<String, Object> outputContext = new HashMap<>();
            outputContext.put("outputKey", outputKey);
            outputContext.put("outputBucket", outputBucketName);
            logStructured(logger, "INFO", "Generated output key", requestId, objectKey, outputContext);
            
            // Step 4: Upload processed audio to output S3 bucket
            logStructured(logger, "INFO", "Uploading processed audio to S3", requestId, objectKey, null);
            uploadToS3(outputBucketName, outputKey, processedAudio);
            
            long outputSize = processedAudio.length;
            Map<String, Object> uploadContext = new HashMap<>();
            uploadContext.put("outputSize", outputSize);
            logStructured(logger, "INFO", "Processed audio uploaded successfully", requestId, objectKey, uploadContext);
            
            // Step 5: Update DynamoDB metadata table
            String outputLocation = "s3://" + outputBucketName + "/" + outputKey;
            updateDynamoDBMetadata(tableName, objectKey, outputLocation, outputSize, "COMPLETED");
            
            logStructured(logger, "INFO", "DynamoDB metadata updated", requestId, objectKey, null);
            
            long processingDuration = System.currentTimeMillis() - startTime;
            
            // Create enriched response with processing details
            Map<String, Object> response = new HashMap<>();
            response.put("status", "COMPLETED");
            response.put("processorVersion", "1.0.0");
            response.put("timestamp", System.currentTimeMillis());
            response.put("message", "Audio processing completed successfully");
            response.put("fileExtension", fileExtension);
            response.put("bucketName", bucketName);
            response.put("objectKey", objectKey);
            response.put("outputBucket", outputBucketName);
            response.put("outputKey", outputKey);
            response.put("outputLocation", outputLocation);
            response.put("outputSize", outputSize);
            response.put("processingDuration", processingDuration);
            response.put("requestId", requestId);
            response.put("metadataUpdated", true);
            
            // Issue #10: Structured logging for successful completion
            Map<String, Object> completionContext = new HashMap<>();
            completionContext.put("status", "COMPLETED");
            completionContext.put("processingDuration", processingDuration);
            completionContext.put("outputLocation", outputLocation);
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
     * Download audio file from S3 bucket.
     * Issue #11: S3 download implementation
     */
    private byte[] downloadFromS3(String bucketName, String objectKey) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build();
            
            ResponseBytes<GetObjectResponse> objectBytes = s3Client.getObjectAsBytes(getObjectRequest);
            return objectBytes.asByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to download file from S3: " + e.getMessage(), e);
        }
    }
    
    /**
     * Process the audio data (basic implementation).
     * Issue #11: Audio processing placeholder
     * 
     * In a real implementation, this would:
     * - Normalize audio levels
     * - Apply filters for relaxation (e.g., low-pass filter)
     * - Mix with generated ambient sounds
     * - Use Amazon Polly for narration if input is text
     * 
     * For now, we simulate processing by returning the original audio data.
     */
    private byte[] processAudio(byte[] audioData, String fileExtension) {
        // Basic processing: In real implementation, this would involve audio manipulation
        // For MVP, we just return the audio data as-is to demonstrate the pipeline
        return audioData;
    }
    
    /**
     * Generate unique output key with timestamp.
     * Issue #11: Output key generation
     */
    private String generateOutputKey(String originalKey, String timestamp) {
        // Extract filename without extension
        int lastSlashIndex = originalKey.lastIndexOf('/');
        int lastDotIndex = originalKey.lastIndexOf('.');
        
        String basePath = "";
        String filename = originalKey;
        String extension = "";
        
        if (lastSlashIndex >= 0) {
            basePath = originalKey.substring(0, lastSlashIndex + 1);
            filename = originalKey.substring(lastSlashIndex + 1);
        }
        
        if (lastDotIndex > lastSlashIndex) {
            extension = originalKey.substring(lastDotIndex);
            filename = originalKey.substring(lastSlashIndex + 1, lastDotIndex);
        }
        
        // Generate output key: processed/{originalFilename}_{timestamp}{extension}
        return "processed/" + filename + "_" + timestamp + extension;
    }
    
    /**
     * Upload processed audio to S3 output bucket.
     * Issue #11: S3 upload implementation
     */
    private void uploadToS3(String bucketName, String objectKey, byte[] audioData) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .contentType("audio/mpeg")  // Default to MP3, can be enhanced based on file type
                .build();
        
        s3Client.putObject(putObjectRequest, RequestBody.fromBytes(audioData));
    }
    
    /**
     * Update DynamoDB metadata table with processing results.
     * Issue #11: DynamoDB update implementation
     */
    private void updateDynamoDBMetadata(String tableName, String audioId, String outputLocation, 
                                        long outputSize, String status) {
        Map<String, AttributeValue> key = new HashMap<>();
        key.put("audioId", AttributeValue.builder().s(audioId).build());
        
        Map<String, AttributeValue> attributeUpdates = new HashMap<>();
        attributeUpdates.put(":status", AttributeValue.builder().s(status).build());
        attributeUpdates.put(":outputLocation", AttributeValue.builder().s(outputLocation).build());
        attributeUpdates.put(":outputSize", AttributeValue.builder().n(String.valueOf(outputSize)).build());
        attributeUpdates.put(":updatedAt", AttributeValue.builder().s(Instant.now().toString()).build());
        
        UpdateItemRequest updateRequest = UpdateItemRequest.builder()
                .tableName(tableName)
                .key(key)
                .updateExpression("SET #status = :status, #outputLocation = :outputLocation, #outputSize = :outputSize, #updatedAt = :updatedAt")
                .expressionAttributeNames(Map.of(
                    "#status", "status",
                    "#outputLocation", "outputLocation",
                    "#outputSize", "outputSize",
                    "#updatedAt", "updatedAt"
                ))
                .expressionAttributeValues(attributeUpdates)
                .build();
        
        dynamoDbClient.updateItem(updateRequest);
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
