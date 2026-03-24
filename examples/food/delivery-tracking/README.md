# Delivery Tracking

Orchestrates delivery tracking through a multi-stage Conductor workflow.

**Input:** `orderId`, `restaurantAddr`, `customerAddr` | **Timeout:** 60s

## Pipeline

```
dlt_assign_driver
    │
dlt_pickup
    │
dlt_track
    │
dlt_deliver
    │
dlt_confirm
```

## Workers

**AssignDriverWorker** (`dlt_assign_driver`)

Reads `orderId`. Outputs `driverId`, `name`, `eta`.

**ConfirmWorker** (`dlt_confirm`)

```java
result.addOutputData("delivery", Map.of("orderId", orderId != null ? orderId : "ORD-733", "status", "DELIVERED", "rating", 5));
```

Reads `orderId`. Outputs `delivery`.

**DeliverWorker** (`dlt_deliver`)

Reads `driverId`, `orderId`. Outputs `delivered`, `deliveryTime`.

**PickupWorker** (`dlt_pickup`)

Reads `driverId`, `orderId`. Outputs `pickedUp`, `pickupTime`.

**TrackWorker** (`dlt_track`)

Reads `destination`, `driverId`. Outputs `location`, `eta`.

## Tests

**2 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
