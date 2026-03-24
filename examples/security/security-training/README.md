# Security Awareness Training

A department of 45 employees needs annual security training. The pipeline assigns training modules, sends a phishing simulation (measuring an 8% click rate), evaluates results (42/45 completed training, 8% phishing susceptibility), and generates a compliance report.

## Workflow

```
st_assign_training ──> st_send_phishing_sim ──> st_evaluate_results ──> st_report_compliance
```

Workflow `security_training_workflow` accepts `department` and `trainingModule`. All tasks have `retryCount` = `2`. Times out after `1800` seconds.

## Workers

**StAssignTrainingWorker** (`st_assign_training`) -- reads `trainingModule` (defaults to `"general-awareness"`) and `department` (defaults to `"all"`). Reports `trainingModule + " assigned to " + department + ": 45 employees"`. Returns `assign_trainingId` = `"ASSIGN_TRAINING-1393"`.

**StSendPhishingSimWorker** (`st_send_phishing_sim`) -- sends simulated phishing emails. Reports `"Phishing simulation sent: 8% click rate"`. Returns `send_phishing_sim` = `true`.

**StEvaluateResultsWorker** (`st_evaluate_results`) -- evaluates training and phishing results. Reports `"42/45 completed training, 8% phishing susceptibility"`. Returns `evaluate_results` = `true`.

**StReportComplianceWorker** (`st_report_compliance`) -- generates the compliance report. Returns `report_compliance` = `true` and `completedAt` = `"2026-01-15T10:00:00Z"`.

## Workflow Output

The workflow produces `assignmentId`, `phishingSimSent`, `evaluationResults`, `complianceReport` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Task Configuration

- `st_assign_training`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30
- `st_send_phishing_sim`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30
- `st_evaluate_results`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30
- `st_report_compliance`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30

These settings are declared in `task-defs.json` and apply independently to each task, controlling retry behavior, timeout detection, and backoff strategy without any changes to worker code.

## Project Structure

This example contains 4 worker implementations in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `security_training_workflow` defines 4 tasks with input parameters `department`, `trainingModule` and a timeout of `1800` seconds.

## Tests

8 tests verify training assignment, phishing simulation, result evaluation, and compliance reporting.

## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
