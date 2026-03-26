# Root Cause Analysis

A production incident needs diagnosis. The pipeline detects the issue from symptoms, collects evidence (logs and metrics), analyzes the evidence, and identifies the root cause with a confidence score.

## Workflow

```
rca_detect_issue ──> rca_collect_evidence ──> rca_analyze ──> rca_identify_root_cause
```

Workflow `root_cause_analysis_425` accepts `incidentId`, `affectedService`, and `symptom`. Times out after `60` seconds.

## Workers

**DetectIssueWorker** (`rca_detect_issue`) -- detects the issue from the incident ID and symptom.

**CollectEvidenceWorker** (`rca_collect_evidence`) -- collects logs and metrics as evidence.

**RcaAnalyzeWorker** (`rca_analyze`) -- analyzes the collected logs and metrics.

**IdentifyRootCauseWorker** (`rca_identify_root_cause`) -- identifies the root cause with a confidence percentage.

## Workflow Output

The workflow produces `rootCause`, `confidence`, `remediation`, `evidenceCount` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Project Structure

This example contains 4 worker implementations in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `root_cause_analysis_425` defines 4 tasks with input parameters `incidentId`, `affectedService`, `symptom` and a timeout of `60` seconds.

## Workflow Definition Details

Schema version `2`, workflow version `1`. Owner: `examples@orkes.io`.

## Tests

2 tests verify issue detection, evidence collection, analysis, and root cause identification.

## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
