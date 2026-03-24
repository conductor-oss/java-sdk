# SOC2 Automation

A SOC2 Type II audit requires collecting controls, testing their effectiveness, identifying exceptions, and generating an evidence package for the auditor. The pipeline collects 48 controls for security, finds 46/48 operating effectively, and flags 2 exceptions: MFA enforcement gap and backup testing overdue.

## Workflow

```
soc2_collect_controls ──> soc2_test_effectiveness ──> soc2_identify_exceptions ──> soc2_generate_evidence
```

Workflow `soc2_automation_workflow` accepts `trustServiceCriteria` and `period`. Times out after `1200` seconds.

## Workers

**CollectControlsWorker** (`soc2_collect_controls`) -- reports `"Collected 48 controls for security"`. Returns `collect_controlsId` = `"COLLECT_CONTROLS-1361"`.

**TestEffectivenessWorker** (`soc2_test_effectiveness`) -- tests control operation. Reports `"46/48 controls operating effectively"`. Returns `test_effectiveness` = `true`.

**IdentifyExceptionsWorker** (`soc2_identify_exceptions`) -- flags gaps. Reports `"2 exceptions: MFA enforcement gap, backup testing overdue"`. Returns `identify_exceptions` = `true`.

**GenerateEvidenceWorker** (`soc2_generate_evidence`) -- produces the evidence package. Reports `"Evidence package generated for auditor"`. Returns `generate_evidence` = `true`.

## Workflow Output

The workflow produces `collect_controlsResult`, `generate_evidenceResult` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Project Structure

This example contains 4 worker implementations in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `soc2_automation_workflow` defines 4 tasks with input parameters `trustServiceCriteria`, `period` and a timeout of `1200` seconds.

## Workflow Definition Details

Schema version `2`, workflow version `1`. Owner: `examples@orkes.io`.

## Tests

2 tests verify the end-to-end SOC2 automation pipeline from control collection through evidence generation.

## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
