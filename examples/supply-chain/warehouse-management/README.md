# Warehouse Management

Warehouse management: receive, put away, pick, pack, and ship.

**Input:** `orderId`, `items`, `shippingMethod` | **Timeout:** 60s

## Pipeline

```
wm_receive
    │
wm_put_away
    │
wm_pick
    │
wm_pack
    │
wm_ship
```

## Workers

**PackWorker** (`wm_pack`)

```java
int count = items != null ? items.size() : 0;
```

Reads `pickedItems`. Outputs `packageId`, `weight`.

**PickWorker** (`wm_pick`)

Reads `locations`, `orderId`. Outputs `pickedItems`, `pickTime`.

**PutAwayWorker** (`wm_put_away`)

Reads `receivedItems`. Outputs `locations`.

**ReceiveWorker** (`wm_receive`)

Reads `items`, `orderId`. Outputs `receivedItems`.

**ShipWorker** (`wm_ship`)

Reads `packageId`, `shippingMethod`. Outputs `trackingNumber`, `carrier`.

## Tests

**10 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
