# Sleep Audio Pipeline (CDK Java)

[![Build Status](https://img.shields.io/github/actions/workflow/status/obstreperous-ai/cdk-sleep-java-copilot/ci.yml?branch=main&label=CI&logo=github)](https://github.com/obstreperous-ai/cdk-sleep-java-copilot/actions)
[![Tests](https://img.shields.io/badge/tests-79%20passing-brightgreen?logo=junit5)](https://github.com/obstreperous-ai/cdk-sleep-java-copilot)
[![AWS CDK](https://img.shields.io/badge/AWS%20CDK-2.255.0%2B-orange?logo=amazonaws)](https://aws.amazon.com/cdk/)
[![Java](https://img.shields.io/badge/Java-17-blue?logo=openjdk)](https://openjdk.org/)
[![License](https://img.shields.io/badge/license-MIT-green)](LICENSE)
[![TDD](https://img.shields.io/badge/methodology-TDD-purple)](#strict-tdd-rules)

> **Event-Driven Serverless Audio Processing Pipeline on AWS**  
> Built with AWS CDK (Java), strict TDD methodology, and infrastructure as code best practices.  
> **A reference implementation and pattern library for agentic IaC development.**

## Table of Contents

- [Overview](#overview)
- [Experiment Methodology](#experiment-methodology)
- [Architecture](#architecture)
- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
- [Project Structure](#project-structure)
- [Development Guidelines](#development-guidelines)
- [Strict TDD Rules](#strict-tdd-rules)
- [Testing](#testing)
- [Environment Configuration](#environment-configuration)
- [Observability](#observability)
- [Supported Audio Formats](#supported-audio-formats)
- [Useful Commands](#useful-commands)
- [Cost Considerations](#cost-considerations)
- [Security](#security)
- [CI/CD](#cicd)
- [Troubleshooting](#troubleshooting)
- [Related Documentation](#related-documentation)
- [Reusable Patterns](#reusable-patterns)
- [Acknowledgments](#acknowledgments)
- [License](#license)

## Overview

The **Sleep Audio Pipeline** is a fully serverless, event-driven system for processing and enhancing audio files into soothing sleep audio content. The pipeline leverages AWS services including S3, Lambda, Step Functions, DynamoDB, SNS, and Polly to automatically ingest, validate, process, and distribute audio files.

### Key Features

- **Event-Driven Architecture**: S3 uploads trigger EventBridge rules that orchestrate processing workflows
- **Serverless Processing**: No servers to manage - auto-scales from zero to production workloads
- **Input Validation**: Comprehensive validation of file formats (.mp3, .wav, .m4a) and required metadata
- **Audio Processing**: Download, process, enhance, and upload audio with unique timestamped outputs
- **Metadata Tracking**: DynamoDB tracks complete processing lifecycle with status, timestamps, and output locations
- **Error Handling**: Comprehensive error catching with automatic retries and exponential backoff
- **Observability**: X-Ray tracing, structured JSON logging, CloudWatch metrics and alarms
- **Multi-Environment**: Deploy to dev, stage, and prod with environment-specific policies
- **Security**: Encryption at rest and in transit, least-privilege IAM, private S3 buckets

## Experiment Methodology

This project serves as a **reference implementation and pattern library** for building cloud infrastructure with AI agents following strict Test-Driven Development (TDD) principles.

### Pure Issue-Driven Development

The entire project was built incrementally through **Issues #2–#12**, with each issue:
- Defining clear, testable requirements
- Following strict TDD discipline (test first, then implement)
- Delivering independently verifiable value
- Updating documentation in sync with code changes

**Key Insights:**
- 📋 **Issue-by-issue delivery** prevents scope creep and ensures focus
- ✅ **Test-first approach** catches design issues before implementation
- 📚 **Living documentation** (ARCHITECTURE.md as source of truth) keeps team aligned
- 🤖 **AI agents thrive** with clear constraints and validation steps
- 🔁 **Incremental refinement** builds confidence and reduces risk

### Reusable Meta-Prompts

This project has extracted **reusable patterns** for agentic TDD IaC development:
- Agent personas and activation prompts
- TDD workflow templates
- Documentation structure patterns
- Testing patterns for CDK infrastructure
- Multi-environment deployment strategies

See [META-PROMPTS.md](META-PROMPTS.md) for the complete pattern library.

### Why This Matters

Traditional IaC development often suffers from:
- ❌ Untested infrastructure code leading to deployment failures
- ❌ Documentation drift from implementation reality
- ❌ Large, risky changes with unclear validation
- ❌ Inconsistent patterns across team members

**This methodology addresses these with:**
- ✅ **Test coverage** (79 tests, 100% passing)
- ✅ **Synchronized docs** (ARCHITECTURE.md updated with every change)
- ✅ **Small, validated increments** (each issue fully tested)
- ✅ **Reusable patterns** (codified in META-PROMPTS.md)

Use this project as a **template for your own agentic IaC initiatives**.

## Architecture

The pipeline follows this flow:

1. **Upload** → User uploads audio to S3 Input Bucket
2. **Event Detection** → S3 emits Object Created event to EventBridge
3. **Orchestration** → EventBridge rule triggers Step Functions state machine
4. **Metadata Init** → DynamoDB record created with status=PROCESSING
5. **Validation** → Lambda validates required fields and file extensions
6. **Audio Processing** → Lambda downloads, processes, and uploads enhanced audio
7. **Metadata Update** → DynamoDB updated with output location and status=COMPLETED
8. **Polly Narration** → Optional text-to-speech narration generated
9. **Notification** → SNS publishes success/failure notifications
10. **Observability** → CloudWatch captures logs, metrics, traces

For detailed architecture diagrams and component descriptions, see [ARCHITECTURE.md](ARCHITECTURE.md).

## Prerequisites

- **Java 17** or later
- **Maven 3.8+**
- **Node.js 18+** (for AWS CDK CLI)
- **AWS CLI** configured with appropriate credentials
- **AWS CDK CLI** (`npm install -g aws-cdk`)

## Getting Started

### 1. Clone the Repository

```bash
git clone https://github.com/obstreperous-ai/cdk-sleep-java-copilot.git
cd cdk-sleep-java-copilot
```

### 2. Build and Test

```bash
# Run all tests (Lambda + CDK infrastructure tests)
mvn test

# Compile the project
mvn compile
```

### 3. Deploy Infrastructure

#### Bootstrap CDK (First Time Only)

```bash
# Bootstrap your AWS environment for CDK
cdk bootstrap aws://ACCOUNT-NUMBER/REGION
```

#### Deploy to Development Environment

```bash
# Synthesize CloudFormation template
cdk synth

# Deploy the stack to dev environment (default)
cdk deploy

# Or explicitly specify environment
cdk deploy -c environment=dev
```

#### Deploy to Other Environments

```bash
# Deploy to staging
cdk deploy -c environment=stage

# Deploy to production
cdk deploy -c environment=prod
```

### 4. Test the Pipeline

Once deployed, test the pipeline:

```bash
# Upload a test audio file to the Input S3 bucket
aws s3 cp test-audio.mp3 s3://YOUR-INPUT-BUCKET-NAME/raw/test-user/test-audio.mp3

# Monitor Step Functions execution
aws stepfunctions list-executions \
  --state-machine-arn YOUR-STATE-MACHINE-ARN \
  --max-results 5

# Check DynamoDB for metadata
aws dynamodb scan --table-name SleepAudioMetadata

# Verify processed output in Output S3 bucket
aws s3 ls s3://YOUR-OUTPUT-BUCKET-NAME/processed/
```

## Project Structure

```
cdk-sleep-java-copilot/
├── src/
│   ├── main/java/com/myorg/
│   │   ├── CdkBaseApp.java          # Application entry point
│   │   ├── CdkBaseStack.java        # Main infrastructure stack
│   │   ├── PipelineStack.java       # CI/CD pipeline stack
│   │   └── lambda/
│   │       └── SleepAudioProcessor.java  # Audio processing Lambda
│   └── test/java/com/myorg/
│       ├── CdkBaseTest.java         # Infrastructure tests (65 tests)
│       └── lambda/
│           └── SleepAudioProcessorTest.java  # Lambda tests (14 tests)
├── ARCHITECTURE.md                   # Detailed architecture documentation
├── CONTRIBUTING.md                   # Contribution guidelines
├── SUMMARY.md                        # Project summary and key decisions
├── pom.xml                          # Maven project configuration
└── cdk.json                         # CDK configuration
```

## Development Guidelines

This project follows **strict Test-Driven Development (TDD)**:

1. **Write tests first** - Always create or update failing tests before implementation
2. **Minimal implementation** - Make the smallest change to pass tests
3. **Refactor** - Clean up code while keeping tests green
4. **Run tests frequently** - Execute `mvn test` after each meaningful change
5. **Validate with CDK** - Run `cdk synth` before committing changes

See [CONTRIBUTING.md](CONTRIBUTING.md) for detailed development guidelines.

## Strict TDD rules

- Always write or update the failing test before implementation.
- Make the smallest possible implementation change to satisfy the test.
- Re-run the relevant tests after each meaningful change.
- Keep `ARCHITECTURE.md` and its Mermaid diagram aligned with the current design.
- Run `mvn test` and `cdk synth` before considering work complete.

## Testing

The project includes comprehensive test coverage:

```bash
# Run all tests (79 total)
mvn test

# Run only Lambda function tests
mvn test -Dtest=SleepAudioProcessorTest

# Run only CDK infrastructure tests
mvn test -Dtest=CdkBaseTest

# Synthesize CloudFormation for all environments
cdk synth                           # dev (default)
cdk synth -c environment=stage      # stage
cdk synth -c environment=prod       # prod
```

### Test Coverage

- **14 Lambda Function Tests**: Input validation, audio processing, error handling
- **65 CDK Infrastructure Tests**: Resource creation, permissions, integrations, multi-environment
- **6 End-to-End Tests**: Complete pipeline flow, error handling, observability

## Environment Configuration

The pipeline supports three environments with different policies:

| Environment | Removal Policy | Use Case | Tags |
|------------|---------------|----------|------|
| **dev** | DESTROY | Development and testing | Environment=dev, Project=SleepAudioPipeline |
| **stage** | DESTROY | Pre-production validation | Environment=stage, Project=SleepAudioPipeline |
| **prod** | RETAIN | Production workloads | Environment=prod, Project=SleepAudioPipeline |

Set environment via CDK context:

```bash
cdk deploy -c environment=prod
```

## Observability

The pipeline includes comprehensive observability:

- **X-Ray Tracing**: End-to-end request tracing through Lambda and Step Functions
- **Structured Logging**: JSON-formatted CloudWatch logs with request IDs
- **CloudWatch Alarms**: Automated alerts for State Machine failures and Lambda errors
- **Metrics**: Built-in AWS service metrics for all components

View logs and traces:

```bash
# View State Machine logs
aws logs tail /aws/vendedlogs/states/SleepAudioPipelineStateMachine --follow

# View Lambda logs
aws logs tail /aws/lambda/SleepAudioProcessor --follow

# View X-Ray traces
aws xray get-trace-summaries --start-time $(date -u -d '1 hour ago' +%s) --end-time $(date +%s)
```

## Supported Audio Formats

The pipeline currently supports:

- **MP3** (.mp3) - MPEG-1 Audio Layer 3
- **WAV** (.wav) - Waveform Audio File Format
- **M4A** (.m4a) - MPEG-4 Audio

Files with unsupported extensions are rejected with clear error messages.

## Useful commands

- `mvn test` - Run all tests (79 tests: 14 Lambda + 65 CDK)
- `npx aws-cdk synth` - Synthesize CloudFormation template

## Cost Considerations

As a serverless pipeline, you pay only for what you use:

- **S3**: Storage and data transfer
- **Lambda**: Execution time (millisecond billing)
- **Step Functions**: State transitions
- **DynamoDB**: On-demand capacity (PAY_PER_REQUEST)
- **SNS**: Published messages
- **CloudWatch**: Log storage and metrics

The dev environment uses DESTROY removal policy for cost optimization during development.

## Security

Security features include:

- **Encryption at Rest**: S3 (SSE-S3), DynamoDB (AWS-managed), SNS (KMS)
- **Encryption in Transit**: TLS for all AWS service communications
- **Private Buckets**: All public access blocked on S3 buckets
- **Least-Privilege IAM**: Minimal permissions for each service
- **No Secrets in Code**: All credentials managed by IAM roles

## CI/CD

The project includes GitHub Actions workflow (`.github/workflows/ci.yml`):

- Builds and tests on every push and PR
- Tests multi-environment CDK synthesis
- Validates dev, stage, and prod configurations

## Troubleshooting

### Common Issues

**Problem**: Tests fail with "bucket name not found"  
**Solution**: Ensure environment variables are set in test setup

**Problem**: CDK synth fails with dependency errors  
**Solution**: Run `mvn clean install` to refresh dependencies

**Problem**: Lambda function times out  
**Solution**: Check CloudWatch logs for errors; increase timeout in `CdkBaseStack.java` if needed

### Getting Help

- Review [ARCHITECTURE.md](ARCHITECTURE.md) for design details
- Check [CONTRIBUTING.md](CONTRIBUTING.md) for development workflow
- Examine test files for usage examples

## Related Documentation

- **[ARCHITECTURE.md](ARCHITECTURE.md)** - Comprehensive architecture documentation with Mermaid diagrams
- **[META-PROMPTS.md](META-PROMPTS.md)** - Reusable patterns and agent guidelines for agentic TDD IaC projects
- **[CONTRIBUTING.md](CONTRIBUTING.md)** - Development guidelines and TDD workflow
- **[SUMMARY.md](SUMMARY.md)** - Project summary, key decisions, and experiment notes
- **[.github/AGENT_GUIDELINES.md](.github/AGENT_GUIDELINES.md)** - Guidelines for AI agents

## Reusable Patterns

This project provides **reusable patterns** for future agentic Infrastructure as Code projects:

### For AI Agents
- **Agent Personas**: Pre-defined specialist roles (AWS CDK Java TDD Specialist, Infrastructure Testing Specialist)
- **Activation Prompts**: Ready-to-use prompts for consistent agent behavior
- **Workflow Templates**: Step-by-step patterns for test-first development

### For Teams
- **Issue Templates**: Structured format for incremental delivery
- **Documentation Patterns**: README, ARCHITECTURE, and SUMMARY structure
- **Testing Strategies**: Unit, integration, and E2E test organization
- **Multi-Environment**: Context-based deployment patterns

### For Projects
- **CDK Patterns**: Service integrations, retry policies, observability
- **TDD Workflow**: Red-Green-Refactor cycle for infrastructure
- **Architecture as Code**: Living documentation synchronized with implementation

**See [META-PROMPTS.md](META-PROMPTS.md) for the complete pattern library with code examples and templates.**

## Acknowledgments

This project was built following strict Test-Driven Development (TDD) principles with AWS CDK and Java 17. It demonstrates:

- Event-driven serverless architecture on AWS
- Infrastructure as Code with AWS CDK
- Comprehensive test coverage (79 tests)
- Multi-environment deployments
- Production-ready observability and error handling

**This project is a reference implementation** showing how AI agents can build production-quality infrastructure through:
- 🧪 **Pure TDD methodology** (test-first, always)
- 📋 **Issue-driven incremental delivery** (Issues #2–#12)
- 🤖 **Agent-guided development** (with explicit personas and guidelines)
- 📚 **Living documentation** (ARCHITECTURE.md as single source of truth)

The patterns, prompts, and workflows extracted from this experiment are available in [META-PROMPTS.md](META-PROMPTS.md) for reuse in your own projects.

**Built by GitHub Copilot** following the methodology described in this repository.

## License

See [LICENSE](LICENSE) file for details.

---

**Built with ❤️ using AWS CDK, Java 17, and strict TDD methodology**

