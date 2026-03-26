# Vendor Risk Assessment

A new vendor (CloudAnalytics Inc) needs security vetting before onboarding. The pipeline collects a security questionnaire, scores the vendor at 72/100 (medium risk with data encryption gaps), reviews their SOC 2 Type II report (valid, no qualifications), and makes a conditional approval decision requiring encryption at rest.

## Workflow

```
vr_collect_questionnaire ──> vr_assess_risk ──> vr_review_soc2 ──> vr_make_decision
```

Workflow `vendor_risk_workflow` accepts `vendorName` and `dataAccess`. Times out after `86400` seconds (24 hours, accounting for vendor response time).

## Workers

**CollectQuestionnaireWorker** (`vr_collect_questionnaire`) -- reports `"CloudAnalytics Inc: security questionnaire completed"`. Returns `collect_questionnaireId` = `"COLLECT_QUESTIONNAIRE-1362"`.

**AssessRiskWorker** (`vr_assess_risk`) -- scores the vendor. Reports `"Risk score: 72/100 -- medium risk, data encryption gaps identified"`. Returns `assess_risk` = `true`.

**ReviewSoc2Worker** (`vr_review_soc2`) -- evaluates the SOC 2 report. Reports `"SOC 2 Type II report valid, no qualifications"`. Returns `review_soc2` = `true`.

**MakeDecisionWorker** (`vr_make_decision`) -- makes the onboarding decision. Reports `"Approved with conditions: require encryption at rest"`. Returns `make_decision` = `true`.

## Workflow Output

The workflow produces `collect_questionnaireResult`, `make_decisionResult` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Project Structure

This example contains 4 worker implementations in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `vendor_risk_workflow` defines 4 tasks with input parameters `vendorName`, `dataAccess` and a timeout of `86400` seconds.

## Workflow Definition Details

Schema version `2`, workflow version `1`. Owner: `examples@orkes.io`.

## Tests

2 tests verify the end-to-end vendor risk assessment pipeline from questionnaire through decision.

## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
