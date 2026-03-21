# Goods Receipt in Java with Conductor : Shipment Receiving, Quality Inspection, PO Matching, Warehouse Storage, and Inventory Update

## The Problem

You need to process inbound shipments at your warehouse receiving dock. When shipment SHP-2024-655 arrives, the dock team must log receipt against purchase order PO-654-001, inspect the goods for damage and spec compliance, verify that received quantities match the PO line items (did we get 5,000 bolts or only 4,800?), assign bin locations for putaway, and update the inventory management system so the stock is available for picking. If the PO match step reveals a shortage, procurement needs to be notified to arrange a replacement shipment.

Without orchestration, the dock team fills out paper receiving forms, a clerk manually enters quantities into the ERP, and inventory updates happen hours later in a batch job. Discrepancies between received and ordered quantities go unnoticed until someone tries to pick the missing items. If the ERP update fails, the warehouse shows phantom stock and downstream orders get allocated against inventory that doesn't exist.

## The Solution

**You just write the receiving workers. Dock receipt, quality inspection, PO matching, bin assignment, and inventory update. Conductor handles sequential gating, automatic retries on ERP failures, and three-way match records for audit compliance.**

Each step of the goods receipt process is a simple, independent worker. a plain Java class that does one thing. Conductor sequences them so receiving logs are created before inspection, inspection results gate PO matching, matched goods are stored before inventory is updated, and inventory records only reflect items that passed inspection. If the inventory update worker fails, Conductor retries it without re-inspecting the entire shipment. Every receiving record, inspection result, PO match, storage assignment, and inventory adjustment is tracked for three-way matching and audit compliance.

### What You Write: Workers

Five workers handle the receiving dock workflow: ReceiveWorker logs inbound shipments, InspectWorker checks quality, MatchPoWorker verifies quantities against the purchase order, StoreWorker assigns bin locations, and UpdateInventoryWorker makes stock available for picking.

| Worker | Task | What It Does |
|---|---|---|
| **InspectWorker** | `grc_inspect` | Inspects received items for damage and spec compliance. |
| **MatchPoWorker** | `grc_match_po` | Matches received quantities against the purchase order line items. |
| **ReceiveWorker** | `grc_receive` | Logs shipment receipt at the dock with item details. |
| **StoreWorker** | `grc_store` | Assigns storage bin locations and records putaway for inspected and matched goods. |
| **UpdateInventoryWorker** | `grc_update_inventory` | Updates the inventory management system so received stock is available for picking. |

### The Workflow

```
grc_receive
 │
 ▼
grc_inspect
 │
 ▼
grc_match_po
 │
 ▼
grc_store
 │
 ▼
grc_update_inventory

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
