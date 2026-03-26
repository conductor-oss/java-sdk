# Shipping Workflow

Shipping workflow: select carrier, create label, track, deliver, confirm

**Input:** `orderId`, `weight`, `dimensions`, `origin`, `destination`, `shippingSpeed` | **Timeout:** 60s

## Pipeline

```
shp_select_carrier
    │
shp_create_label
    │
shp_track
    │
shp_deliver
    │
shp_confirm
```

## Workers

**ConfirmDeliveryWorker** (`shp_confirm`)

Reads `orderId`, `trackingNumber`. Outputs `confirmed`, `notificationSent`.

**CreateLabelWorker** (`shp_create_label`)

```java
String prefix = "FedEx".equals(carrier) ? "FX" : "US";
```

Reads `carrier`, `serviceType`. Outputs `trackingNumber`, `labelUrl`.

**DeliverShipmentWorker** (`shp_deliver`)

Reads `destination`, `trackingNumber`. Outputs `delivered`, `deliveredAt`, `signedBy`.

**SelectCarrierWorker** (`shp_select_carrier`)

Reads `speed`, `weight`. Outputs `carrier`, `serviceType`, `cost`.

**TrackShipmentWorker** (`shp_track`)

Reads `trackingNumber`. Outputs `events`, `currentStatus`.

## Tests

**13 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
