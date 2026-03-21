# Event Driven Workflow in Java Using Conductor

Event-driven workflow that receives events, classifies them by type, and routes to the appropriate handler via a SWITCH task. ## The Problem

You need to route incoming events to different handlers based on their type. When events arrive, each one must be classified (user event, order event, system event, etc.) and dispatched to the handler that knows how to process that specific type. Sending an order event to the user handler produces incorrect results; dropping an unknown event type loses data.

Without orchestration, you'd build an event dispatcher with a switch statement or handler registry, manually routing events by type, handling unknown types with fallback logic, and logging every routing decision to debug misrouted events.

## The Solution

**You just write the event-receiver, classifier, and type-specific handler workers. Conductor handles type-based SWITCH routing, per-handler retries, and full event classification and processing history.**

Each event type handler is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of receiving the event, classifying it, routing via a SWITCH task to the correct handler, retrying if the handler fails, and tracking every event's routing and processing. ### What You Write: Workers

Five workers route events by type: ReceiveEventWorker ingests events, ClassifyEventWorker determines the category, then HandleOrderWorker, HandlePaymentWorker, or HandleGenericWorker processes the event based on its classification.

| Worker | Task | What It Does |
|---|---|---|
| **ClassifyEventWorker** | `ed_classify_event` | Classifies an event by its type into a category and priority. |
| **HandleGenericWorker** | `ed_handle_generic` | Handles generic (unclassified) events. |
| **HandleOrderWorker** | `ed_handle_order` | Handles order-related events. |
| **HandlePaymentWorker** | `ed_handle_payment` | Handles payment-related events. |
| **ReceiveEventWorker** | `ed_receive_event` | Receives an incoming event and enriches it with metadata. |

Workers implement event processing with realistic payloads so you can trace the full event flow without external message brokers. Replace the simulation with real event sources. the workflow and routing logic stay the same.

### The Workflow

```
ed_receive_event
 │
 ▼
ed_classify_event
 │
 ▼
SWITCH (category_switch_ref)
 ├── order: ed_handle_order
 ├── payment: ed_handle_payment
 └── default: ed_handle_generic

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
