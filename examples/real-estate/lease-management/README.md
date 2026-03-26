# Lease Management

Orchestrates lease management through a multi-stage Conductor workflow.

**Input:** `tenantId`, `propertyId`, `action` | **Timeout:** 60s

## Pipeline

```
lse_create
    │
lse_sign
    │
lse_activate
    │
lease_action [SWITCH]
  ├─ renew: lse_renew
  └─ terminate: lse_terminate
```

## Workers

**ActivateLeaseWorker** (`lse_activate`)

Outputs `active`.

**CreateLeaseWorker** (`lse_create`)

Outputs `leaseId`.

**RenewLeaseWorker** (`lse_renew`)

Outputs `renewed`.

**SignLeaseWorker** (`lse_sign`)

Outputs `signed`.

**TerminateLeaseWorker** (`lse_terminate`)

Outputs `terminated`.

## Tests

**10 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
