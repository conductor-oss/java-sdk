# Secrets Management

A service needs a new API credential. The system must generate a cryptographically secure secret using `SecureRandom`, distribute it to authorized consumers with an integrity hash for verification, verify access permissions, and schedule automatic rotation on a configurable cycle (default 90 days).

## Workflow

```
sec_create_secret ──> sec_distribute ──> sec_verify_access ──> sec_schedule_rotation
```

Workflow `secrets_management_workflow` accepts `secretName`, `secretType`, and `consumers`. Times out after `300` seconds.

## Workers

**CreateSecretWorker** (`sec_create_secret`) -- reads `secretName` (defaults to `"default-secret"`) and `length` (clamped to 16-256 range, default `32`). Generates random bytes via `SecureRandom`, encodes with `Base64.getUrlEncoder().withoutPadding()`. Generates a `secretId` with `"SEC-"` prefix from 12 random bytes. Returns `secretId`, `secretName`, `secretValue`, `lengthBytes`, and `createdAt`.

**DistributeWorker** (`sec_distribute`) -- reads `secretId` and `secretValue`. Computes a SHA-256 `integrityHash` (first 16 hex characters) of the secret value using `MessageDigest` and `HexFormat`. Returns `distributed` = `true`, the `integrityHash`, and `distributedAt`.

**VerifyAccessWorker** (`sec_verify_access`) -- reads `requestorId`, `secretName`, and `operation` (defaults to `"read"`). Access is granted if `requestorId` is non-null and non-empty. Returns `hasAccess` boolean and `verifiedAt`.

**ScheduleRotationWorker** (`sec_schedule_rotation`) -- reads `secretId` and `rotationDays` (defaults to `90`). Computes `nextRotationAt` as `Instant.now().plus(rotationDays, ChronoUnit.DAYS)`. Returns `scheduled` = `true`, `rotationDays`, and `nextRotationAt`.

## Workflow Output

The workflow produces `create_secretResult`, `schedule_rotationResult` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Project Structure

This example contains 4 worker implementations in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `secrets_management_workflow` defines 4 tasks with input parameters `secretName`, `secretType`, `consumers` and a timeout of `300` seconds.

## Workflow Definition Details

Schema version `2`, workflow version `1`. Owner: `examples@orkes.io`.

## Tests

1 test verifies the end-to-end secrets lifecycle from creation through rotation scheduling.

## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
