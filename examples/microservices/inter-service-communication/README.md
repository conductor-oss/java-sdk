# Inter Service Communication in Java with Conductor

Orchestrates request-response communication between microservices.

## The Problem

Fulfilling a customer order requires coordinating four microservices in sequence: the order service validates the order, the inventory service reserves stock, the shipping service creates a shipment, and the notification service sends the customer a tracking email. Each service depends on the output of the previous one.

Without orchestration, the calling service makes four sequential HTTP calls with bespoke error handling around each one. If the shipping service is down, the order and inventory changes are already committed with no automatic compensation, and there is no single view of the order fulfillment pipeline.

## The Solution

**You just write the order, inventory, shipping, and notification service workers. Conductor handles sequential service coordination, per-call retries with backoff, and end-to-end order traceability.**

Each worker represents a service boundary. Conductor manages cross-service orchestration, compensating transactions, timeout enforcement, and distributed tracing. your workers just make the service calls.

### What You Write: Workers

Four service workers chain together for order fulfillment: OrderServiceWorker validates the order, InventoryServiceWorker reserves stock, ShippingServiceWorker creates a shipment, and NotificationServiceWorker sends tracking to the customer.

| Worker | Task | What It Does |
|---|---|---|
| **InventoryServiceWorker** | `isc_inventory_service` | Reserves items for the order in the nearest warehouse, returning the warehouse ID. |
| **NotificationServiceWorker** | `isc_notification_service` | Sends the tracking information to the customer via email. |
| **OrderServiceWorker** | `isc_order_service` | Validates and processes the incoming order, returning an order reference number. |
| **ShippingServiceWorker** | `isc_shipping_service` | Creates a shipment from the assigned warehouse and returns a tracking ID and ETA. |

the workflow coordination stays the same.

### The Workflow

```
isc_order_service
 │
 ▼
isc_inventory_service
 │
 ▼
isc_shipping_service
 │
 ▼
isc_notification_service

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
