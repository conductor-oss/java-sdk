# Choreography Vs Orchestration in Java with Conductor

Service A publishes an `order.created` event. Service B picks it up and reserves inventory. Service C is supposed to process the payment; but someone on the payments team renamed the topic from `order.created` to `orders.new` three sprints ago, and nobody updated the documentation. So Service C never fires. The order sits in "reserved" limbo. There's no central view of the flow, no shared transaction ID, and debugging means correlating logs across four services to discover that the event your payment service is listening for simply doesn't exist anymore. This example replaces invisible choreography with explicit Conductor orchestration: place order, reserve inventory, process payment, ship: each step visible, traceable, and retriable from a single execution view. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers, you write the business logic, Conductor handles retries, failure routing, durability, and observability.

## The Problem

An e-commerce order involves placing the order, reserving inventory, processing payment, and shipping. Four services that must coordinate. In a choreography approach, each service emits events and the next service reacts, but the overall flow is invisible and failures are hard to trace. Orchestration makes the flow explicit: Conductor drives each step, passes data between services, and provides a single execution view.

Without orchestration (pure choreography), you lose visibility into the end-to-end order flow. If the payment service silently drops an event, the order is stuck with no alert. Debugging requires correlating logs across four services with no shared transaction ID.

## The Solution

**You just write the order, inventory, payment, and shipping workers. Conductor handles cross-service sequencing, compensating retries, and end-to-end order traceability.**

Each worker represents a service boundary. Conductor manages cross-service orchestration, compensating transactions, timeout enforcement, and distributed tracing. Your workers just make the service calls.

### What You Write: Workers

The order flow chains four domain workers: PlaceOrderWorker creates the order, ReserveInventoryWorker holds stock, ProcessPaymentWorker charges the customer, and ShipOrderWorker dispatches the shipment.

| Worker | Task | What It Does |
|---|---|---|
| **PlaceOrderWorker** | `cvo_place_order` | Creates the order record with items and computes the order total. |
| **ProcessPaymentWorker** | `cvo_process_payment` | Charges the order total to the customer's payment method. |
| **ReserveInventoryWorker** | `cvo_reserve_inventory` | Reserves the ordered items in the warehouse and returns the warehouse ID for shipping. |
| **ShipOrderWorker** | `cvo_ship_order` | Creates a shipment from the assigned warehouse and returns a tracking ID. |

### The Workflow

```
cvo_place_order
 │
 ▼
cvo_reserve_inventory
 │
 ▼
cvo_process_payment
 │
 ▼
cvo_ship_order

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
