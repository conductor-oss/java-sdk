# Event Choreography in Java Using Conductor

Choreography pattern: services communicate through events with no central orchestrator. Each service emits an event that triggers the next service. ## The Problem

You need to coordinate an order fulfillment flow where each service communicates through events. The order service creates an order and emits an event, the payment service processes payment and emits a confirmation event, the inventory service reserves stock and emits a shipment event, and the notification service sends a confirmation to the customer. Each service reacts to the previous service's event rather than being called directly.

Without orchestration, you'd implement this as a pure event-driven choreography with each service subscribing to topics; but then you lose visibility into the end-to-end flow, cannot easily trace a single order across services, have no centralized retry mechanism when payment fails, and must build custom tooling to debug stuck orders.

## The Solution

**You just write the order, payment, inventory, notification, and event-emission workers. Conductor handles event-driven sequencing with full end-to-end traceability, per-service retries, and centralized monitoring of the entire order flow.**

Each service and its event emission is a simple, independent worker. a plain Java class that does one thing. Conductor gives you choreography-style decoupling (each worker only knows its own concern) with orchestration benefits (full traceability, automatic retries, centralized monitoring). You get the best of both patterns without writing event routing code.

### What You Write: Workers

Seven workers model choreography with traceability: OrderServiceWorker, PaymentServiceWorker, and InventoryServiceWorker handle domain logic, while EmitOrderEventWorker, EmitPaymentEventWorker, and EmitInventoryEventWorker publish domain events, and NotificationServiceWorker sends the customer confirmation.

| Worker | Task | What It Does |
|---|---|---|
| **EmitInventoryEventWorker** | `ch_emit_inventory_event` | Emits an inventory event to the event bus. |
| **EmitOrderEventWorker** | `ch_emit_order_event` | Emits an order event to the event bus. |
| **EmitPaymentEventWorker** | `ch_emit_payment_event` | Emits a payment event to the event bus. |
| **InventoryServiceWorker** | `ch_inventory_service` | Reserves inventory items for an order. |
| **NotificationServiceWorker** | `ch_notification_service` | Sends a notification to the customer. |
| **OrderServiceWorker** | `ch_order_service` | Processes an incoming order and calculates the total amount. |
| **PaymentServiceWorker** | `ch_payment_service` | Processes payment for an order. |

Workers implement event processing with realistic payloads so you can trace the full event flow without external message brokers. Replace the simulation with real event sources. the workflow and routing logic stay the same.

### The Workflow

```
ch_order_service
 │
 ▼
ch_emit_order_event
 │
 ▼
ch_payment_service
 │
 ▼
ch_emit_payment_event
 │
 ▼
ch_inventory_service
 │
 ▼
ch_emit_inventory_event
 │
 ▼
ch_notification_service

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
