# Telecom Provisioning

Orchestrates telecom provisioning through a multi-stage Conductor workflow.

**Input:** `customerId`, `serviceType`, `planId` | **Timeout:** 60s

## Pipeline

```
tpv_order
    │
tpv_validate
    │
tpv_configure
    │
tpv_activate
    │
tpv_confirm
```

## Workers

**ActivateWorker** (`tpv_activate`)

Reads `configId`. Outputs `serviceId`, `activatedAt`.

**ConfigureWorker** (`tpv_configure`)

Reads `serviceType`. Outputs `configId`, `bandwidth`.

**ConfirmWorker** (`tpv_confirm`)

Reads `customerId`, `serviceId`. Outputs `provisionStatus`, `confirmed`.

**OrderWorker** (`tpv_order`)

Reads `customerId`. Outputs `orderId`.

**ValidateWorker** (`tpv_validate`)

Reads `orderId`, `planId`. Outputs `valid`, `creditCheck`.

## Tests

**2 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
