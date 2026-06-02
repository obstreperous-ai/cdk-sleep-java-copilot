package com.myorg;

import org.junit.jupiter.api.Test;
import software.amazon.awscdk.App;
import software.amazon.awscdk.assertions.Match;
import software.amazon.awscdk.assertions.Template;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CdkBaseTest {
    
    private Template getTestTemplate() {
        App app = new App();
        CdkBaseStack stack = new CdkBaseStack(app, "test");
        return Template.fromStack(stack);
    }

    @Test
    void createsInputS3Bucket() {
        Template template = getTestTemplate();
        
        // Verify Input S3 Bucket exists with encryption, versioning, and public access blocked
        template.hasResourceProperties("AWS::S3::Bucket", Match.objectLike(Map.of(
            "BucketEncryption", Match.objectLike(Map.of(
                "ServerSideEncryptionConfiguration", List.of(
                    Map.of(
                        "ServerSideEncryptionByDefault", Map.of(
                            "SSEAlgorithm", "AES256"
                        )
                    )
                )
            )),
            "VersioningConfiguration", Map.of(
                "Status", "Enabled"
            ),
            "PublicAccessBlockConfiguration", Map.of(
                "BlockPublicAcls", true,
                "BlockPublicPolicy", true,
                "IgnorePublicAcls", true,
                "RestrictPublicBuckets", true
            )
        )));
        
        // Verify EventBridge notifications are configured via custom resource
        template.hasResourceProperties("Custom::S3BucketNotifications", Match.objectLike(Map.of(
            "NotificationConfiguration", Match.objectLike(Map.of(
                "EventBridgeConfiguration", Match.objectLike(Map.of())
            ))
        )));
    }

    @Test
    void createsOutputS3Bucket() {
        Template template = getTestTemplate();
        
        // Verify Output S3 Bucket exists with encryption and versioning
        template.hasResourceProperties("AWS::S3::Bucket", Match.objectLike(Map.of(
            "BucketEncryption", Match.objectLike(Map.of(
                "ServerSideEncryptionConfiguration", List.of(
                    Map.of(
                        "ServerSideEncryptionByDefault", Map.of(
                            "SSEAlgorithm", "AES256"
                        )
                    )
                )
            )),
            "VersioningConfiguration", Map.of(
                "Status", "Enabled"
            ),
            "PublicAccessBlockConfiguration", Map.of(
                "BlockPublicAcls", true,
                "BlockPublicPolicy", true,
                "IgnorePublicAcls", true,
                "RestrictPublicBuckets", true
            )
        )));
    }

    @Test
    void createsBothS3Buckets() {
        Template template = getTestTemplate();
        
        // Verify exactly 2 S3 buckets exist
        assertEquals(2, template.findResources("AWS::S3::Bucket").size());
    }

    @Test
    void createsEventBridgeRule() {
        Template template = getTestTemplate();
        
        // Verify EventBridge Rule exists with correct event pattern
        template.hasResourceProperties("AWS::Events::Rule", Match.objectLike(Map.of(
            "State", "ENABLED",
            "EventPattern", Match.objectLike(Map.of(
                "source", List.of("aws.s3"),
                "detail-type", List.of("Object Created")
            ))
        )));
    }

    @Test
    void eventBridgeRuleHasTarget() {
        Template template = getTestTemplate();
        
        // Verify EventBridge Rule has at least one target
        template.hasResourceProperties("AWS::Events::Rule", Match.objectLike(Map.of(
            "Targets", Match.anyValue()
        )));
    }

    @Test
    void hasPlaceholderQueueForEventBridgeTarget() {
        App app = new App();
        CdkBaseStack stack = new CdkBaseStack(app, "test");

        Template template = Template.fromStack(stack);

        // Placeholder queue exists (will be replaced with Step Functions later)
        assertEquals(1, template.findResources("AWS::SQS::Queue").size());
    }
}
