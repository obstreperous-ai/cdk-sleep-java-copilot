# Meta-Prompts & Reusable Patterns

> **Reusable patterns, agent guidelines, and meta-prompts extracted from the Sleep Audio Pipeline project**  
> Use these patterns when building future agentic TDD IaC projects with AWS CDK

## Table of Contents

- [Overview](#overview)
- [Core Principles](#core-principles)
- [Agent Personas & Guidelines](#agent-personas--guidelines)
- [TDD Workflow Patterns](#tdd-workflow-patterns)
- [Issue-Driven Development](#issue-driven-development)
- [Documentation Patterns](#documentation-patterns)
- [Testing Patterns](#testing-patterns)
- [CDK Infrastructure Patterns](#cdk-infrastructure-patterns)
- [Multi-Environment Patterns](#multi-environment-patterns)
- [Observability Patterns](#observability-patterns)
- [Prompt Templates](#prompt-templates)

---

## Overview

This document captures **reusable meta-prompting patterns** learned from building the Sleep Audio Pipeline project following strict Test-Driven Development (TDD) methodology with AWS CDK and Java 17. These patterns are designed to be extracted and applied to future agentic Infrastructure as Code (IaC) projects.

**Key Context:**  
The Sleep Audio Pipeline was built entirely issue-by-issue (#2–#12) with AI agents following strict TDD discipline. Every feature was test-first, every change was validated, and all documentation remained synchronized with the code. These patterns codify what made that approach successful.

---

## Core Principles

### 1. Test-First, Always

**Pattern:**  
```
BEFORE any implementation:
1. Write a failing test that captures the desired behavior
2. Run the test and confirm it fails for the right reason
3. Make the SMALLEST possible change to pass the test
4. Run the test again and confirm it passes
5. Refactor if needed while keeping tests green
```

**Agent Prompt:**
```
You are a Senior TDD Specialist. For every requested feature:
1. Start by writing a failing test
2. Show me the test output proving it fails
3. Implement the minimal change to pass the test
4. Show me the test output proving it passes
5. Only then consider the feature complete

Never implement before testing. Never skip validation steps.
```

### 2. Architecture as Single Source of Truth

**Pattern:**  
Maintain `ARCHITECTURE.md` as the definitive design document. Before implementing any feature:
- Read ARCHITECTURE.md first
- Align your implementation with the documented design
- Update ARCHITECTURE.md (including diagrams) if the design evolves
- Keep Mermaid diagrams synchronized with code changes

**Agent Prompt:**
```
The ARCHITECTURE.md file is the single source of truth. Before starting any work:
1. Read ARCHITECTURE.md to understand the current design
2. Ensure your changes align with the documented architecture
3. If your changes affect the architecture, update ARCHITECTURE.md AND its Mermaid diagrams
4. Verify all references and links remain valid
```

### 3. Issue-Driven, Incremental Delivery

**Pattern:**  
- Break large features into small, testable issues
- Each issue should be independently deliverable
- Complete one issue fully before starting the next
- Each issue should add observable, tested value

**Agent Prompt:**
```
Work on ONE issue at a time. For this issue:
1. Understand the specific requirements
2. Identify the minimal scope needed to satisfy the issue
3. Implement with tests
4. Validate completely
5. Document what was done
6. Close the issue before moving to the next

Do not speculate or implement features from future issues.
```

### 4. Observable and Traceable

**Pattern:**  
Every component must be observable:
- Structured JSON logging with correlation IDs
- X-Ray tracing for distributed request tracking
- CloudWatch metrics for operational visibility
- CloudWatch Alarms for critical failure paths

---

## Agent Personas & Guidelines

### AWS CDK Java TDD Specialist

**Persona Definition:**
```
You are a Senior AWS CDK Java TDD Specialist with expertise in:
- AWS Cloud Development Kit (CDK) with Java
- Strict Test-Driven Development (TDD) methodology
- Event-driven serverless architectures
- Infrastructure as Code best practices
- AWS service integrations

Key Characteristics:
- Explicit and verbose in explanations
- Always writes tests before implementation
- Maintains perfect sync with ARCHITECTURE.md
- Follows conventional commit messages
- Focuses on minimal, surgical changes
```

**Activation Prompt:**
```
You are a Senior AWS CDK Java TDD Specialist. 

Before starting:
1. Read ARCHITECTURE.md - it is the single source of truth
2. Review existing tests to understand patterns
3. Check CONTRIBUTING.md for workflow requirements

For every change:
- Write tests first, implementation second
- Keep ARCHITECTURE.md synchronized with code
- Run `mvn test` after each change
- Run `cdk synth` before considering work complete
- Use conventional commits

Be explicit and verbose. Ask questions if requirements are unclear.
```

### Infrastructure Testing Specialist

**Persona Definition:**
```
You are an Infrastructure Testing Specialist focused on:
- CDK snapshot testing and assertions
- AWS CloudFormation template validation
- IAM permission verification
- Resource property validation
- Integration testing across AWS services

Expertise:
- JUnit 5 with CDK assertions
- CloudFormation template structure
- AWS service quotas and limits
- Security best practices
```

---

## TDD Workflow Patterns

### Pattern: Red-Green-Refactor Cycle

**Step-by-step:**
```
1. RED: Write a failing test
   - Clearly define expected behavior
   - Test should fail for the right reason
   - Run: mvn test -Dtest=<TestClass>

2. GREEN: Make it pass
   - Implement the MINIMAL code to pass the test
   - Don't add extra features
   - Run: mvn test -Dtest=<TestClass>

3. REFACTOR: Clean up (optional)
   - Improve code structure
   - Remove duplication
   - Keep tests green
   - Run: mvn test

4. VALIDATE: Full validation
   - Run: mvn test (all tests)
   - Run: cdk synth (ensure CloudFormation synthesis works)
```

### Pattern: Test Organization

**Structure:**
```
src/test/java/com/myorg/
├── CdkBaseTest.java           # Infrastructure tests
│   ├── Resource creation tests
│   ├── Permission tests
│   ├── Integration tests
│   └── Multi-environment tests
└── lambda/
    └── SleepAudioProcessorTest.java  # Lambda function tests
        ├── Input validation tests
        ├── Business logic tests
        └── Error handling tests
```

**Test Naming Convention:**
```java
@Test
public void testComponentName_Scenario_ExpectedBehavior() {
    // Example: testS3Bucket_WhenCreated_HasEncryptionEnabled
}
```

### Pattern: Test Isolation

**Guidelines:**
- Each test should be independent
- Use `@BeforeEach` for test setup
- Create fresh CDK App and Stack per test
- No shared mutable state between tests

**Example:**
```java
@BeforeEach
public void setUp() {
    app = new App();
    stack = new CdkBaseStack(app, "TestStack");
    template = Template.fromStack(stack);
}
```

---

## Issue-Driven Development

### Pattern: Issue Template Structure

**Effective Issue Structure:**
```markdown
# [Issue Number] Title: Clear, Actionable Goal

## Goal
Brief description of what this issue accomplishes

## Strict Discipline (must follow)
- Read ARCHITECTURE.md first
- Write tests before implementation
- Run mvn test after each change
- Update ARCHITECTURE.md if design changes
- Use conventional commits

## Requirements
1. Specific requirement 1
2. Specific requirement 2
3. ...

## Tasks (in strict order)
1. [ ] Task 1
2. [ ] Task 2
3. [ ] Task 3

## Success Criteria
- [ ] All tests pass (mvn test)
- [ ] CDK synth succeeds
- [ ] ARCHITECTURE.md is updated
- [ ] Documentation is complete

## Next Issue
[Issue Number] Title (when complete)
```

### Pattern: Progressive Elaboration

**Approach:**  
Start with foundational infrastructure, then layer on features:

1. **Foundation** (Issue #2): Core resources (S3 buckets)
2. **Events** (Issue #3): Event detection and routing (EventBridge)
3. **Orchestration** (Issue #4): Workflow coordination (Step Functions)
4. **State** (Issue #5): Data persistence (DynamoDB)
5. **Notifications** (Issue #6): Success/failure paths (SNS)
6. **Logic** (Issue #7): Business logic skeleton (Lambda)
7. **Validation** (Issue #8): Input validation and error handling
8. **Environments** (Issue #9): Multi-environment support
9. **Observability** (Issue #10): Monitoring and resilience
10. **Implementation** (Issue #11): Full feature implementation
11. **Validation** (Issue #12): End-to-end testing and documentation

---

## Documentation Patterns

### Pattern: README.md Structure

**Comprehensive README Template:**
```markdown
# Project Title

> Tagline describing the project in one sentence

## Overview
[High-level description, key features]

## Architecture
[Brief architecture summary with link to ARCHITECTURE.md]

## Prerequisites
[Required tools and versions]

## Getting Started
[Quick start guide with code examples]

## Project Structure
[Directory tree showing organization]

## Development Guidelines
[TDD rules, workflows, best practices]

## Testing
[How to run tests, test coverage details]

## Environment Configuration
[Multi-environment setup and deployment]

## Observability
[Monitoring, logging, tracing]

## Cost Considerations
[Pricing model and optimization tips]

## Security
[Security features and best practices]

## CI/CD
[Continuous integration and deployment]

## Troubleshooting
[Common issues and solutions]

## Related Documentation
[Links to other docs]

## License
[License information]
```

### Pattern: ARCHITECTURE.md Structure

**Architecture Document Template:**
```markdown
# Architecture

> Status: [Current status and completion notes]

## 1. High-Level Overview
[System description, design principles]

## 2. Data Flow
[Step-by-step data flow through the system]

## 3. Architecture Diagram
```mermaid
[Comprehensive Mermaid diagram]
```

## 4. Key AWS Services and Rationale
[Service choices with justifications]

## 5. Security
[Security controls and practices]

## 6. Observability
[Monitoring, logging, tracing approach]

## 7. Cost Considerations
[Cost model and optimization]

## 8. Multi-Environment Support
[Environment strategy]

## 9. Future Extensibility
[Evolution and enhancement paths]
```

### Pattern: Living Documentation

**Synchronization Rules:**
- Update documentation in the same commit as code changes
- Keep Mermaid diagrams synchronized with architecture
- Document decisions and rationale, not just implementation
- Link between related documents
- Maintain a changelog or summary of major milestones

---

## Testing Patterns

### Pattern: CDK Infrastructure Testing

**Types of Tests:**

1. **Resource Creation Tests**
   ```java
   @Test
   public void testS3InputBucket_Created() {
       template.resourceCountIs("AWS::S3::Bucket", 2);
   }
   ```

2. **Resource Properties Tests**
   ```java
   @Test
   public void testS3Bucket_HasEncryption() {
       template.hasResourceProperties("AWS::S3::Bucket", Map.of(
           "BucketEncryption", Map.of(
               "ServerSideEncryptionConfiguration", Arrays.asList(
                   Map.of("ServerSideEncryptionByDefault", 
                       Map.of("SSEAlgorithm", "AES256"))
               )
           )
       ));
   }
   ```

3. **Permission Tests**
   ```java
   @Test
   public void testLambda_HasS3ReadPermission() {
       template.hasResourceProperties("AWS::IAM::Policy", Map.of(
           "PolicyDocument", Map.of(
               "Statement", Match.arrayWith(Arrays.asList(
                   Match.objectLike(Map.of(
                       "Action", Arrays.asList("s3:GetObject", "s3:ListBucket"),
                       "Effect", "Allow"
                   ))
               ))
           )
       ));
   }
   ```

4. **Integration Tests**
   ```java
   @Test
   public void testEventBridgeRule_TriggersStateMachine() {
       template.hasResourceProperties("AWS::Events::Rule", Map.of(
           "Targets", Match.arrayWith(Arrays.asList(
               Match.objectLike(Map.of("Arn", Match.anyValue()))
           ))
       ));
   }
   ```

### Pattern: Lambda Function Testing

**Test Structure:**
```java
// Input validation
@Test
public void testHandleRequest_MissingBucket_ThrowsException() { }

// Business logic
@Test
public void testHandleRequest_ValidInput_ProcessesAudio() { }

// Error handling
@Test
public void testHandleRequest_InvalidFormat_ThrowsException() { }
```

### Pattern: Multi-Environment Testing

**Test environments:**
```java
@Test
public void testDevEnvironment_HasDestroyRemovalPolicy() {
    App app = new App();
    Map<String, Object> context = Map.of("environment", "dev");
    CdkBaseStack stack = new CdkBaseStack(app, "DevStack", context);
    // Verify DESTROY removal policy
}

@Test
public void testProdEnvironment_HasRetainRemovalPolicy() {
    App app = new App();
    Map<String, Object> context = Map.of("environment", "prod");
    CdkBaseStack stack = new CdkBaseStack(app, "ProdStack", context);
    // Verify RETAIN removal policy
}
```

---

## CDK Infrastructure Patterns

### Pattern: Environment-Based Configuration

**Context-Driven Environments:**
```java
public CdkBaseStack(final Construct scope, final String id, 
                    final StackProps props) {
    super(scope, id, props);
    
    String environment = (String) this.getNode().tryGetContext("environment");
    if (environment == null) {
        environment = "dev";
    }
    
    RemovalPolicy removalPolicy = environment.equals("prod") 
        ? RemovalPolicy.RETAIN 
        : RemovalPolicy.DESTROY;
    
    // Apply environment-specific configuration
}
```

**Usage:**
```bash
cdk deploy -c environment=dev    # Development
cdk deploy -c environment=stage  # Staging
cdk deploy -c environment=prod   # Production
```

### Pattern: Tagging Strategy

**Consistent Tagging:**
```java
Tags.of(this).add("Environment", environment);
Tags.of(this).add("Project", "SleepAudioPipeline");
Tags.of(this).add("ManagedBy", "CDK");
```

### Pattern: Naming Conventions

**Resource Naming:**
```java
String resourceName = "SleepAudio" + capitalizeFirstLetter(resourceType) + 
                      "-" + environment;
```

### Pattern: Service Integration with Step Functions

**CallAwsService for Direct Integration:**
```java
CallAwsService pollyTask = CallAwsService.Builder.create(this, "PollyNarration")
    .service("polly")
    .action("startSpeechSynthesisTask")
    .iamResources(List.of("*"))
    .parameters(Map.of(
        "Text", JsonPath.stringAt("$.narrationText"),
        "OutputFormat", "mp3",
        "VoiceId", "Joanna"
    ))
    .resultPath("$.pollyResult")
    .build();
```

---

## Multi-Environment Patterns

### Pattern: Environment Progression

**Deployment Pipeline:**
```
Developer → Git Push → CI Tests → Dev (auto) → Stage (manual) → Prod (manual)
```

**Environment Policies:**

| Environment | Removal Policy | Use Case | Auto-Deploy |
|------------|---------------|----------|-------------|
| dev | DESTROY | Development | Yes |
| stage | DESTROY | Pre-prod testing | No |
| prod | RETAIN | Production | No |

### Pattern: Environment-Specific Resources

**Conditional Resource Creation:**
```java
if (environment.equals("prod")) {
    // Add production-specific resources
    // e.g., cross-region replication, enhanced monitoring
}
```

---

## Observability Patterns

### Pattern: Structured JSON Logging

**Lambda Logging:**
```java
private void logInfo(String message, Map<String, Object> context) {
    Map<String, Object> logEntry = new HashMap<>();
    logEntry.put("timestamp", Instant.now().toString());
    logEntry.put("level", "INFO");
    logEntry.put("message", message);
    logEntry.put("context", context);
    System.out.println(new Gson().toJson(logEntry));
}
```

### Pattern: X-Ray Tracing

**Enable Tracing:**
```java
// Lambda
Function lambdaFunction = Function.Builder.create(this, "MyFunction")
    .tracing(Tracing.ACTIVE)
    // ... other properties
    .build();

// Step Functions
StateMachine stateMachine = StateMachine.Builder.create(this, "MyStateMachine")
    .tracingEnabled(true)
    // ... other properties
    .build();
```

### Pattern: CloudWatch Alarms

**Critical Failure Alarms:**
```java
Alarm stateMachineFailureAlarm = Alarm.Builder.create(this, "StateMachineFailures")
    .metric(stateMachine.metricFailed())
    .threshold(1)
    .evaluationPeriods(1)
    .comparisonOperator(ComparisonOperator.GREATER_THAN_OR_EQUAL_TO_THRESHOLD)
    .alarmDescription("Alert when State Machine execution fails")
    .build();
```

### Pattern: Retry Policies with Exponential Backoff

**Resilient Tasks:**
```java
LambdaInvoke lambdaTask = LambdaInvoke.Builder.create(this, "ProcessAudio")
    .lambdaFunction(audioProcessorFunction)
    .retryOnServiceExceptions(true)
    .build();

lambdaTask.addRetry(RetryProps.builder()
    .errors(Arrays.asList("States.TaskFailed", "States.Timeout"))
    .interval(Duration.seconds(2))
    .maxAttempts(3)
    .backoffRate(2.0)
    .build());
```

---

## Prompt Templates

### Template: New Feature Implementation

```
I need to implement [FEATURE] in the Sleep Audio Pipeline.

Requirements:
- [Requirement 1]
- [Requirement 2]

Please follow these steps:
1. Review ARCHITECTURE.md to understand the current design
2. Write failing tests for the new feature
3. Implement the minimal code to pass the tests
4. Update ARCHITECTURE.md if the design changes
5. Run mvn test to verify all tests pass
6. Run cdk synth to verify CloudFormation synthesis
7. Document the changes in comments

Use strict TDD methodology throughout.
```

### Template: Bug Fix

```
There is a bug in [COMPONENT]: [DESCRIPTION]

To fix:
1. First, write a failing test that reproduces the bug
2. Confirm the test fails
3. Fix the bug with minimal code changes
4. Confirm the test passes
5. Run the full test suite (mvn test)
6. Verify with cdk synth

Do not fix other issues - only address this specific bug.
```

### Template: Add Tests for Existing Code

```
I need tests for [COMPONENT] that currently lacks test coverage.

The component should:
- [Behavior 1]
- [Behavior 2]

Please:
1. Review the existing implementation
2. Write comprehensive tests covering all behaviors
3. Verify all tests pass (mvn test)
4. Document any gaps or issues found

Follow the existing test patterns in the codebase.
```

### Template: Multi-Environment Deployment

```
I need to deploy [STACK] to [ENVIRONMENT].

Environment: [dev/stage/prod]

Steps:
1. Review environment-specific configuration
2. Run tests: mvn test
3. Synthesize CloudFormation: cdk synth -c environment=[ENV]
4. Deploy: cdk deploy -c environment=[ENV]
5. Verify deployment in AWS Console
6. Test the deployed resources

Document any environment-specific considerations.
```

### Template: Architecture Review

```
Review the current architecture for [COMPONENT/FEATURE].

Please:
1. Read ARCHITECTURE.md
2. Review the implementation in [FILE(S)]
3. Verify alignment between documentation and code
4. Check for:
   - Security best practices
   - Cost optimization opportunities
   - Observability gaps
   - Error handling completeness
5. Suggest improvements if any
6. Update ARCHITECTURE.md if needed

Be thorough and explicit in your review.
```

---

## Reuse Guidelines

### How to Apply These Patterns

1. **Starting a New CDK Project:**
   - Copy the issue template structure
   - Establish ARCHITECTURE.md as source of truth
   - Set up agent personas with the provided prompts
   - Begin with foundation issues (S3, EventBridge, etc.)

2. **Adapting to Different Stacks:**
   - Replace AWS services as needed (e.g., Kinesis instead of S3)
   - Keep the TDD workflow patterns intact
   - Maintain the documentation structure
   - Preserve observability patterns

3. **Working with Different Languages:**
   - Patterns apply to TypeScript, Python, etc.
   - Adjust syntax but keep TDD cycle
   - Use language-specific testing frameworks
   - Maintain same documentation rigor

4. **Scaling to Larger Teams:**
   - Assign issue-based ownership
   - Enforce TDD discipline via code review
   - Use agent personas for consistency
   - Maintain centralized ARCHITECTURE.md

### Success Metrics

Track these metrics to ensure pattern effectiveness:
- **Test Coverage**: >80% for all components
- **TDD Adherence**: 100% of features test-first
- **Documentation Sync**: ARCHITECTURE.md updated in same commit as code
- **CI Success Rate**: >95% green builds
- **Issue Velocity**: Consistent completion of small, incremental issues

---

## License

These patterns are extracted from the Sleep Audio Pipeline project and are provided as-is for reuse in other projects. Adapt them to your specific needs and context.

---

**Built with ❤️ through strict TDD methodology and issue-driven development**
