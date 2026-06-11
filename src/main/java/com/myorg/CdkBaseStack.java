package com.myorg;

import software.constructs.Construct;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.Tags;
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
import software.amazon.awscdk.services.stepfunctions.tasks.LambdaInvoke;
import software.amazon.awscdk.services.stepfunctions.Pass;
import software.amazon.awscdk.services.stepfunctions.Succeed;
import software.amazon.awscdk.services.stepfunctions.Fail;
import software.amazon.awscdk.services.stepfunctions.Errors;
import software.amazon.awscdk.services.stepfunctions.CatchProps;
import software.amazon.awscdk.services.stepfunctions.RetryProps;
import software.amazon.awscdk.services.stepfunctions.tasks.SnsPublish;
import software.amazon.awscdk.services.stepfunctions.tasks.DynamoUpdateItem;
import software.amazon.awscdk.services.stepfunctions.TaskInput;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.services.dynamodb.Table;
import software.amazon.awscdk.services.dynamodb.Attribute;
import software.amazon.awscdk.services.dynamodb.AttributeType;
import software.amazon.awscdk.services.dynamodb.BillingMode;
import software.amazon.awscdk.services.dynamodb.TableEncryption;
import software.amazon.awscdk.services.dynamodb.PointInTimeRecoverySpecification;
import software.amazon.awscdk.services.sns.Topic;
import software.amazon.awscdk.services.kms.Key;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Tracing;
import software.amazon.awscdk.services.cloudwatch.Alarm;
import software.amazon.awscdk.services.cloudwatch.ComparisonOperator;
import software.amazon.awscdk.services.cloudwatch.TreatMissingData;
import software.amazon.awscdk.services.cloudwatch.Metric;
import software.amazon.awscdk.services.cloudwatch.Unit;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class CdkBaseStack extends Stack {
    // Placeholder values for initial Polly integration (Issue #4)
    // These will be replaced with dynamic input from S3 events in future issues
    private static final String PLACEHOLDER_POLLY_TEXT = "Placeholder text for sleep audio narration";
    private static final String POLLY_VOICE_ID = "Joanna";
    private static final String POLLY_OUTPUT_FORMAT = "mp3";
    
    // DynamoDB metadata status values (Issue #5 and #6)
    private static final String STATUS_PROCESSING = "PROCESSING";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_FAILED = "FAILED";
    
    // JsonPath expressions for S3 event data (Issue #5)
    private static final String JSONPATH_OBJECT_KEY = "$.detail.object.key";
    private static final String JSONPATH_BUCKET_NAME = "$.detail.bucket.name";
    private static final String JSONPATH_EVENT_TIME = "$.time";
    
    public CdkBaseStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public CdkBaseStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        // Issue #9: Multi-environment support
        // Get environment from context (dev, stage, prod), default to "dev"
        String environment = (String) this.getNode().tryGetContext("environment");
        if (environment == null || environment.isEmpty()) {
            environment = "dev";
        }

        // Issue #9: Environment-specific removal policy
        // Dev environments use DESTROY for easy cleanup, prod uses RETAIN for data protection
        RemovalPolicy removalPolicy = environment.equals("prod") ? RemovalPolicy.RETAIN : RemovalPolicy.DESTROY;

        // Issue #9: Apply environment tag to all resources in stack
        Tags.of(this).add("Environment", environment);
        Tags.of(this).add("Project", "SleepAudioPipeline");

        // Input S3 Bucket - for raw audio uploads
        Bucket inputBucket = Bucket.Builder.create(this, "SleepAudioInputBucket")
                .encryption(BucketEncryption.S3_MANAGED)
                .versioned(true)
                .eventBridgeEnabled(true)
                .blockPublicAccess(BlockPublicAccess.BLOCK_ALL)
                .removalPolicy(removalPolicy)
                .build();

        // Output S3 Bucket - for processed audio
        Bucket outputBucket = Bucket.Builder.create(this, "SleepAudioOutputBucket")
                .encryption(BucketEncryption.S3_MANAGED)
                .versioned(true)
                .blockPublicAccess(BlockPublicAccess.BLOCK_ALL)
                .removalPolicy(removalPolicy)
                .build();

        // DynamoDB Table - for audio pipeline metadata (Issue #5, #9)
        Table metadataTable = Table.Builder.create(this, "SleepAudioMetadataTable")
                .partitionKey(Attribute.builder()
                        .name("audioId")
                        .type(AttributeType.STRING)
                        .build())
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .encryption(TableEncryption.AWS_MANAGED)
                .pointInTimeRecoverySpecification(PointInTimeRecoverySpecification.builder()
                        .pointInTimeRecoveryEnabled(true)
                        .build())  // Issue #9: Updated from deprecated pointInTimeRecovery
                .removalPolicy(removalPolicy)
                .build();

        // KMS Key for SNS Topic Encryption (Issue #6)
        Key snsEncryptionKey = Key.Builder.create(this, "SnsEncryptionKey")
                .description("KMS key for SNS topic encryption")
                .enableKeyRotation(true)
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();

        // SNS Topics for Pipeline Notifications (Issue #6)
        Topic completedTopic = Topic.Builder.create(this, "SleepAudioPipelineCompletedTopic")
                .displayName("Sleep Audio Pipeline Completed")
                .masterKey(snsEncryptionKey)
                .build();

        Topic failedTopic = Topic.Builder.create(this, "SleepAudioPipelineFailedTopic")
                .displayName("Sleep Audio Pipeline Failed")
                .masterKey(snsEncryptionKey)
                .build();

        // CloudWatch Log Group for Step Functions state machine
        LogGroup stateMachineLogGroup = LogGroup.Builder.create(this, "StateMachineLogGroup")
                .retention(RetentionDays.ONE_WEEK)
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();

        // DynamoDB PutItem Task - Write initial metadata record (Issue #5)
        // This writes the initial record when the pipeline starts
        // Issue #10: Retry policy configured for transient failures
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
        
        // Issue #10: Add retry policy for DynamoDB transient errors
        putMetadataTask.addRetry(RetryProps.builder()
                .errors(List.of(
                    "DynamoDB.ProvisionedThroughputExceededException",
                    "DynamoDB.RequestLimitExceeded",
                    "DynamoDB.InternalServerError",
                    "States.Timeout"
                ))
                .interval(Duration.seconds(2))
                .maxAttempts(3)
                .backoffRate(2.0)
                .build());

        // Lambda Function - Sleep Audio Processor (Issue #7)
        // Basic skeleton for audio processing, metadata enrichment, and validation
        // Issue #10: X-Ray tracing enabled for observability
        // Issue #11: Full audio processing with S3 and DynamoDB integration
        Function audioProcessorFunction = Function.Builder.create(this, "SleepAudioProcessorFunction")
                .functionName("SleepAudioProcessor")
                .runtime(Runtime.JAVA_17)
                .handler("com.myorg.lambda.SleepAudioProcessor::handleRequest")
                .code(Code.fromAsset("target/classes"))
                .timeout(Duration.seconds(60))  // Issue #11: Increased timeout for audio processing
                .memorySize(1024)  // Issue #11: Increased memory for audio processing
                .tracing(Tracing.ACTIVE)  // Issue #10: Enable X-Ray tracing
                .environment(Map.of(
                    "METADATA_TABLE_NAME", metadataTable.getTableName(),
                    "OUTPUT_BUCKET_NAME", outputBucket.getBucketName()  // Issue #11: Output bucket name
                ))
                .build();

        // Issue #11: Grant Lambda function permissions to access S3 buckets and DynamoDB table
        inputBucket.grantRead(audioProcessorFunction);
        outputBucket.grantWrite(audioProcessorFunction);
        metadataTable.grantReadWriteData(audioProcessorFunction);

        // Lambda Invoke Task - Process audio metadata (Issue #7)
        // This task invokes the Lambda function to validate and enrich metadata
        // By default, LambdaInvoke passes the entire state input to the Lambda
        // Issue #10: Retry policy configured for transient Lambda failures
        LambdaInvoke processAudioTask = LambdaInvoke.Builder.create(this, "ProcessAudioTask")
                .lambdaFunction(audioProcessorFunction)
                .resultPath("$.processorResult")
                .build();
        
        // Issue #10: Add retry policy for Lambda service errors with exponential backoff
        processAudioTask.addRetry(RetryProps.builder()
                .errors(List.of(
                    "Lambda.ServiceException",
                    "Lambda.TooManyRequestsException",
                    "Lambda.SdkClientException",
                    "States.Timeout"
                ))
                .interval(Duration.seconds(2))
                .maxAttempts(3)
                .backoffRate(2.0)
                .build());

        // Polly Task - Minimal integration using CallAwsService
        // This is a placeholder that will invoke Polly StartSpeechSynthesisTask
        // NOTE: iamResources uses wildcard for initial development (Issue #4)
        // TODO: Scope to specific resources before production (e.g., output bucket ARN, specific Polly task ARNs)
        // Issue #10: Retry policy configured for transient Polly failures
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
        
        // Issue #10: Add retry policy for Polly service errors with exponential backoff
        pollyTask.addRetry(RetryProps.builder()
                .errors(List.of(
                    "Polly.ServiceException",
                    "Polly.ThrottlingException",
                    "States.Timeout"
                ))
                .interval(Duration.seconds(3))
                .maxAttempts(3)
                .backoffRate(2.0)
                .build());

        // DynamoDB UpdateItem Task - Update status to COMPLETED (Issue #6)
        DynamoUpdateItem updateStatusToCompleted = DynamoUpdateItem.Builder.create(this, "UpdateStatusToCompleted")
                .table(metadataTable)
                .key(Map.of(
                    "audioId", DynamoAttributeValue.fromString(JSONPATH_OBJECT_KEY)
                ))
                .updateExpression("SET #status = :completed, #updatedAt = :updatedAt")
                .expressionAttributeNames(Map.of(
                    "#status", "status",
                    "#updatedAt", "updatedAt"
                ))
                .expressionAttributeValues(Map.of(
                    ":completed", DynamoAttributeValue.fromString(STATUS_COMPLETED),
                    ":updatedAt", DynamoAttributeValue.fromString("$$.State.EnteredTime")
                ))
                .resultPath("$.updateResult")
                .build();

        // SNS Publish Task - Send success notification (Issue #6)
        SnsPublish publishSuccessNotification = SnsPublish.Builder.create(this, "PublishSuccessNotification")
                .topic(completedTopic)
                .message(TaskInput.fromObject(Map.of(
                    "status", "COMPLETED",
                    "audioId", TaskInput.fromJsonPathAt(JSONPATH_OBJECT_KEY),
                    "message", "Sleep audio pipeline completed successfully",
                    "timestamp", TaskInput.fromJsonPathAt("$$.State.EnteredTime")
                )))
                .subject("Sleep Audio Pipeline - Processing Completed")
                .resultPath("$.snsResult")
                .build();

        // Success state
        Succeed successState = Succeed.Builder.create(this, "Success")
                .comment("Processing completed successfully")
                .build();

        // DynamoDB UpdateItem Task - Update status to FAILED (Issue #6)
        DynamoUpdateItem updateStatusToFailed = DynamoUpdateItem.Builder.create(this, "UpdateStatusToFailed")
                .table(metadataTable)
                .key(Map.of(
                    "audioId", DynamoAttributeValue.fromString(JSONPATH_OBJECT_KEY)
                ))
                .updateExpression("SET #status = :failed, #updatedAt = :updatedAt, #errorInfo = :errorInfo")
                .expressionAttributeNames(Map.of(
                    "#status", "status",
                    "#updatedAt", "updatedAt",
                    "#errorInfo", "errorInfo"
                ))
                .expressionAttributeValues(Map.of(
                    ":failed", DynamoAttributeValue.fromString(STATUS_FAILED),
                    ":updatedAt", DynamoAttributeValue.fromString("$$.State.EnteredTime"),
                    ":errorInfo", DynamoAttributeValue.fromString("$.Error")
                ))
                .resultPath("$.updateResult")
                .build();

        // SNS Publish Task - Send failure notification (Issue #6)
        SnsPublish publishFailureNotification = SnsPublish.Builder.create(this, "PublishFailureNotification")
                .topic(failedTopic)
                .message(TaskInput.fromObject(Map.of(
                    "status", "FAILED",
                    "audioId", TaskInput.fromJsonPathAt(JSONPATH_OBJECT_KEY),
                    "error", TaskInput.fromJsonPathAt("$.Error"),
                    "message", "Sleep audio pipeline failed during processing",
                    "timestamp", TaskInput.fromJsonPathAt("$$.State.EnteredTime")
                )))
                .subject("Sleep Audio Pipeline - Processing Failed")
                .resultPath("$.snsResult")
                .build();

        // Fail state
        Fail failState = Fail.Builder.create(this, "ProcessingFailed")
                .comment("Processing failed - see error logs")
                .build();

        // Chain success path: UpdateStatus -> PublishSuccess -> Success
        updateStatusToCompleted.next(publishSuccessNotification).next(successState);

        // Chain error path: UpdateStatusToFailed -> PublishFailure -> Fail
        updateStatusToFailed.next(publishFailureNotification).next(failState);

        // Add error handling to Lambda invoke task (Issue #8)
        // Catch validation errors and route to failure path
        processAudioTask.addCatch(updateStatusToFailed, CatchProps.builder()
                .errors(List.of(Errors.ALL))
                .resultPath("$.Error")
                .build());

        // Add error handling to PutMetadata task (Issue #8)
        // Catch DynamoDB errors and route to failure path
        putMetadataTask.addCatch(updateStatusToFailed, CatchProps.builder()
                .errors(List.of(Errors.ALL))
                .resultPath("$.Error")
                .build());

        // Add error handling to Polly task (Issue #6)
        pollyTask.addCatch(updateStatusToFailed, CatchProps.builder()
                .errors(List.of(Errors.ALL))
                .resultPath("$.Error")
                .build());

        // Chain the states: PutMetadata -> ProcessAudio (Lambda) -> Polly Task -> UpdateStatusToCompleted (Issue #7, #8)
        // Complete end-to-end pipeline with error handling at each stage
        putMetadataTask.next(processAudioTask).next(pollyTask).next(updateStatusToCompleted);

        // Step Functions State Machine
        // Issue #10: X-Ray tracing enabled for end-to-end observability
        StateMachine stateMachine = StateMachine.Builder.create(this, "SleepAudioPipelineStateMachine")
                .stateMachineName("SleepAudioPipelineStateMachine")
                .stateMachineType(StateMachineType.STANDARD)
                .definitionBody(software.amazon.awscdk.services.stepfunctions.DefinitionBody.fromChainable(putMetadataTask))
                .logs(LogOptions.builder()
                        .destination(stateMachineLogGroup)
                        .level(LogLevel.ALL)
                        .includeExecutionData(true)
                        .build())
                .tracingEnabled(true)  // Issue #10: Enable X-Ray tracing
                .build();

        // Grant the state machine permissions to write to the output bucket
        outputBucket.grantWrite(stateMachine);

        // Grant the state machine permissions to write to DynamoDB table (Issue #5)
        metadataTable.grantWriteData(stateMachine);

        // Grant the state machine permissions to publish to SNS topics (Issue #6)
        completedTopic.grantPublish(stateMachine);
        failedTopic.grantPublish(stateMachine);

        // Issue #10: CloudWatch Alarms for critical failure paths and observability
        
        // Alarm for State Machine execution failures
        Alarm stateMachineFailureAlarm = Alarm.Builder.create(this, "StateMachineExecutionFailureAlarm")
                .alarmName("SleepAudioPipeline-StateMachineFailures")
                .alarmDescription("Triggers when Step Functions state machine executions fail")
                .metric(stateMachine.metricFailed(software.amazon.awscdk.services.cloudwatch.MetricOptions.builder()
                        .statistic("Sum")
                        .period(Duration.minutes(5))
                        .build()))
                .threshold(1)
                .evaluationPeriods(1)
                .comparisonOperator(ComparisonOperator.GREATER_THAN_OR_EQUAL_TO_THRESHOLD)
                .treatMissingData(TreatMissingData.NOT_BREACHING)
                .build();
        
        // Alarm for Lambda function errors
        Alarm lambdaErrorAlarm = Alarm.Builder.create(this, "LambdaErrorAlarm")
                .alarmName("SleepAudioPipeline-LambdaErrors")
                .alarmDescription("Triggers when Lambda function encounters errors")
                .metric(audioProcessorFunction.metricErrors(software.amazon.awscdk.services.cloudwatch.MetricOptions.builder()
                        .statistic("Sum")
                        .period(Duration.minutes(5))
                        .build()))
                .threshold(1)
                .evaluationPeriods(1)
                .comparisonOperator(ComparisonOperator.GREATER_THAN_OR_EQUAL_TO_THRESHOLD)
                .treatMissingData(TreatMissingData.NOT_BREACHING)
                .build();
        
        // Alarm for Lambda function throttles (additional observability)
        Alarm lambdaThrottleAlarm = Alarm.Builder.create(this, "LambdaThrottleAlarm")
                .alarmName("SleepAudioPipeline-LambdaThrottles")
                .alarmDescription("Triggers when Lambda function is throttled")
                .metric(audioProcessorFunction.metricThrottles(software.amazon.awscdk.services.cloudwatch.MetricOptions.builder()
                        .statistic("Sum")
                        .period(Duration.minutes(5))
                        .build()))
                .threshold(5)
                .evaluationPeriods(1)
                .comparisonOperator(ComparisonOperator.GREATER_THAN_OR_EQUAL_TO_THRESHOLD)
                .treatMissingData(TreatMissingData.NOT_BREACHING)
                .build();

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
