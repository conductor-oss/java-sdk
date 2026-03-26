# Authentication Workflow

A user logs in with a password. The system must validate credentials, verify MFA, assess risk based on device and location, and issue a JWT token with a 1-hour expiry -- all as a sequential pipeline where each step feeds context to the next.

## Workflow

```
auth_validate_credentials ──> auth_check_mfa ──> auth_risk_assessment ──> auth_issue_token
```

Workflow `authentication_workflow` accepts `userId` and `authMethod` as inputs. All tasks have `retryCount` = `2`. Times out after `1800` seconds.

## Workers

**ValidateCredentialsWorker** (`auth_validate_credentials`) -- reads `userId` from input (defaults to `"unknown"`). Validates the password and returns `validate_credentialsId` = `"VALIDATE_CREDENTIALS-1371"` and `success` = `true`.

**CheckMfaWorker** (`auth_check_mfa`) -- reads `authMethod` from input (defaults to `"unknown"`). Reports `authMethod + " verification passed"`. Returns `check_mfa` = `true`.

**RiskAssessmentWorker** (`auth_risk_assessment`) -- evaluates device and location context. Reports `"Known device, known location -- low risk"`. Returns `risk_assessment` = `true`.

**IssueTokenWorker** (`auth_issue_token`) -- issues a JWT with 1-hour expiry after all checks pass. Returns `issue_token` = `true` and `completedAt` = `"2024-01-15T10:30:00Z"`.

## Workflow Output

The workflow produces `credentialsValid`, `mfaVerified`, `riskScore`, `token` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Task Configuration

- `auth_validate_credentials`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30
- `auth_check_mfa`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30
- `auth_risk_assessment`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30
- `auth_issue_token`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30

These settings are declared in `task-defs.json` and apply independently to each task, controlling retry behavior, timeout detection, and backoff strategy without any changes to worker code.

## Project Structure

This example contains 4 worker implementations in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `authentication_workflow` defines 4 tasks with input parameters `userId`, `authMethod` and a timeout of `1800` seconds.

## Tests

8 tests verify credential validation, MFA verification, risk assessment, token issuance, and the full authentication pipeline.

## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
