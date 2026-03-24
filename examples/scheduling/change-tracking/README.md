# Change Tracking

An infrastructure resource changes. The pipeline detects the change by resource type and ID, diffs the before/after state, classifies the change by file count, line count, change type, and risk level, then records it permanently.

## Workflow

```
chg_detect_change ──> chg_diff ──> chg_classify ──> chg_record
```

Workflow `change_tracking_427` accepts `resourceType`, `resourceId`, and `changeSource`. Times out after `60` seconds.

## Workers

**DetectChangeWorker** (`chg_detect_change`) -- detects the change in the specified resource.

**DiffWorker** (`chg_diff`) -- computes the diff between before and after states.

**ClassifyChangeWorker** (`chg_classify`) -- classifies by file count, line count, change type, and risk level.

**RecordChangeWorker** (`chg_record`) -- records the classified change with its risk level.

## Workflow Output

The workflow produces `changeDetected`, `filesChanged`, `riskLevel`, `recorded` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Project Structure

This example contains 4 worker implementations in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `change_tracking_427` defines 4 tasks with input parameters `resourceType`, `resourceId`, `changeSource` and a timeout of `60` seconds.

## Workflow Definition Details

Schema version `2`, workflow version `1`. Owner: `examples@orkes.io`.

## Tests

2 tests verify change detection, diffing, classification, and recording.

## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
