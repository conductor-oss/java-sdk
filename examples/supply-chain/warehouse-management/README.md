# Warehouse Management in Java with Conductor : Receiving, Putaway, Picking, Packing, and Shipping

## The Problem

You need to manage the flow of goods through your warehouse from dock to door. Inbound shipments must be received, inspected, and logged. Received items must be put away to optimal storage locations based on product type, pick frequency, and available capacity. When outbound orders come in, items must be picked from the correct bins in the right sequence to minimize picker travel distance. Picked items must be packed into the right box size with appropriate dunnage. Finally, packed orders must be shipped via the customer's selected method with correct labels and documentation.

Without orchestration, each step is managed by a different warehouse associate with clipboards or handheld scanners that don't talk to each other. If putaway assigns a bin that's already full, the picker doesn't find the item and the order ships late. If packing completes but the shipping label printer is offline, packed boxes sit on the dock without tracking numbers. There is no end-to-end visibility into where an order is in the fulfillment process.

## The Solution

**You just write the warehouse workers. Receiving, putaway, picking, packing, and shipping. Conductor handles step sequencing, shipping label retries, and full order visibility for warehouse performance analytics.**

Each step of the warehouse flow is a simple, independent worker. a plain Java class that does one thing. Conductor sequences them so goods are received before putaway, putaway completes before items are available for picking, picking drives packing, and packing drives shipping. If the shipping label API times out, Conductor retries without re-packing the order. Every receiving record, putaway location, pick confirmation, pack verification, and ship confirmation is tracked for order visibility and warehouse performance analytics.

### What You Write: Workers

Five workers manage dock-to-door operations: ReceiveWorker logs inbound goods, PutAwayWorker assigns optimal bins, PickWorker retrieves items for orders, PackWorker prepares shipments, and ShipWorker generates tracking labels.

| Worker | Task | What It Does |
|---|---|---|
| **PackWorker** | `wm_pack` | Packs picked items into the appropriate box size with dunnage and labels. |
| **PickWorker** | `wm_pick` | Picks items from storage bins in optimized sequence to fulfill outbound orders. |
| **PutAwayWorker** | `wm_put_away` | Assigns optimal storage locations and puts away received items based on product type and pick frequency. |
| **ReceiveWorker** | `wm_receive` | Receives inbound goods at the dock, inspects, and logs the receipt. |
| **ShipWorker** | `wm_ship` | Ships packed orders via the selected carrier with tracking numbers and documentation. |

### The Workflow

```
wm_receive
 │
 ▼
wm_put_away
 │
 ▼
wm_pick
 │
 ▼
wm_pack
 │
 ▼
wm_ship

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
