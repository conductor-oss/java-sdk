# Event Routing in Java Using Conductor

An order-cancellation event lands in the user-profile handler. The handler doesn't know what to do with it, silently drops it, and the customer's order stays active. Meanwhile, a user-signup event hits the order processor, which tries to look up a nonexistent order ID and throws a NullPointerException, taking down the entire event consumer. You restart the consumer, but the misrouted events are gone. When every event flows through the same pipe with a big `if/else` block deciding where it goes, one wrong routing decision cascades into data corruption, silent failures, and lost events. This workflow extracts each event's domain, routes it to the correct processor via a SWITCH task, and gives you a full trace of every routing decision. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers, you write the business logic, Conductor handles retries, failure routing, durability, and observability.

## The Problem

You need to route incoming events to domain-specific processors based on the event's domain. User events go to the user processor, order events go to the order processor, and system events go to the system processor. Each domain has different processing logic, different downstream systems, and different SLAs. Routing an event to the wrong domain processor produces incorrect results or data corruption.

Without orchestration, you'd build a routing table with a switch statement or map lookup, manually dispatching events to different services, handling unknown domains with fallback logic, and logging every routing decision to debug misrouted events.

## The Solution

**You just write the event-receive, type-extraction, and domain-specific processor workers. Conductor handles domain-based SWITCH routing, per-processor retries, and full traceability of every routing decision.**

Each domain processor is a simple, independent worker, a plain Java class that does one thing. Conductor takes care of receiving the event, extracting its domain, routing via a SWITCH task to the correct processor (user, order, system), retrying if the processor fails, and tracking every event's routing and outcome.

### What You Write: Workers

Five workers implement domain-based routing: ReceiveEventWorker ingests the event, ExtractTypeWorker parses the domain and sub-type, then UserProcessorWorker, OrderProcessorWorker, or SystemProcessorWorker handles it based on the extracted domain.

| Worker | Task | What It Does |
|---|---|---|
| **ExtractTypeWorker** | `eo_extract_type` | Splits the eventDomain string by "." to extract the domain (first part) and subType (remaining parts joined by "."). |
| **OrderProcessorWorker** | `eo_order_processor` | Processes order-domain events. Extracts the orderId from eventData and returns a result indicating fulfillment |
| **ReceiveEventWorker** | `eo_receive_event` | Receives an incoming event and passes through its domain and data, stamping a receivedAt timestamp. |
| **SystemProcessorWorker** | `eo_system_processor` | Default processor for events that do not match user or order domains. Passes through the domain and marks the event a |
| **UserProcessorWorker** | `eo_user_processor` | Processes user-domain events. Returns a result indicating the user event was handled: profile updated, notifica |

Workers implement event processing with realistic payloads so you can trace the full event flow without external message brokers. Replace the simulation with real event sources, the workflow and routing logic stay the same.

### The Workflow

```
eo_receive_event
 │
 ▼
eo_extract_type
 │
 ▼
SWITCH (route_ref)
 ├── user: eo_user_processor
 ├── order: eo_order_processor
 └── default: eo_system_processor

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
