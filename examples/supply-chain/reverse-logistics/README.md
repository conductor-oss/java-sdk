# Reverse Logistics in Java with Conductor : Return Receipt, Condition Inspection, Refurbish/Recycle/Dispose Routing, and Processing

## The Problem

You need to handle product returns efficiently and recover maximum value. When a customer returns wireless headphones with a defective speaker (return ID RET-2024-669), the item must be received at the returns center, inspected to determine condition (cosmetic damage only? functional defect? beyond repair?), and routed to the appropriate disposition: refurbishment if repairable, recycling if materials can be recovered, or disposal if neither is viable. The final processing step updates inventory, triggers the customer refund, and records the disposition for sustainability reporting.

Without orchestration, returns pile up at the dock waiting for inspection. Inspectors make disposition decisions with no consistent criteria, and refurbishable items end up in the recycle bin because there is no routing logic. Processing refunds is disconnected from the disposition decision, so customers wait weeks for refunds on items that were scrapped on day one. There is no visibility into recovery rates or disposition mix.

## The Solution

**You just write the returns workers. Receipt logging, condition inspection, refurbish/recycle/dispose routing, and processing. Conductor handles SWITCH-based disposition routing, refurbishment retries, and disposition records for sustainability reporting.**

Each step of the reverse logistics process is a simple, independent worker. a plain Java class that does one thing. Conductor sequences them so returns are received before inspection, inspection results drive the SWITCH task that routes to refurbish, recycle, or dispose, and final processing runs regardless of which disposition path was taken. If the refurbishment worker fails, Conductor retries without re-inspecting the item. Every receipt, inspection result, disposition decision, and processing action is recorded for return analytics and sustainability reporting.

### What You Write: Workers

Six workers handle returns end-to-end: ReceiveReturnWorker logs the return, InspectWorker assesses condition, RefurbishWorker repairs salvageable items, RecycleWorker recovers materials, DisposeWorker handles waste, and ProcessWorker triggers refunds and inventory updates.

| Worker | Task | What It Does |
|---|---|---|
| **DisposeWorker** | `rvl_dispose` | Disposes of items that cannot be refurbished or recycled, with proper waste handling. |
| **InspectWorker** | `rvl_inspect` | Inspects the returned product's condition. cosmetic damage, functional defect, or beyond repair. |
| **ProcessWorker** | `rvl_process` | Processes the return. updates inventory, triggers the customer refund, and records the disposition. |
| **ReceiveReturnWorker** | `rvl_receive_return` | Receives the returned product at the returns center and logs the return ID. |
| **RecycleWorker** | `rvl_recycle` | Recovers materials from items that cannot be refurbished but have recyclable components. |
| **RefurbishWorker** | `rvl_refurbish` | Refurbishes repairable items for resale or warranty replacement. |

### The Workflow

```
rvl_receive_return
 │
 ▼
rvl_inspect
 │
 ▼
SWITCH (rvl_switch_ref)
 ├── refurbish: rvl_refurbish
 ├── recycle: rvl_recycle
 └── default: rvl_dispose
 │
 ▼
rvl_process

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
