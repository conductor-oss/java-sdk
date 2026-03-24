# Event Sourcing in Java with Conductor

Event sourcing with append-only event log and state rebuild.

## The Problem

Event sourcing persists every state change as an immutable event rather than overwriting the current state. When a new event arrives, it must be validated against business rules, appended to the event log, the current state rebuilt by replaying all events, and the event published to downstream consumers.

Without orchestration, the validate-append-rebuild-publish pipeline is implemented in a single service method with no separation of concerns. If the state rebuild fails after the event is appended, downstream consumers see a stale projection, and there is no retry mechanism.

## The Solution

**You just write the event validation, append, state-rebuild, and publish workers. Conductor handles append ordering, state-rebuild retry on failure, and a durable record of every event lifecycle.**

Each worker represents a service boundary. Conductor manages cross-service orchestration, compensating transactions, timeout enforcement, and distributed tracing. your workers just make the service calls.

### What You Write: Workers

Four workers maintain the event-sourced aggregate: ValidateEventWorker checks business rules, AppendEventWorker writes to the immutable log, RebuildStateWorker replays events to compute current state, and PublishEventWorker notifies downstream consumers.

| Worker | Task | What It Does |
|---|---|---|
| **AppendEventWorker** | `es_append_event` | Appends the validated event to the append-only event log and returns the event ID and version number. |
| **PublishEventWorker** | `es_publish_event` | Publishes the persisted event to downstream subscribers for async processing. |
| **RebuildStateWorker** | `es_rebuild_state` | Replays all events for the aggregate to rebuild the current state (e.g., balance, status). |
| **ValidateEventWorker** | `es_validate_event` | Validates the incoming event against business rules for the aggregate and produces a timestamped event payload. |

the workflow coordination stays the same.

### The Workflow

```
es_validate_event
 │
 ▼
es_append_event
 │
 ▼
es_rebuild_state
 │
 ▼
es_publish_event

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
