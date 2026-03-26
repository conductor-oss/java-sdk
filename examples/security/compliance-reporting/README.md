# Compliance Reporting

A quarterly audit requires collecting evidence across 156 items, mapping them to 64 control objectives, identifying gaps (backup verification and access review timeliness), and generating a report for the auditor.

## Workflow

```
cr_collect_evidence ──> cr_map_controls ──> cr_assess_gaps ──> cr_generate_report
```

Workflow `compliance_reporting_workflow` accepts `framework` and `period`. All tasks have `retryCount` = `2`. Times out after `1800` seconds.

## Workers

**CollectEvidenceWorker** (`cr_collect_evidence`) -- reads `framework` from input (defaults to `"unknown"`). Reports `"Collected 156 evidence items for " + framework`. Returns `collect_evidenceId` = `"COLLECT_EVIDENCE-1383"`.

**MapControlsWorker** (`cr_map_controls`) -- maps evidence to control objectives. Reports `"Mapped evidence to 64 control objectives"`. Returns `map_controls` = `true`.

**AssessGapsWorker** (`cr_assess_gaps`) -- identifies compliance gaps. Reports `"2 gaps found: backup verification, access review timeliness"`. Returns `assess_gaps` = `true`.

**GenerateReportWorker** (`cr_generate_report`) -- generates the auditor report. Returns `generate_report` = `true` and `completedAt` = `"2026-01-15T10:05:00Z"`.

## Workflow Output

The workflow produces `evidenceCollected`, `controlsMapped`, `gapsAssessed`, `reportGenerated` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Task Configuration

- `cr_collect_evidence`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30
- `cr_map_controls`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30
- `cr_assess_gaps`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30
- `cr_generate_report`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30

These settings are declared in `task-defs.json` and apply independently to each task, controlling retry behavior, timeout detection, and backoff strategy without any changes to worker code.

## Project Structure

This example contains 4 worker implementations in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `compliance_reporting_workflow` defines 4 tasks with input parameters `framework`, `period` and a timeout of `1800` seconds.

## Tests

8 tests verify evidence collection, control mapping, gap assessment, report generation, and the full compliance pipeline.

## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
