# cdk-sleep-java-copilot

This repository contains the Java AWS CDK foundation for an event-driven sleep audio pipeline that will ingest audio from Amazon S3, route workflow events through EventBridge, process and enrich assets, and publish resulting artifacts and notifications to AWS services such as S3, DynamoDB, and SNS.

## Strict TDD rules

- Always write or update the failing test before implementation.
- Make the smallest possible implementation change to satisfy the test.
- Re-run the relevant tests after each meaningful change.
- Keep `ARCHITECTURE.md` and its Mermaid diagram aligned with the current design.
- Run `mvn test` and `cdk synth` before considering work complete.

## Useful commands

- `mvn test`
- `npx aws-cdk synth`
