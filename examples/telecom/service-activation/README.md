# Service Activation

Orchestrates service activation through a multi-stage Conductor workflow.

**Input:** `orderId`, `customerId`, `serviceType` | **Timeout:** 60s

## Pipeline

```
sac_validate_order
    │
sac_provision
    │
sac_test
    │
sac_activate
    │
sac_notify
```

## Workers

**ActivateWorker** (`sac_activate`)

Reads `serviceId`. Outputs `activated`, `activatedAt`.

**NotifyWorker** (`sac_notify`)

Reads `customerId`. Outputs `notified`.

**ProvisionWorker** (`sac_provision`)

Reads `serviceType`. Outputs `serviceId`, `endpoint`.

**TestWorker** (`sac_test`)

Reads `serviceId`. Outputs `passed`, `latency`, `bandwidth`.

**ValidateOrderWorker** (`sac_validate_order`)

Reads `orderId`. Outputs `valid`, `paymentVerified`.

## Tests

**2 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
