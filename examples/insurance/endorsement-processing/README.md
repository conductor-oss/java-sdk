# Endorsement Processing

Orchestrates endorsement processing through a multi-stage Conductor workflow.

**Input:** `policyId`, `changeType`, `details` | **Timeout:** 60s

## Pipeline

```
edp_request_change
    │
edp_assess
    │
edp_price
    │
edp_approve
    │
edp_apply
```

## Workers

**ApplyWorker** (`edp_apply`)

Reads `policyId`. Outputs `applied`, `effectiveDate`.

**ApproveWorker** (`edp_approve`)

Outputs `approved`.

**AssessWorker** (`edp_assess`)

Outputs `impact`.

**PriceWorker** (`edp_price`)

Outputs `premiumChange`, `prorated`.

**RequestChangeWorker** (`edp_request_change`)

Reads `policyId`. Outputs `endorsementId`.

## Tests

**2 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
