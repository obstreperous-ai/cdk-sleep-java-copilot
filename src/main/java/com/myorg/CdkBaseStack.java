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
import software.amazon.awscdk.services.stepfunctions.StateMachine;
import software.amazon.awscdk.services.stepfunctions.StateMachineType;
import software.amazon.awscdk.services.stepfunctions.LogLevel;
import software.amazon.awscdk.services.stepfunctions.LogOptions;
import software.amazon.awscdk.services.stepfunctions.tasks.CallAwsService;
import software.amazon.awscdk.services.stepfunctions.Pass;
import software.amazon.awscdk.services.stepfunctions.Succeed;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.RetentionDays;

import java.util.List;
import java.util.Map;

public class CdkBaseStack extends Stack {
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

        // CloudWatch Log Group for Step Functions state machine
        LogGroup stateMachineLogGroup = LogGroup.Builder.create(this, "StateMachineLogGroup")
                .retention(RetentionDays.ONE_WEEK)
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();

        // Polly Task - Minimal integration using CallAwsService
        // This is a placeholder that will invoke Polly StartSpeechSynthesisTask
        CallAwsService pollyTask = CallAwsService.Builder.create(this, "PollyTask")
                .service("polly")
                .action("startSpeechSynthesisTask")
                .parameters(Map.of(
                    "Text", "Placeholder text for sleep audio narration",
                    "OutputFormat", "mp3",
                    "VoiceId", "Joanna",
                    "OutputS3BucketName", outputBucket.getBucketName()
                ))
                .iamResources(List.of("*"))
                .resultPath("$.pollyResult")
                .build();

        // Success state
        Succeed successState = Succeed.Builder.create(this, "Success")
                .comment("Processing completed successfully")
                .build();

        // Chain the states: Polly Task -> Success
        pollyTask.next(successState);

        // Step Functions State Machine
        StateMachine stateMachine = StateMachine.Builder.create(this, "SleepAudioPipelineStateMachine")
                .stateMachineName("SleepAudioPipelineStateMachine")
                .stateMachineType(StateMachineType.STANDARD)
                .definitionBody(software.amazon.awscdk.services.stepfunctions.DefinitionBody.fromChainable(pollyTask))
                .logs(LogOptions.builder()
                        .destination(stateMachineLogGroup)
                        .level(LogLevel.ALL)
                        .includeExecutionData(true)
                        .build())
                .build();

        // Grant the state machine permissions to write to the output bucket
        outputBucket.grantWrite(stateMachine);

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

        // Add Step Functions state machine as target
        eventRule.addTarget(SfnStateMachine.Builder.create(stateMachine).build());
    }
}
