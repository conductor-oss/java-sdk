# Event Driven Microservices in Java with Conductor

Event-driven microservices choreography via Conductor.

## The Problem

In an event-driven architecture, a domain event must be emitted, processed by business logic, used to update read-side projections, and fanned out to interested subscribers. Each step depends on the previous one. Subscribers cannot be notified until the event is processed, and the projection must reflect the latest state.

Without orchestration, event processing is scattered across message consumers with no unified view of the event lifecycle. If the projection update fails, the event is lost unless you build your own retry and dead-letter infrastructure.

## The Solution

**You just write the event-emit, processing, projection-update, and subscriber-notification workers. Conductor handles event pipeline sequencing, guaranteed projection updates, and full lifecycle visibility per event.**

Each worker represents a service boundary. Conductor manages cross-service orchestration, compensating transactions, timeout enforcement, and distributed tracing. your workers just make the service calls.

### What You Write: Workers

Four workers model the event lifecycle: EmitEventWorker publishes a domain event, ProcessEventWorker applies business logic, UpdateProjectionWorker refreshes the read-side view, and NotifySubscribersWorker fans out to downstream consumers.

| Worker | Task | What It Does |
|---|---|---|
| **EmitEventWorker** | `edm_emit_event` | Publishes a domain event with a type, payload, and source, returning a unique event ID. |
| **NotifySubscribersWorker** | `edm_notify_subscribers` | Sends notifications to all identified subscribers of the event. |
| **ProcessEventWorker** | `edm_process_event` | Processes the event by applying business logic and identifying downstream subscribers (e.g., billing, shipping, analytics). |
| **UpdateProjectionWorker** | `edm_update_projection` | Updates the read-side projection (materialized view) with the processed event data. |

the workflow coordination stays the same.

### The Workflow

```
edm_emit_event
 │
 ▼
edm_process_event
 │
 ▼
edm_update_projection
 │
 ▼
edm_notify_subscribers

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
