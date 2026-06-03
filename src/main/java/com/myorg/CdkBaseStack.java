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
import software.amazon.awscdk.services.events.targets.SqsQueue;
import software.amazon.awscdk.services.sqs.Queue;

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

        // Placeholder target for EventBridge rule (will be replaced with Step Functions later)
        Queue placeholderQueue = Queue.Builder.create(this, "PlaceholderQueue")
                .build();

        // EventBridge Rule - triggers on S3 Object Created events from input bucket
        Rule.Builder.create(this, "S3ObjectCreatedRule")
                .eventPattern(EventPattern.builder()
                        .source(List.of("aws.s3"))
                        .detailType(List.of("Object Created"))
                        .detail(Map.of(
                                "bucket", Map.of(
                                        "name", List.of(inputBucket.getBucketName())
                                )
                        ))
                        .build())
                .targets(List.of(new SqsQueue(placeholderQueue)))
                .build();
    }
}
