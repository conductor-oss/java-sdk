# Rfp Automation

RFP pipeline: create, distribute, collect, evaluate, and select.

**Input:** `projectTitle`, `requirements`, `deadline` | **Timeout:** 60s

## Pipeline

```
rfp_create
    │
rfp_distribute
    │
rfp_collect
    │
rfp_evaluate
    │
rfp_select
```

## Workers

**CollectWorker** (`rfp_collect`)

Outputs `proposals`, `proposalCount`.

**CreateWorker** (`rfp_create`)

Reads `projectTitle`. Outputs `rfpId`.

**DistributeWorker** (`rfp_distribute`)

Reads `deadline`, `rfpId`. Outputs `distributedTo`.

**EvaluateWorker** (`rfp_evaluate`)

Reads `proposals`. Outputs `topCandidate`, `evaluationComplete`.

**SelectWorker** (`rfp_select`)

Reads `rfpId`, `topCandidate`. Outputs `selectedVendor`, `notified`.

## Tests

**10 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
