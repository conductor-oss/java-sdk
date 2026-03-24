# Data Classification

A database with 42 tables and 318 columns needs automated data classification. The pipeline scans column schemas, detects 24 PII fields (emails, SSNs, phone numbers), classifies them by sensitivity level (24 sensitive, 89 internal, 205 public), and applies labels to the data catalog.

## Workflow

```
dc_scan_data_stores ──> dc_detect_pii ──> dc_classify ──> dc_apply_labels
```

Workflow `data_classification_workflow` accepts `dataStore` and `scanType`. All tasks have `retryCount` = `2`. Times out after `1800` seconds.

## Workers

**ScanDataStoresWorker** (`dc_scan_data_stores`) -- reads `dataStore` from input (defaults to `"unknown"`). Reports scanning the data store with `"42 tables, 318 columns"`. Returns `scan_data_storesId` = `"SCAN_DATA_STORES-1384"`.

**DetectPiiWorker** (`dc_detect_pii`) -- analyzes scanned data for personally identifiable information. Reports `"Detected 24 PII fields: emails, SSNs, phone numbers"`. Returns `detect_pii` = `true`.

**ClassifyWorker** (`dc_classify`) -- assigns sensitivity levels. Reports `"Classified: 24 sensitive, 89 internal, 205 public"`. Returns `classify` = `true`.

**ApplyLabelsWorker** (`dc_apply_labels`) -- writes classification labels to the data catalog. Returns `apply_labels` = `true` and `completedAt` = `"2026-01-15T10:05:00Z"`.

## Workflow Output

The workflow produces `scanResult`, `piiDetected`, `classification`, `labelsApplied` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Task Configuration

- `dc_scan_data_stores`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30
- `dc_detect_pii`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30
- `dc_classify`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30
- `dc_apply_labels`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30

These settings are declared in `task-defs.json` and apply independently to each task, controlling retry behavior, timeout detection, and backoff strategy without any changes to worker code.

## Project Structure

This example contains 4 worker implementations in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `data_classification_workflow` defines 4 tasks with input parameters `dataStore`, `scanType` and a timeout of `1800` seconds.

## Tests

4 tests verify data store scanning, PII detection, classification, and label application.

## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
