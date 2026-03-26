# Access Review

A department with 45 users has accumulated 312 entitlements over time. Some are excessive, some belong to dormant accounts, and managers need to certify which access grants should remain before enforcement revokes the rest.

## Workflow

```
ar_collect_entitlements ──> ar_identify_anomalies ──> ar_request_certification ──> ar_enforce_decisions
```

Workflow `access_review_workflow` accepts `department` and `reviewCycle` as inputs. Times out after `7200` seconds.

## Workers

**CollectEntitlementsWorker** (`ar_collect_entitlements`) -- scans the engineering department and reports `"45 users, 312 entitlements collected"`. Returns `collect_entitlementsId` = `"COLLECT_ENTITLEMENTS-1300"` and `success` = `true`.

**IdentifyAnomaliesWorker** (`ar_identify_anomalies`) -- analyzes collected entitlements and reports `"8 excessive access grants, 3 dormant accounts"`. Returns `identify_anomalies` = `true` and `processed` = `true`.

**RequestCertificationWorker** (`ar_request_certification`) -- sends anomalies to managers for approval and reports `"Manager approved 5 revocations, kept 6"`. Returns `request_certification` = `true`.

**EnforceDecisionsWorker** (`ar_enforce_decisions`) -- applies the certified decisions and reports `"5 access grants revoked across 3 systems"`. Returns `enforce_decisions` = `true`.

## Workflow Output

The workflow produces `collect_entitlementsResult`, `enforce_decisionsResult` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Project Structure

This example contains 4 worker implementations in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `access_review_workflow` defines 4 tasks with input parameters `department`, `reviewCycle` and a timeout of `7200` seconds.

## Workflow Definition Details

Schema version `2`, workflow version `1`. Owner: `examples@orkes.io`.

## Tests

2 tests verify the end-to-end access review pipeline from entitlement collection through enforcement.

## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
