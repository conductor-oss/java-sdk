# Inventory Management

Inventory management workflow: check stock, reserve, update, reorder if low

**Input:** `sku`, `requestedQty`, `warehouseId`, `reorderThreshold` | **Timeout:** 60s

## Pipeline

```
inv_check_stock
    │
inv_reserve
    │
inv_update
    │
inv_reorder
```

## Workers

**CheckStockWorker** (`inv_check_stock`): Checks real stock levels from the thread-safe InventoryStore.

- Queries the InventoryStore for current available quantity
- Checks stock across the specific warehouse and total across all warehouses


Reads `sku`, `warehouseId`. Outputs `availableQty`, `totalStockAllWarehouses`, `warehouseId`, `location`, `lowStock`.

**InventoryStore** (`InventoryStore`)


**ReorderWorker** (`inv_reorder`): Evaluates whether a reorder is needed and generates a purchase order.

- Compares remaining quantity against the reorder threshold
- Calculates reorder quantity using Economic Order Quantity (EOQ) heuristic:

- `remaining <= 0` &rarr; `"rush"`


Reads `remainingQty`, `reorderThreshold`, `sku`. Outputs `reorderPlaced`, `reorderQty`, `poNumber`, `priority`, `estimatedArrival`.

**ReserveStockWorker** (`inv_reserve`): Reserves stock using CAS (compare-and-swap) for thread safety.

- Uses InventoryStore.reserve() which performs atomic CAS operations
- If full quantity is not available, reserves as much as possible


Reads `availableQty`, `requestedQty`, `sku`, `warehouseId`. Outputs `reserved`, `reservedQty`, `reservationId`, `requestedQty`, `warehouseId`.

**UpdateInventoryWorker** (`inv_update`): Updates inventory after a reservation is fulfilled. Confirms the deduction.

- Verifies the reservation exists and is valid
- Marks the reservation as fulfilled in the InventoryStore


Reads `previousQty`, `reservationId`, `reservedQty`, `sku`, `warehouseId`. Outputs `remainingQty`, `previousQty`, `deductedQty`, `delta`, `verified`.

## Tests

**29 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
