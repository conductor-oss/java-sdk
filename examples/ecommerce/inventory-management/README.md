# Inventory Management in Java Using Conductor: Check Stock, Reserve, Update, Reorder

It's Black Friday. Your product page shows 500 units of the hot new headphones "in stock"; but that number is a lie. The website sees 500, the Amazon channel sees 500, and the outlet app sees 500, because all three read from a cache that hasn't synced with the warehouse. Physically, 200 units sit on the shelf. By noon, you've sold 800 headphones you don't have, customer service is fielding cancellation calls, and your supplier can't restock for three weeks. This example uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate atomic stock checks, reservations, updates, and reorders as independent workers. You write the business logic, Conductor handles retries, failure routing, durability, and observability.

## Inventory Must Be Accurate, Atomic, and Proactive

A customer orders 5 units of SKU-12345. The warehouse has 12 in stock with a reorder threshold of 10. The system must check that 5 are available (not already reserved by other orders), reserve exactly 5 (atomically.; no double-reserving), update the available quantity to 7, and trigger a reorder because 7 is below the threshold of 10.

Inventory operations must be atomic: if two orders for 8 units arrive simultaneously against 12 in stock, only one should succeed. . Not both. If the update step fails after reservation, the reserved quantity must be rolled back. And reorder decisions must be based on the updated quantity, not the pre-reservation quantity. Without orchestration, these race conditions and consistency requirements lead to overselling, stockouts, and phantom inventory.

## The Solution

**You just write the stock check, reservation, inventory update, and reorder logic. Conductor handles reservation retries, reorder sequencing, and stock level tracking across warehouses.**

`CheckStockWorker` queries the current available quantity for the SKU at the specified warehouse, accounting for existing reservations. `ReserveWorker` atomically reserves the requested quantity. Failing if insufficient stock is available. `UpdateWorker` decrements the available inventory by the reserved amount and records the transaction. `ReorderWorker` checks the updated quantity against the reorder threshold and triggers a purchase order if stock is low, calculating the reorder quantity based on demand forecasts. Conductor chains these four steps, retries failed updates without double-reserving, and records every inventory movement for audit.

### What You Write: Workers

Stock checking, reservation, inventory updates, and reorder workers manage warehouse state through discrete, independently testable operations.

| Worker | Task | What It Does |
|---|---|---|
| **CheckStockWorker** | `inv_check_stock` | Performs the check stock operation |
| **ReorderWorker** | `inv_reorder` | Performs the reorder operation |
| **ReserveStockWorker** | `inv_reserve` | Performs the reserve stock operation |
| **UpdateInventoryWorker** | `inv_update` | Performs the update inventory operation |

### The Workflow

```
inv_check_stock
 │
 ▼
inv_reserve
 │
 ▼
inv_update
 │
 ▼
inv_reorder

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
