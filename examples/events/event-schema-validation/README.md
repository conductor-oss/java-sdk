# Event Schema Validation in Java Using Conductor

Event Schema Validation. validate an incoming event against a named schema, then route valid events for processing or invalid events to a dead-letter queue via a SWITCH task.

## The Problem

You need to validate incoming events against a schema before processing them. Malformed events (missing required fields, wrong data types, extra fields) must be caught early and routed to a dead-letter queue rather than corrupting downstream systems. Valid events proceed to normal processing. Without schema validation, one malformed event can crash a consumer, corrupt a database, or produce silently incorrect results.

Without orchestration, you'd embed validation logic in every consumer, duplicate schema definitions across services, handle validation failures with try/catch and manual DLQ routing, and debug production issues caused by events that passed one consumer's loose validation but failed another's strict checks.

## The Solution

**You just write the schema-validation, valid-event processing, and dead-letter workers. Conductor handles valid/invalid SWITCH routing, guaranteed DLQ delivery for bad events, and schema compliance tracking for every event.**

Each validation concern is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of validating the event against the named schema, routing via a SWITCH task to processing (valid) or dead-letter (invalid), retrying if the schema registry is unavailable, and tracking every event's validation result.

### What You Write: Workers

Three workers enforce schema compliance: ValidateSchemaWorker checks the event against a named schema, ProcessValidWorker handles conforming events, and DeadLetterWorker routes malformed events to the DLQ with validation errors attached.

| Worker | Task | What It Does |
|---|---|---|
| **DeadLetterWorker** | `sv_dead_letter` | Sends an invalid event to the dead-letter queue along with its validation errors. |
| **ProcessValidWorker** | `sv_process_valid` | Processes a valid event that has passed schema validation. |
| **ValidateSchemaWorker** | `sv_validate_schema` | Validate Schema. Computes and returns errors, schema used |

Workers implement event processing with realistic payloads so you can trace the full event flow without external message brokers. Replace the simulation with real event sources. the workflow and routing logic stay the same.

### The Workflow

```
sv_validate_schema
 │
 ▼
SWITCH (route_ref)
 ├── valid: sv_process_valid
 ├── invalid: sv_dead_letter

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
