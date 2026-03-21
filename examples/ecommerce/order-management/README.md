# Order Management in Java Using Conductor: Create, Validate, Fulfill, Ship, Deliver

A customer orders a laptop and a USB-C hub. The warehouse picks the laptop but grabs the wrong hub: someone else's return, repackaged with the wrong SKU label. The customer opens the box, finds a used cable they didn't order, and initiates a return. But the returns system doesn't cross-reference the original pick list, so it marks the laptop as returned too and issues a full refund for items the customer still has. One mis-pick cascades into a $2,000 inventory discrepancy that takes three departments a week to untangle. This example uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate each order stage as independent workers, you write the business logic, Conductor handles retries, failure routing, durability, and observability.

## Orders Have Five Stages, Each With Different Failure Modes

An order for 3 items with next-day shipping must flow through creation (assign order number, lock prices), validation (verify inventory, check payment authorization), fulfillment (pick items, pack shipment, generate packing slip), shipping (create label, schedule pickup, trigger tracking), and delivery confirmation (update status, send delivery notification). Each stage involves different systems, the order database, inventory service, warehouse management, carrier API, and notification service.

If the carrier API is down during the shipping step, the order is already packed and waiting: retrying should resume from shipping, not restart fulfillment. If delivery confirmation fails, the shipment is still in transit, the status update should be retried. Every order needs a complete audit trail showing exactly when it moved through each stage.

## The Solution

**You just write the order creation, validation, fulfillment, shipping, and delivery confirmation logic. Conductor handles fulfillment retries, status transition tracking, and order lifecycle audit trails.**

`CreateWorker` initializes the order with a unique ID, line items, pricing, and customer details. `ValidateWorker` verifies item availability, confirms pricing hasn't changed, and validates the shipping address. `FulfillWorker` triggers warehouse operations. Pick list generation, item picking, packing, and quality check. `ShipWorker` creates shipping labels, schedules carrier pickup, and generates tracking numbers. `DeliverWorker` monitors delivery status and sends confirmation notifications upon delivery. Conductor sequences these five stages, retries failed carrier calls without re-packing, and records timestamps for every stage transition.

### What You Write: Workers

Five workers track an order from creation through validation, fulfillment, shipping, and delivery, with each step owning its own state transitions.

| Worker | Task | What It Does |
|---|---|---|
| **CreateOrderWorker** | `ord_create` | Performs the create order operation |
| **DeliverOrderWorker** | `ord_deliver` | Performs the deliver order operation |
| **FulfillOrderWorker** | `ord_fulfill` | Performs the fulfill order operation |
| **ShipOrderWorker** | `ord_ship` | Ships the order |
| **ValidateOrderWorker** | `ord_validate` | Performs the validate order operation |

### The Workflow

```
ord_create
 │
 ▼
ord_validate
 │
 ▼
ord_fulfill
 │
 ▼
ord_ship
 │
 ▼
ord_deliver

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
