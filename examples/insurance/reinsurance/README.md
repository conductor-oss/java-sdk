# Reinsurance

Orchestrates reinsurance through a multi-stage Conductor workflow.

**Input:** `policyId`, `coverageAmount`, `riskCategory` | **Timeout:** 60s

## Pipeline

```
rin_assess_risk
    │
rin_treaty_lookup
    │
rin_cede
    │
rin_confirm
    │
rin_reconcile
```

## Workers

**AssessRiskWorker** (`rin_assess_risk`)

Reads `policyId`. Outputs `netExposure`, `retainedRisk`.

**CedeWorker** (`rin_cede`)

Reads `treatyId`. Outputs `cessionId`, `ceded`.

**ConfirmWorker** (`rin_confirm`)

Reads `cessionId`. Outputs `confirmed`, `confirmationDate`.

**ReconcileWorker** (`rin_reconcile`)

Reads `cessionId`. Outputs `reconciled`, `variance`.

**TreatyLookupWorker** (`rin_treaty_lookup`)

Reads `riskCategory`. Outputs `treatyId`, `reinsurer`, `cessionAmount`.

## Tests

**2 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
