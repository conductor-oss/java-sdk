# Event Correlation in Java Using Conductor

Event Correlation. init correlation session, fork to receive order/payment/shipping events in parallel, join, correlate, and process.

## The Problem

You need to correlate related events that arrive independently from different sources. An order event, a payment event, and a shipping event may arrive at different times from different services, but they all belong to the same business transaction. The workflow must initialize a correlation session, receive all expected events (potentially in parallel), correlate them by matching fields, and process the fully correlated result.

Without orchestration, you'd build a stateful correlation engine with in-memory maps keyed by correlation ID, timeout logic for events that never arrive, manual cleanup of stale sessions, and complex multi-threaded code to handle events arriving in any order. all while hoping state is not lost on restarts.

## The Solution

**You just write the correlation-init, event-receiver, event-correlation, and processing workers. Conductor handles parallel event reception, durable session state, and automatic join before correlation.**

Each correlation concern is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of initializing the session, receiving events in parallel via FORK_JOIN, correlating them after all arrive, and processing the result, with durable state that survives restarts and full visibility into which events have arrived.

### What You Write: Workers

Six workers correlate cross-service events: InitCorrelationWorker opens a session, ReceiveOrderWorker, ReceivePaymentWorker, and ReceiveShippingWorker collect events in parallel via FORK_JOIN, CorrelateEventsWorker matches them, and ProcessCorrelatedWorker acts on the unified result.

| Worker | Task | What It Does |
|---|---|---|
| **CorrelateEventsWorker** | `ec_correlate_events` | Correlates order, payment, and shipping events into a unified data set. Returns deterministic output with fixed match... |
| **InitCorrelationWorker** | `ec_init_correlation` | Initializes a correlation session with a fixed correlation ID and timestamp. Returns deterministic output with no ran... |
| **ProcessCorrelatedWorker** | `ec_process_correlated` | Processes correlated event data and determines the action to take. Returns deterministic output with fixed timestamps. |
| **ReceiveOrderWorker** | `ec_receive_order` | Simulates receiving an order event for event correlation. Returns deterministic, fixed order data. |
| **ReceivePaymentWorker** | `ec_receive_payment` | Simulates receiving a payment event for event correlation. Returns deterministic, fixed payment data. |
| **ReceiveShippingWorker** | `ec_receive_shipping` | Simulates receiving a shipping event for event correlation. Returns deterministic, fixed shipping data. |

Workers implement event processing with realistic payloads so you can trace the full event flow without external message brokers. Replace the simulation with real event sources. the workflow and routing logic stay the same.

### The Workflow

```
ec_init_correlation
 │
 ▼
FORK_JOIN
 ├── ec_receive_order
 ├── ec_receive_payment
 └── ec_receive_shipping
 │
 ▼
JOIN (wait for all branches)
ec_correlate_events
 │
 ▼
ec_process_correlated

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
