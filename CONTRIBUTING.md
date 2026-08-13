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

### Running the OSS integration suite locally

The `tests` module also has an integration suite (`-PIntegrationTests`) that runs against a
real Conductor server, separate from the unit suite above. `scripts/run-integration-oss.sh`
mirrors the `integration-tests-oss` job in `ci.yml`: it starts a local Conductor OSS +
Postgres stack (defined in `scripts/docker-compose-oss.yaml`), waits for `/health`, runs the
integration suite, and tears the stack down on exit.

```shell
scripts/run-integration-oss.sh                        # against `latest`
scripts/run-integration-oss.sh --version 3.32.0-rc18
scripts/run-integration-oss.sh --keep-up               # leave the stack running afterwards
scripts/run-integration-oss.sh --include-gated         # also run tests normally skipped as Orkes-only
```

The script always prints the resolved `conductoross/conductor` tag and pulls it before
starting the stack, since `latest` (the local default) is a mutable tag — without an
explicit pull, `docker compose up` would silently reuse a stale cached image instead of
fetching the current one. It also always runs Gradle with `--rerun-tasks`, since the `test`
task's up-to-date check doesn't account for env vars like `CONDUCTOR_SERVER_TYPE` or the
state of the live server underneath — without it, a rerun after changing gating or switching
server versions could silently report a stale cached result instead of executing anything.

The script doesn't pin a JDK itself, but CI runs on Zulu 21. If your local default JDK is
newer (e.g. 23) you may hit `Unsupported class file major version` errors compiling tests —
set `JAVA_HOME` explicitly to match CI:

```shell
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-21.jdk/Contents/Home ./scripts/run-integration-oss.sh
```

Tests annotated `@DisabledIfEnvironmentVariable(named = "CONDUCTOR_SERVER_TYPE", matches =
"oss")` skip themselves against plain OSS because they exercise Orkes-managed-only features
(e.g. the Service Registry, Authorization, Prompts/Integrations, Environment Variables, and
Secrets APIs, plus a handful of task/workflow endpoints OSS doesn't implement or that hit
known Postgres-persistence bugs). Each annotation's `disabledReason` documents the specific,
empirically-confirmed gap — treat those as the source of truth rather than a list here, since
they can drift as OSS gains features. If you add or remove that annotation, re-verify against
a freshly-pulled image first: a test that fails against a stale local image may pass against
current OSS, and vice versa.

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
