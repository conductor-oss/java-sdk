# Goods Receipt

Goods receipt: receive shipment, inspect, match to PO, store, and update inventory.

**Input:** `shipmentId`, `poNumber`, `items` | **Timeout:** 60s

## Pipeline

```
grc_receive
    │
grc_inspect
    │
grc_match_po
    │
grc_store
    │
grc_update_inventory
```

## Workers

**InspectWorker** (`grc_inspect`)

Reads `receivedItems`. Outputs `inspectedItems`, `passed`, `defectRate`.

**MatchPoWorker** (`grc_match_po`)

Reads `poNumber`. Outputs `matched`, `discrepancies`.

**ReceiveWorker** (`grc_receive`)

Reads `items`, `shipmentId`. Outputs `receiptId`, `receivedItems`.

**StoreWorker** (`grc_store`)

Reads `inspectedItems`. Outputs `storedItems`, `location`.

**UpdateInventoryWorker** (`grc_update_inventory`)

```java
int count = storedItems != null ? storedItems.size() : 0;
```

Reads `storedItems`. Outputs `updated`, `itemsUpdated`.

## Tests

**11 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
