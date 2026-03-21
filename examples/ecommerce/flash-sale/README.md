# Flash Sale in Java Using Conductor : Prepare Inventory, Open Sale, Process Orders, Close, Report

Flash sale: prepare inventory, open sale, process orders, close, report. ## Flash Sales Need Precise Timing and Inventory Control

A 2-hour flash sale on 500 units at 60% off generates a traffic spike. The system must prepare inventory (reserve 500 units from general stock), open the sale at exactly the scheduled time (not a second early), process orders atomically (decrement inventory, no overselling), close when time expires or inventory hits zero, and produce a report showing units sold, revenue, peak order rate, and customer distribution.

Overselling is the cardinal sin. selling 520 units when only 500 exist creates fulfillment nightmares. Underselling (closing too early) leaves money on the table. The order processing step must be atomic: check inventory, decrement, and confirm in a single operation. If the ordering system crashes mid-sale, it must resume with the correct inventory count, not start over or lose orders.

## The Solution

**You just write the inventory preparation, sale activation, order processing, and reporting logic. Conductor handles concurrent order processing, inventory locking, and sale event audit trails.**

`PrepareInventoryWorker` reserves the sale quantity from general inventory, sets quantity caps, and configures per-customer purchase limits. `OpenSaleWorker` activates the sale at the scheduled time, making the discounted items available for purchase. `ProcessOrdersWorker` handles incoming orders with atomic inventory decrement. checking availability, reserving quantity, and confirming each order without overselling. `CloseSaleWorker` ends the sale when time expires or inventory is exhausted, returning unsold units to general inventory. `ReportWorker` generates the sales report, units sold, revenue, average order value, peak order rate, and customer demographics. Conductor sequences these stages and tracks the entire sale lifecycle.

### What You Write: Workers

Inventory preparation, sale activation, order processing, and reporting workers handle the lifecycle of a time-limited sale event independently.

| Worker | Task | What It Does |
|---|---|---|
| **CloseSaleWorker** | `fls_close_sale` | Flash sale closed - |
| **OpenSaleWorker** | `fls_open_sale` | Performs the open sale operation |
| **PrepareInventoryWorker** | `fls_prepare_inventory` | Reserving inventory for \ |
| **ProcessOrdersWorker** | `fls_process_orders` | Performs the process orders operation |
| **ReportWorker** | `fls_report` | Performs the report operation |

### The Workflow

```
fls_prepare_inventory
 │
 ▼
fls_open_sale
 │
 ▼
fls_process_orders
 │
 ▼
fls_close_sale
 │
 ▼
fls_report

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
