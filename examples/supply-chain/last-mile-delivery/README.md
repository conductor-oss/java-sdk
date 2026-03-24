# Last Mile Delivery

Last mile delivery: assign driver, optimize route, deliver, and confirm.

**Input:** `orderId`, `address`, `timeWindow` | **Timeout:** 60s

## Pipeline

```
lmd_assign_driver
    │
lmd_optimize_route
    │
lmd_deliver
    │
lmd_confirm
```

## Workers

**AssignDriverWorker** (`lmd_assign_driver`)

Reads `orderId`. Outputs `driverId`, `driverName`.

**ConfirmWorker** (`lmd_confirm`)

Reads `deliveryStatus`, `orderId`. Outputs `confirmed`, `customerNotified`.

**DeliverWorker** (`lmd_deliver`)

Reads `driverId`, `orderId`. Outputs `status`, `photoProof`.

**OptimizeRouteWorker** (`lmd_optimize_route`)

Reads `address`, `driverId`. Outputs `route`, `estimatedTime`, `distance`.

## Tests

**8 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
