# Event Ttl in Java Using Conductor

Event TTL workflow that checks if an event has expired, processes it if still valid, or logs it if the TTL has passed. Uses SWITCH to branch on expiry status.

## The Problem

You need to enforce time-to-live (TTL) on events so expired events are discarded rather than processed. When an event arrives, the workflow checks whether it was created within the allowed TTL window. If still valid, it proceeds to processing; if expired, it is logged and discarded. Processing stale events (e.g., a price update from 2 hours ago) can produce incorrect results when the data has already been superseded.

Without orchestration, you'd embed TTL checks in every consumer, calculate expiry from timestamps manually, route expired events with if/else, and risk inconsistent TTL enforcement across different consumers that use different clock sources or tolerance windows.

## The Solution

**You just write the expiry-check, event-processing, expired-logging, and acknowledgment workers. Conductor handles expiry-based SWITCH routing, guaranteed processing of valid events, and a complete record of every TTL decision.**

Each TTL concern is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of checking the event's age against its TTL, routing via a SWITCH task to processing (valid) or logging (expired), retrying processing if it fails, and tracking every event's TTL decision.

### What You Write: Workers

Four workers enforce event expiration: CheckExpiryWorker evaluates the event's age against its TTL, ProcessEventWorker handles valid events, LogExpiredWorker discards stale ones, and AcknowledgeWorker confirms successful processing.

| Worker | Task | What It Does |
|---|---|---|
| **AcknowledgeWorker** | `xl_acknowledge` | Acknowledges a successfully processed event. |
| **CheckExpiryWorker** | `xl_check_expiry` | Checks whether an event has exceeded its TTL. |
| **LogExpiredWorker** | `xl_log_expired` | Logs an expired event that exceeded its TTL. |
| **ProcessEventWorker** | `xl_process_event` | Processes a valid (non-expired) event. |

Workers implement event processing with realistic payloads so you can trace the full event flow without external message brokers. Replace the simulation with real event sources. the workflow and routing logic stay the same.

### The Workflow

```
xl_check_expiry
 │
 ▼
SWITCH (switch_ref)
 ├── valid: xl_process_event -> xl_acknowledge
 ├── expired: xl_log_expired

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
