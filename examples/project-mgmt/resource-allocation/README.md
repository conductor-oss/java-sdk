# Resource Allocation

Resource allocation: assess demand, check capacity, allocate, confirm.

**Input:** `projectId`, `resourceType`, `hoursNeeded` | **Timeout:** 60s

## Pipeline

```
ral_assess_demand
    │
ral_check_capacity
    │
ral_allocate
    │
ral_confirm
```

## Workers

**AllocateWorker** (`ral_allocate`)

Reads `projectId`. Outputs `allocation`.

**AssessDemandWorker** (`ral_assess_demand`)

Reads `hoursNeeded`, `projectId`. Outputs `demand`.

**CheckCapacityWorker** (`ral_check_capacity`)

Reads `resourceType`. Outputs `available`.

**ConfirmWorker** (`ral_confirm`)

Outputs `confirmed`.

## Tests

**8 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
