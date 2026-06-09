package com.myorg;

import software.constructs.Construct;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.pipelines.CodePipeline;
import software.amazon.awscdk.pipelines.CodePipelineSource;
import software.amazon.awscdk.pipelines.ShellStep;
import software.amazon.awscdk.pipelines.StageDeployment;
import software.amazon.awscdk.Stage;
import software.amazon.awscdk.StageProps;

import java.util.List;

/**
 * CDK Pipeline Stack for deploying the Sleep Audio Pipeline application across environments.
 * (Issue #9: Pipeline Testing, Refinement & Deployment Preparation)
 * 
 * This stack creates a self-mutating CDK Pipeline that:
 * - Pulls source from GitHub
 * - Synthesizes the CDK application
 * - Deploys to dev, stage, and prod environments in sequence
 * 
 * This is a skeleton implementation to satisfy initial tests.
 * Future enhancements will add:
 * - Manual approval steps for prod
 * - Integration tests between stages
 * - CloudWatch alarms as deployment gates
 */
public class PipelineStack extends Stack {
    
    public PipelineStack(final Construct scope, final String id) {
        this(scope, id, null);
    }
    
    public PipelineStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);
        
        // Issue #9: Basic CDK Pipeline skeleton
        // Source: GitHub repository (placeholder - will be configured via context or environment)
        // In production, use GitHubSource with OAuth token from Secrets Manager
        CodePipelineSource source = CodePipelineSource.gitHub(
            "obstreperous-ai/cdk-sleep-java-copilot",  // repository
            "main"  // branch
        );
        
        // Synth step: Build and synthesize the CDK application
        ShellStep synthStep = ShellStep.Builder.create("Synth")
            .input(source)
            .commands(List.of(
                "npm install -g aws-cdk",
                "mvn clean compile",
                "npx cdk synth"
            ))
            .primaryOutputDirectory("cdk.out")
            .build();
        
        // Create the pipeline
        CodePipeline pipeline = CodePipeline.Builder.create(this, "SleepAudioPipeline")
            .pipelineName("SleepAudioPipeline")
            .synth(synthStep)
            .crossAccountKeys(true)  // Enable cross-account deployments
            .build();
        
        // Issue #9: Add deployment stages
        // Dev environment - automatic deployment
        pipeline.addStage(new ApplicationStage(this, "Dev", StageProps.builder()
            .env(Environment.builder()
                .account(getAccountFromContext("dev"))
                .region(getRegionFromContext("dev"))
                .build())
            .build(), "dev"));
        
        // Stage environment - automatic deployment
        pipeline.addStage(new ApplicationStage(this, "Stage", StageProps.builder()
            .env(Environment.builder()
                .account(getAccountFromContext("stage"))
                .region(getRegionFromContext("stage"))
                .build())
            .build(), "stage"));
        
        // Prod environment - automatic deployment (manual approval can be added later)
        pipeline.addStage(new ApplicationStage(this, "Prod", StageProps.builder()
            .env(Environment.builder()
                .account(getAccountFromContext("prod"))
                .region(getRegionFromContext("prod"))
                .build())
            .build(), "prod"));
    }
    
    /**
     * Get AWS account ID from context for the given environment.
     * Defaults to CDK_DEFAULT_ACCOUNT if not specified.
     */
    private String getAccountFromContext(String environment) {
        String contextKey = environment + "-account";
        String account = (String) this.getNode().tryGetContext(contextKey);
        if (account == null || account.isEmpty()) {
            // Fallback to CDK_DEFAULT_ACCOUNT
            return System.getenv("CDK_DEFAULT_ACCOUNT");
        }
        return account;
    }
    
    /**
     * Get AWS region from context for the given environment.
     * Defaults to CDK_DEFAULT_REGION if not specified.
     */
    private String getRegionFromContext(String environment) {
        String contextKey = environment + "-region";
        String region = (String) this.getNode().tryGetContext(contextKey);
        if (region == null || region.isEmpty()) {
            // Fallback to CDK_DEFAULT_REGION
            return System.getenv("CDK_DEFAULT_REGION");
        }
        return region;
    }
    
    /**
     * Application Stage that deploys the CdkBaseStack to a specific environment.
     * This stage is deployed by the pipeline to dev, stage, and prod.
     */
    public static class ApplicationStage extends Stage {
        public ApplicationStage(final Construct scope, final String id, 
                               final StageProps props, final String environment) {
            super(scope, id, props);
            
            // Set environment context for the stack
            this.getNode().setContext("environment", environment);
            
            // Create the CdkBaseStack with environment-specific configuration
            new CdkBaseStack(this, "CdkBaseStack-" + environment, StackProps.builder()
                .env(props.getEnv())
                .build());
        }
    }
}
