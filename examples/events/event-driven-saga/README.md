# Event Driven Saga in Java Using Conductor

A customer places an order. Your service creates the order record, charges their credit card, and then, the shipping service is down. The payment went through, the order shows "confirmed," but nothing ships. Three days later the customer calls asking where their package is. You check the logs: the shipping call threw a connection timeout, the catch block logged a warning, and nobody ever refunded the charge or cancelled the order. You now have a paid, confirmed order that will never ship, and no automated way to unwind it. This workflow implements the saga pattern: if any step fails, compensation runs automatically to cancel the order and refund the payment. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers, you write the business logic, Conductor handles retries, failure routing, durability, and observability.

## The Problem

You need to coordinate a distributed transaction across order creation, payment processing, and shipping. If payment succeeds, the order ships. If payment fails, you must run compensation logic to cancel the order and release any reserved resources. This is the saga pattern, a sequence of local transactions with compensating actions for failures. Without proper compensation, you end up with paid orders that never ship or shipped orders that were never paid for.

Without orchestration, you'd implement the saga with nested try/catch blocks, manually calling compensation methods on failure, hoping the compensation itself does not fail, and logging every branch to debug inconsistent state across services.

## The Solution

**You just write the order-creation, payment, shipping, and compensation workers. Conductor handles SWITCH-based compensation routing, guaranteed saga completion, and a full audit trail of every saga step and rollback.**

Each saga step is a simple, independent worker, a plain Java class that does one thing. Conductor takes care of executing the happy path (order, payment, shipping), routing via a SWITCH task to compensation on payment failure, retrying transient failures before triggering compensation, and tracking the entire saga with full audit trail.

### What You Write: Workers

Five workers implement the saga: CreateOrderWorker starts the order, ProcessPaymentWorker charges the customer, ShipOrderWorker dispatches the shipment, while CancelOrderWorker and CompensatePaymentWorker handle rollback when payment fails.

| Worker | Task | What It Does |
|---|---|---|
| **CancelOrderWorker** | `ds_cancel_order` | Cancels an order as part of saga compensation. |
| **CompensatePaymentWorker** | `ds_compensate_payment` | Compensates (refunds) a payment when the saga needs to roll back. |
| **CreateOrderWorker** | `ds_create_order` | Creates an order in the saga. |
| **ProcessPaymentWorker** | `ds_process_payment` | Processes payment for an order. |
| **ShipOrderWorker** | `ds_ship_order` | Ships an order after successful payment. |

Workers implement event processing with realistic payloads so you can trace the full event flow without external message brokers. Replace the simulation with real event sources, the workflow and routing logic stay the same.

### The Workflow

```
ds_create_order
 │
 ▼
ds_process_payment
 │
 ▼
SWITCH (switch_ref)
 ├── success: ds_ship_order
 ├── failed: ds_compensate_payment -> ds_cancel_order

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
