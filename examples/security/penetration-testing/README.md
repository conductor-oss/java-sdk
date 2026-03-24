# Penetration Testing

A target system needs security assessment. The pipeline performs reconnaissance (discovering 12 endpoints and 4 open ports), scans for 5 potential vulnerabilities, tests exploitability (confirming 2 of 5 are exploitable), and generates a report with remediation steps.

## Workflow

```
pen_reconnaissance ──> pen_scan_vulnerabilities ──> pen_exploit_test ──> pen_generate_report
```

Workflow `penetration_testing_workflow` accepts `target` and `scope`. All tasks have `retryCount` = `2`. Times out after `1800` seconds.

## Workers

**ReconnaissanceWorker** (`pen_reconnaissance`) -- reads `target` from input (defaults to `"unknown"`). Reports `target + ": 12 endpoints, 4 open ports discovered"`. Returns `reconnaissanceId` = `"RECONNAISSANCE-1382"`.

**ScanVulnerabilitiesWorker** (`pen_scan_vulnerabilities`) -- scans discovered attack surface. Reports `"Found 5 potential vulnerabilities"`. Returns `scan_vulnerabilities` = `true`.

**ExploitTestWorker** (`pen_exploit_test`) -- tests vulnerability exploitability. Reports `"2 of 5 vulnerabilities confirmed exploitable"`. Returns `exploit_test` = `true`.

**GenerateReportWorker** (`pen_generate_report`) -- produces the assessment report. Reports `"Pen test report generated with remediation steps"`. Returns `generate_report` = `true` and `completedAt` = `"2026-01-15T10:05:00Z"`.

## Workflow Output

The workflow produces `reconData`, `vulnerabilitiesFound`, `exploitResults`, `report` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Task Configuration

- `pen_reconnaissance`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30
- `pen_scan_vulnerabilities`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30
- `pen_exploit_test`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30
- `pen_generate_report`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30

These settings are declared in `task-defs.json` and apply independently to each task, controlling retry behavior, timeout detection, and backoff strategy without any changes to worker code.

## Project Structure

This example contains 4 worker implementations in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `penetration_testing_workflow` defines 4 tasks with input parameters `target`, `scope` and a timeout of `1800` seconds.

## Tests

8 tests verify reconnaissance, vulnerability scanning, exploit testing, report generation, and the full pen test pipeline.

## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
