package com.myorg;

import software.constructs.Construct;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.services.s3.Bucket;
import software.amazon.awscdk.services.s3.BucketEncryption;
import software.amazon.awscdk.services.s3.BlockPublicAccess;
import software.amazon.awscdk.services.events.Rule;
import software.amazon.awscdk.services.events.EventPattern;
import software.amazon.awscdk.services.events.targets.SfnStateMachine;
import software.amazon.awscdk.services.events.RuleTargetInput;
import software.amazon.awscdk.services.stepfunctions.StateMachine;
import software.amazon.awscdk.services.stepfunctions.StateMachineType;
import software.amazon.awscdk.services.stepfunctions.LogLevel;
import software.amazon.awscdk.services.stepfunctions.LogOptions;
import software.amazon.awscdk.services.stepfunctions.tasks.CallAwsService;
import software.amazon.awscdk.services.stepfunctions.tasks.DynamoPutItem;
import software.amazon.awscdk.services.stepfunctions.tasks.DynamoAttributeValue;
import software.amazon.awscdk.services.stepfunctions.Pass;
import software.amazon.awscdk.services.stepfunctions.Succeed;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.services.dynamodb.Table;
import software.amazon.awscdk.services.dynamodb.Attribute;
import software.amazon.awscdk.services.dynamodb.AttributeType;
import software.amazon.awscdk.services.dynamodb.BillingMode;
import software.amazon.awscdk.services.dynamodb.TableEncryption;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class CdkBaseStack extends Stack {
    // Placeholder values for initial Polly integration (Issue #4)
    // These will be replaced with dynamic input from S3 events in future issues
    private static final String PLACEHOLDER_POLLY_TEXT = "Placeholder text for sleep audio narration";
    private static final String POLLY_VOICE_ID = "Joanna";
    private static final String POLLY_OUTPUT_FORMAT = "mp3";
    
    // DynamoDB metadata status values (Issue #5)
    private static final String STATUS_PROCESSING = "PROCESSING";
    
    // JsonPath expressions for S3 event data (Issue #5)
    private static final String JSONPATH_OBJECT_KEY = "$.detail.object.key";
    private static final String JSONPATH_BUCKET_NAME = "$.detail.bucket.name";
    private static final String JSONPATH_EVENT_TIME = "$.time";
    
    public CdkBaseStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public CdkBaseStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        // Input S3 Bucket - for raw audio uploads
        Bucket inputBucket = Bucket.Builder.create(this, "SleepAudioInputBucket")
                .encryption(BucketEncryption.S3_MANAGED)
                .versioned(true)
                .eventBridgeEnabled(true)
                .blockPublicAccess(BlockPublicAccess.BLOCK_ALL)
                .removalPolicy(RemovalPolicy.RETAIN)
                .build();

        // Output S3 Bucket - for processed audio
        Bucket outputBucket = Bucket.Builder.create(this, "SleepAudioOutputBucket")
                .encryption(BucketEncryption.S3_MANAGED)
                .versioned(true)
                .blockPublicAccess(BlockPublicAccess.BLOCK_ALL)
                .removalPolicy(RemovalPolicy.RETAIN)
                .build();

        // DynamoDB Table - for audio pipeline metadata (Issue #5)
        Table metadataTable = Table.Builder.create(this, "SleepAudioMetadataTable")
                .partitionKey(Attribute.builder()
                        .name("audioId")
                        .type(AttributeType.STRING)
                        .build())
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .encryption(TableEncryption.AWS_MANAGED)
                .pointInTimeRecovery(true)  // Deprecated but works; will migrate in future
                .removalPolicy(RemovalPolicy.RETAIN)
                .build();

        // CloudWatch Log Group for Step Functions state machine
        LogGroup stateMachineLogGroup = LogGroup.Builder.create(this, "StateMachineLogGroup")
                .retention(RetentionDays.ONE_WEEK)
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();

        // DynamoDB PutItem Task - Write initial metadata record (Issue #5)
        // This writes the initial record when the pipeline starts
        DynamoPutItem putMetadataTask = DynamoPutItem.Builder.create(this, "PutMetadataTask")
                .table(metadataTable)
                .item(Map.of(
                    "audioId", DynamoAttributeValue.fromString(JSONPATH_OBJECT_KEY),
                    "status", DynamoAttributeValue.fromString(STATUS_PROCESSING),
                    "inputBucket", DynamoAttributeValue.fromString(JSONPATH_BUCKET_NAME),
                    "inputKey", DynamoAttributeValue.fromString(JSONPATH_OBJECT_KEY),
                    "createdAt", DynamoAttributeValue.fromString(JSONPATH_EVENT_TIME)
                ))
                .resultPath("$.dynamoResult")
                .build();

        // Polly Task - Minimal integration using CallAwsService
        // This is a placeholder that will invoke Polly StartSpeechSynthesisTask
        // NOTE: iamResources uses wildcard for initial development (Issue #4)
        // TODO: Scope to specific resources before production (e.g., output bucket ARN, specific Polly task ARNs)
        CallAwsService pollyTask = CallAwsService.Builder.create(this, "PollyTask")
                .service("polly")
                .action("startSpeechSynthesisTask")
                .parameters(Map.of(
                    "Text", PLACEHOLDER_POLLY_TEXT,
                    "OutputFormat", POLLY_OUTPUT_FORMAT,
                    "VoiceId", POLLY_VOICE_ID,
                    "OutputS3BucketName", outputBucket.getBucketName()
                ))
                .iamResources(List.of("*"))  // Broad permissions for development; narrow before production
                .resultPath("$.pollyResult")
                .build();

        // Success state
        Succeed successState = Succeed.Builder.create(this, "Success")
                .comment("Processing completed successfully")
                .build();

        // Chain the states: PutMetadata -> Polly Task -> Success (Issue #5)
        putMetadataTask.next(pollyTask).next(successState);

        // Step Functions State Machine
        StateMachine stateMachine = StateMachine.Builder.create(this, "SleepAudioPipelineStateMachine")
                .stateMachineName("SleepAudioPipelineStateMachine")
                .stateMachineType(StateMachineType.STANDARD)
                .definitionBody(software.amazon.awscdk.services.stepfunctions.DefinitionBody.fromChainable(putMetadataTask))
                .logs(LogOptions.builder()
                        .destination(stateMachineLogGroup)
                        .level(LogLevel.ALL)
                        .includeExecutionData(true)
                        .build())
                .build();

        // Grant the state machine permissions to write to the output bucket
        outputBucket.grantWrite(stateMachine);

        // Grant the state machine permissions to write to DynamoDB table (Issue #5)
        metadataTable.grantWriteData(stateMachine);

        // EventBridge Rule - triggers on S3 Object Created events from input bucket
        Rule eventRule = Rule.Builder.create(this, "S3ObjectCreatedRule")
                .eventPattern(EventPattern.builder()
                        .source(List.of("aws.s3"))
                        .detailType(List.of("Object Created"))
                        .detail(Map.of(
                                "bucket", Map.of(
                                        "name", List.of(inputBucket.getBucketName())
                                )
                        ))
                        .build())
                .build();

        // Add Step Functions state machine as target with input transformation (Issue #5)
        // Transform S3 event to pass relevant data to state machine
        eventRule.addTarget(SfnStateMachine.Builder.create(stateMachine)
                .input(RuleTargetInput.fromObject(Map.of(
                    "detail", Map.of(
                        "bucket", Map.of(
                            "name", software.amazon.awscdk.services.events.EventField.fromPath("$.detail.bucket.name")
                        ),
                        "object", Map.of(
                            "key", software.amazon.awscdk.services.events.EventField.fromPath("$.detail.object.key")
                        )
                    ),
                    "time", software.amazon.awscdk.services.events.EventField.fromPath("$.time")
                )))
                .build());
    }
}
