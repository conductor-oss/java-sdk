# Food Ordering in Java with Conductor

Processes a food order from menu browsing through payment, kitchen preparation, and delivery. ## The Problem

You need to process a food order from browsing to delivery. The customer browses the restaurant's menu, places an order with selected items, pays for the order, the kitchen prepares the food, and it is delivered (or picked up). Each step depends on the previous. you cannot prepare food without a confirmed, paid order; you cannot deliver without prepared food.

Without orchestration, you'd build a monolithic ordering app that handles menu display, cart management, payment processing, kitchen dispatch, and delivery coordination in one service. manually managing order state through each phase, retrying failed payment attempts, and handling the handoff between payment confirmation and kitchen preparation.

## The Solution

**You just write the menu selection, payment processing, kitchen preparation, and delivery dispatch logic. Conductor handles payment retries, kitchen dispatch sequencing, and order lifecycle tracking.**

Each ordering concern is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of executing them in order (browse, order, pay, prepare, deliver), retrying if the payment gateway is temporarily unavailable, tracking every order from menu to doorstep, and resuming from the last step if the process crashes. ### What You Write: Workers

Menu browsing, cart assembly, payment, and kitchen dispatch workers handle the ordering flow as independent, loosely coupled services.

| Worker | Task | What It Does |
|---|---|---|
| **BrowseWorker** | `fod_browse` | Browses the restaurant menu and returns selected items with names, prices, and quantities |
| **DeliverWorker** | `fod_deliver` | Delivers the order and returns delivery status with ETA |
| **OrderWorker** | `fod_order` | Places the order for the customer and returns an order ID, total, and item count |
| **PayWorker** | `fod_pay` | Processes payment for the order total and returns a transaction ID |
| **PrepareWorker** | `fod_prepare` | Prepares the order in the kitchen and returns prep time and readiness status |

### The Workflow

```
fod_browse
 │
 ▼
fod_order
 │
 ▼
fod_pay
 │
 ▼
fod_prepare
 │
 ▼
fod_deliver

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
