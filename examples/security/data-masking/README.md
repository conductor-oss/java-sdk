# Data Masking

A data source needs masking for non-production use. The pipeline identifies 18 sensitive fields, selects the appropriate masking strategy (tokenization for emails, redaction for SSNs) based on the data purpose, applies the masking, and validates that no PII remains in the output while referential integrity is maintained.

## Workflow

```
dm_identify_fields ──> dm_select_strategy ──> dm_apply_masking ──> dm_validate_output
```

Workflow `data_masking_workflow` accepts `dataSource` and `purpose`. All tasks have `retryCount` = `2`. Times out after `1800` seconds.

## Workers

**DmIdentifyFieldsWorker** (`dm_identify_fields`) -- reads `dataSource` from input (defaults to `"unknown"`). Reports detection of `"18 sensitive fields"`. Returns `identify_fieldsId` = `"IDENTIFY_FIELDS-1392"`.

**DmSelectStrategyWorker** (`dm_select_strategy`) -- reads `purpose` from input (defaults to `"general"`). Reports `"Masking strategy for " + purpose + ": tokenization + redaction"`. Returns `select_strategy` = `true`.

**DmApplyMaskingWorker** (`dm_apply_masking`) -- applies masking rules. Reports `"18 fields masked: emails tokenized, SSNs redacted"`. Returns `apply_masking` = `true`.

**DmValidateOutputWorker** (`dm_validate_output`) -- validates the masked output. Reports `"No PII found in masked output, referential integrity maintained"`. Returns `validate_output` = `true` and `completedAt` = `"2026-01-15T10:00:00Z"`.

## Workflow Output

The workflow produces `fieldsIdentified`, `strategySelected`, `maskingApplied`, `validated` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Task Configuration

- `dm_identify_fields`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30
- `dm_select_strategy`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30
- `dm_apply_masking`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30
- `dm_validate_output`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30

These settings are declared in `task-defs.json` and apply independently to each task, controlling retry behavior, timeout detection, and backoff strategy without any changes to worker code.

## Project Structure

This example contains 4 worker implementations in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `data_masking_workflow` defines 4 tasks with input parameters `dataSource`, `purpose` and a timeout of `1800` seconds.

## Tests

8 tests verify field identification, strategy selection, masking application, and output validation.

## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
