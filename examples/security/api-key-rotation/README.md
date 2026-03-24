# API Key Rotation

A service's API key needs rotation without downtime. The system must generate a new key, run both old and new keys simultaneously during a transition period, migrate all 5 consumers to the new key, then revoke the old one.

## Workflow

```
akr_generate_new ──> akr_dual_active ──> akr_migrate_consumers ──> akr_revoke_old
```

Workflow `api_key_rotation_workflow` accepts `service` and `keyId` as inputs. All tasks have `retryCount` = `2` and `responseTimeoutSeconds` = `30`. Times out after `1800` seconds.

## Workers

**GenerateNewWorker** (`akr_generate_new`) -- reads `service` from input (defaults to `"unknown"`). Generates a new API key and returns `generate_newId` = `"GENERATE_NEW-1374"` and `success` = `true`.

**DualActiveWorker** (`akr_dual_active`) -- receives `oldKeyId` and `newKeyId`. Enables both keys simultaneously during the transition window. Returns `dual_active` = `true`.

**MigrateConsumersWorker** (`akr_migrate_consumers`) -- updates consumers to use the new key and reports `"5 consumers updated to new key"`. Returns `migrate_consumers` = `true`.

**RevokeOldWorker** (`akr_revoke_old`) -- revokes the old key after all consumers are migrated. Returns `revoke_old` = `true` and `completedAt` = `"2024-01-15T10:30:00Z"`.

## Workflow Output

The workflow produces `newKeyId`, `consumersMigrated`, `oldKeyRevoked` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Task Configuration

- `akr_generate_new`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30
- `akr_dual_active`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30
- `akr_migrate_consumers`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30
- `akr_revoke_old`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30

These settings are declared in `task-defs.json` and apply independently to each task, controlling retry behavior, timeout detection, and backoff strategy without any changes to worker code.

## Project Structure

This example contains 4 worker implementations in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `api_key_rotation_workflow` defines 4 tasks with input parameters `service`, `keyId` and a timeout of `1800` seconds.

## Tests

4 tests verify key generation, dual-active period, consumer migration, and old key revocation.

## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
