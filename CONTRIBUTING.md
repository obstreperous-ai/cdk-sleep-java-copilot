# Contributing

## Source of truth

[`ARCHITECTURE.md`](ARCHITECTURE.md) is the single source of truth for the Event-Driven Sleep
Audio Pipeline. Read it before starting any issue and ensure every change stays consistent with
its description and Mermaid diagram.

## Working agreement

- Follow strict TDD: write or update a failing test first, then implement the smallest change needed to make it pass.
- Re-run the relevant tests immediately after each change and finish with `mvn test` plus `cdk synth`.
- Keep `ARCHITECTURE.md` and its Mermaid diagram in sync with any architectural change.
- Use conventional commits only.
- Prefer issue-driven, incremental delivery over speculative implementation.

## Local development

1. Install Java 17, Maven, Node.js, and the AWS CDK CLI (or use `npx aws-cdk`).
2. Run `mvn test` before implementation changes.
3. Run `cdk synth` before opening or updating a pull request.
