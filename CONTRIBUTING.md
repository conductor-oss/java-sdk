# Contributing to the Conductor Java SDK

Thanks for contributing. This repository contains the Java client, AI agent SDK, Spring integrations, examples, and end-to-end tests.

All participation is governed by the [Code of Conduct](CODE_OF_CONDUCT.md).

## Prerequisites

- Java 21+
- Docker for examples and integration tests that start Conductor

## Local checks

Run formatting before committing:

```shell
./gradlew spotlessApply
```

Run the SDK test suite:

```shell
./gradlew test jacocoTestReport
```

Compile the maintained agent examples when changing their APIs or documentation:

```shell
./gradlew :agent-examples:compileJava
```

## Pull requests

- Keep changes focused and include tests for behavior changes.
- Update the relevant documentation and examples when public APIs or commands change.
- Do not add secrets, credentials, or private endpoints to source, tests, or documentation.
- Open pull requests against `main` and complete the pull-request template.

To contribute across the broader project, browse [Conductor OSS contribution opportunities](https://github.com/conductor-oss/conductor/contribute) and follow the [upstream contribution guide](https://github.com/conductor-oss/conductor/blob/main/CONTRIBUTING.md).
