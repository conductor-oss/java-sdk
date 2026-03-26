# E Discovery

Orchestrates e discovery through a multi-stage Conductor workflow.

**Input:** `matterId`, `custodians`, `dateRange` | **Timeout:** 60s

## Pipeline

```
edc_identify
    │
edc_collect
    │
edc_process
    │
edc_review
    │
edc_produce
```

## Workers

**CollectWorker** (`edc_collect`)

Outputs `collected`, `sources`, `processed`, `results`.

**IdentifyWorker** (`edc_identify`)

Outputs `sources`, `collected`, `processed`, `results`.

**ProcessWorker** (`edc_process`)

Outputs `processed`, `sources`, `collected`, `results`.

**ProduceWorker** (`edc_produce`)

Outputs `producedCount`, `sources`, `collected`, `processed`, `results`.

**ReviewWorker** (`edc_review`)

Outputs `results`, `sources`, `collected`, `processed`.

## Tests

**10 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
