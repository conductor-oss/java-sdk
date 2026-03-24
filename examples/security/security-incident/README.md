# Security Incident Response

A compromised API key is detected being used from an unauthorized IP. The system must triage the incident by type and severity, contain it by isolating the affected system, investigate the root cause, and remediate by revoking the key and preserving access logs.

## Workflow

```
si_triage ──> si_contain ──> si_investigate ──> si_remediate
```

Workflow `security_incident_workflow` accepts `incidentType`, `severity`, and `affectedSystem`. All tasks have `retryCount` = `2`. Times out after `1800` seconds.

## Workers

**TriageWorker** (`si_triage`) -- reads `incidentType` (defaults to `"unknown"`) and `severity` (defaults to `"P3"`). Reports `incidentType + " -- severity: " + severity`. Returns `triageId` = `"TRIAGE-1381"`.

**ContainWorker** (`si_contain`) -- isolates the affected system. Reports `"Isolated affected system from network"`. Returns `contain` = `true`.

**InvestigateWorker** (`si_investigate`) -- determines root cause. Reports `"Root cause: compromised API key used from unauthorized IP"`. Returns `investigate` = `true`.

**RemediateWorker** (`si_remediate`) -- applies fixes. Reports `"API key revoked, access logs preserved, patches applied"`. Returns `remediate` = `true` and `completedAt` = `"2026-01-15T10:05:00Z"`.

## Workflow Output

The workflow produces `triageResult`, `containmentResult`, `investigationResult`, `remediationResult` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Task Configuration

- `si_triage`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30
- `si_contain`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30
- `si_investigate`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30
- `si_remediate`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30

These settings are declared in `task-defs.json` and apply independently to each task, controlling retry behavior, timeout detection, and backoff strategy without any changes to worker code.

## Project Structure

This example contains 4 worker implementations in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `security_incident_workflow` defines 4 tasks with input parameters `incidentType`, `severity`, `affectedSystem` and a timeout of `1800` seconds.

## Tests

8 tests verify triage, containment, investigation, remediation, and the full incident response pipeline.

## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
