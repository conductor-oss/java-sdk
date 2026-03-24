# Maintenance Request

Orchestrates maintenance request through a multi-stage Conductor workflow.

**Input:** `tenantId`, `propertyId`, `description` | **Timeout:** 60s

## Pipeline

```
mtr_submit
    │
mtr_classify
    │
mtr_assign
    │
mtr_complete
    │
mtr_invoice
```

## Workers

**AssignWorker** (`mtr_assign`)

Outputs `technicianId`, `priority`.

**ClassifyWorker** (`mtr_classify`)

Outputs `category`, `priority`.

**CompleteWorker** (`mtr_complete`)

Outputs `laborHours`, `priority`.

**InvoiceWorker** (`mtr_invoice`)

Outputs `totalCost`, `priority`.

**SubmitWorker** (`mtr_submit`)

Outputs `requestId`, `priority`.

## Tests

**10 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
