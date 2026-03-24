# CQRS Pattern in Java with Conductor

CQRS pattern - Command side with validation, persistence, and read model update.

## The Problem

CQRS (Command Query Responsibility Segregation) separates write operations from read operations. A command must be validated, its resulting event persisted to an event store, and the read model updated to reflect the new state. These steps must happen in order, the read model update depends on the persisted event.

Without orchestration, the command handler directly calls the event store and read-model updater in a single method, mixing concerns and making it hard to add new projections. If the read-model update fails after the event is persisted, the system is in an inconsistent state with no automatic retry.

## The Solution

**You just write the command validation, event persistence, and read-model projection workers. Conductor handles write-side sequencing, guaranteed projection updates via retries, and full command execution history.**

Each worker represents a service boundary. Conductor manages cross-service orchestration, compensating transactions, timeout enforcement, and distributed tracing. your workers just make the service calls.

### What You Write: Workers

Three command-side workers separate write concerns: ValidateCommandWorker enforces business rules, PersistEventWorker appends to the event store, and UpdateReadModelWorker projects the change into the read-optimized view.

| Worker | Task | What It Does |
|---|---|---|
| **PersistEventWorker** | `cqrs_persist_event` | Appends the validated event to the event store and returns the event ID. |
| **QueryReadModelWorker** | `cqrs_query_read_model` | Queries the read model to return the current state of an aggregate. |
| **UpdateReadModelWorker** | `cqrs_update_read_model` | Updates the read-side projection (denormalized view) to reflect the new event. |
| **ValidateCommandWorker** | `cqrs_validate_command` | Validates the incoming command against business rules and produces a domain event. |

the workflow coordination stays the same.

### The Workflow

```
cqrs_validate_command
 │
 ▼
cqrs_persist_event
 │
 ▼
cqrs_update_read_model

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
