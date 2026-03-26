# Security Orchestration (SOAR)

A CrowdStrike alert fires. The system must ingest the alert, enrich it with threat intel, asset context, and user history, decide on an action plan (isolate host, block C2 domain, collect forensics), and execute the playbook automatically.

## Workflow

```
soar_ingest_alert ──> soar_enrich ──> soar_decide_action ──> soar_execute_playbook
```

Workflow `security_orchestration_workflow` accepts `alertId` and `alertSource`. Times out after `300` seconds.

## Workers

**IngestAlertWorker** (`soar_ingest_alert`) -- ingests the alert. Reports `"Alert ALERT-2024-commission-insurance from crowdstrike"`. Returns `ingest_alertId` = `"INGEST_ALERT-1352"`.

**EnrichWorker** (`soar_enrich`) -- adds context. Reports `"Added threat intel, asset context, user history"`. Returns `enrich` = `true`.

**DecideActionWorker** (`soar_decide_action`) -- determines response. Reports `"Action: isolate host, block C2 domain, collect forensics"`. Returns `decide_action` = `true`.

**ExecutePlaybookWorker** (`soar_execute_playbook`) -- executes the response plan. Reports `"Host isolated, C2 domain blocked, forensic collection started"`. Returns `execute_playbook` = `true`.

## Workflow Output

The workflow produces `ingest_alertResult`, `execute_playbookResult` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Project Structure

This example contains 4 worker implementations in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `security_orchestration_workflow` defines 4 tasks with input parameters `alertId`, `alertSource` and a timeout of `300` seconds.

## Workflow Definition Details

Schema version `2`, workflow version `1`. Owner: `examples@orkes.io`.

## Tests

2 tests verify the end-to-end SOAR pipeline from alert ingestion through playbook execution.

## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
