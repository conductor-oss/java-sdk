# OAuth Token Management

A client application requests an OAuth token. The system must validate the grant type and client credentials, issue access and refresh tokens with the requested scope, store token metadata for revocation support, and log the issuance event for compliance.

## Workflow

```
otm_validate_grant ──> otm_issue_tokens ──> otm_store_token ──> otm_audit_log
```

Workflow `oauth_token_management_workflow` accepts `clientId`, `grantType`, and `scope`. All tasks have `retryCount` = `2`. Times out after `1800` seconds.

## Workers

**ValidateGrantWorker** (`otm_validate_grant`) -- reads `clientId` and `grantType` from input (both default to `"unknown"`). Reports `"Client " + clientId + ": " + grantType + " grant validated"`. Returns `validate_grantId` = `"VALIDATE_GRANT-1373"`.

**IssueTokensWorker** (`otm_issue_tokens`) -- reads `scope` from input (defaults to `"default"`). Reports `"Access token issued with scope: " + scope`. Returns `issue_tokens` = `true`.

**StoreTokenWorker** (`otm_store_token`) -- stores token metadata for future revocation. Reports `"Token metadata stored for revocation support"`. Returns `store_token` = `true`.

**AuditLogWorker** (`otm_audit_log`) -- logs the token issuance for compliance. Returns `audit_log` = `true` and `completedAt` = `"2024-01-15T10:30:00Z"`.

## Workflow Output

The workflow produces `grantValid`, `tokenId`, `stored`, `auditLogged` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Task Configuration

- `otm_validate_grant`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30
- `otm_issue_tokens`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30
- `otm_store_token`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30
- `otm_audit_log`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30

These settings are declared in `task-defs.json` and apply independently to each task, controlling retry behavior, timeout detection, and backoff strategy without any changes to worker code.

## Project Structure

This example contains 4 worker implementations in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `oauth_token_management_workflow` defines 4 tasks with input parameters `clientId`, `grantType`, `scope` and a timeout of `1800` seconds.

## Tests

8 tests verify grant validation, token issuance, metadata storage, audit logging, and the full OAuth pipeline.

## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
