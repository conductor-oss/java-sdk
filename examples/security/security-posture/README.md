# Security Posture Assessment

An organization needs a composite security score. The pipeline assesses infrastructure (85/100, patching gaps), applications (78/100, 2 critical vulnerabilities in production), and compliance (92/100, access review overdue for 1 department), then calculates an overall grade of B at 85/100.

## Workflow

```
sp_assess_infrastructure ──> sp_assess_application ──> sp_assess_compliance ──> sp_calculate_score
```

Workflow `security_posture_workflow` accepts `organization` and `assessmentScope`. Times out after `1200` seconds.

## Workers

**AssessInfrastructureWorker** (`sp_assess_infrastructure`) -- reports `"acme-corp: 85/100 -- patching gaps found"`. Returns `assess_infrastructureId` = `"ASSESS_INFRASTRUCTURE-1400"`.

**AssessApplicationWorker** (`sp_assess_application`) -- reports `"78/100 -- 2 critical vulnerabilities in production"`. Returns `assess_application` = `true`.

**AssessComplianceWorker** (`sp_assess_compliance`) -- reports `"92/100 -- access review overdue for 1 department"`. Returns `assess_compliance` = `true`.

**CalculateScoreWorker** (`sp_calculate_score`) -- computes the composite score. Reports `"Overall security posture: B, 85/100"`. Returns `calculate_score` = `true`.

## Workflow Output

The workflow produces `assess_infrastructureResult`, `calculate_scoreResult` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Project Structure

This example contains 4 worker implementations in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `security_posture_workflow` defines 4 tasks with input parameters `organization`, `assessmentScope` and a timeout of `1200` seconds.

## Workflow Definition Details

Schema version `2`, workflow version `1`. Owner: `examples@orkes.io`.

## Tests

2 tests verify the end-to-end security posture assessment from infrastructure through final scoring.

## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
