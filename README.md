# Sleep Audio Pipeline (CDK Java)

> **Event-Driven Serverless Audio Processing Pipeline on AWS**  
> Built with AWS CDK (Java), strict TDD methodology, and infrastructure as code best practices.

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
- **[CONTRIBUTING.md](CONTRIBUTING.md)** - Development guidelines and TDD workflow
- **[SUMMARY.md](SUMMARY.md)** - Project summary, key decisions, and experiment notes
- **[.github/AGENT_GUIDELINES.md](.github/AGENT_GUIDELINES.md)** - Guidelines for AI agents

## Acknowledgments

This project was built following strict Test-Driven Development (TDD) principles with AWS CDK and Java 17. It demonstrates:

- Event-driven serverless architecture on AWS
- Infrastructure as Code with AWS CDK
- Comprehensive test coverage (79 tests)
- Multi-environment deployments
- Production-ready observability and error handling

## License

See [LICENSE](LICENSE) file for details.

---

**Built with ❤️ using AWS CDK, Java 17, and strict TDD methodology**

