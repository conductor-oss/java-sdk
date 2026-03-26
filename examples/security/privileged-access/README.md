# Privileged Access Management

An engineer needs temporary production database access for an incident. The system must validate the request with justification, route it to the security team for approval, grant just-in-time access for the specified duration (default `"1h"`), and automatically revoke it after expiry.

## Workflow

```
pam_request ──> pam_approve ──> pam_grant_access ──> pam_revoke_access
```

Workflow `privileged_access_workflow` accepts `userId`, `resource`, `justification`, and `duration`. All tasks have `retryCount` = `2`. Times out after `1800` seconds.

## Workers

**PamRequestWorker** (`pam_request`) -- reads `userId`, `resource`, and `justification` from input (all default to `"unknown"` or `"none"`). Reports the access request. Returns `requestId` = `"REQUEST-1391"`.

**PamApproveWorker** (`pam_approve`) -- security team reviews the request. Reports `"Security team approved -- risk: low"`. Returns `approve` = `true`.

**PamGrantAccessWorker** (`pam_grant_access`) -- reads `resource` (defaults to `"unknown-resource"`) and `duration` (defaults to `"1h"`). Reports `"Temporary " + resource + " access granted for " + duration`. Returns `grant_access` = `true`.

**PamRevokeAccessWorker** (`pam_revoke_access`) -- automatically revokes access after expiry. Returns `revoke_access` = `true` and `completedAt` = `"2026-01-15T10:00:00Z"`.

## Workflow Output

The workflow produces `requestId`, `approved`, `accessGranted`, `accessRevoked` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Task Configuration

- `pam_request`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30
- `pam_approve`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30
- `pam_grant_access`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30
- `pam_revoke_access`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30

These settings are declared in `task-defs.json` and apply independently to each task, controlling retry behavior, timeout detection, and backoff strategy without any changes to worker code.

## Project Structure

This example contains 4 worker implementations in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `privileged_access_workflow` defines 4 tasks with input parameters `userId`, `resource`, `justification`, `duration` and a timeout of `1800` seconds.

## Tests

8 tests verify the request-approve-grant-revoke lifecycle and justification validation.

## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
