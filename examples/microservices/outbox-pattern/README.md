# Outbox Pattern in Java with Conductor

Transactional outbox pattern for reliable event publishing. ## The Problem

When a service writes to its database and needs to publish an event, doing both atomically is impossible across different systems (database + message broker). The transactional outbox pattern writes the event to an outbox table in the same database transaction as the entity change, then a separate process polls the outbox, publishes events to the message broker, and marks them as published.

Without orchestration, outbox polling is implemented as a scheduled job with no visibility into which events are pending, which failed to publish, or how long events sit unpublished. Duplicate publishing is common without careful at-least-once/exactly-once handling.

## The Solution

**You just write the outbox-write, poll, event-publish, and mark-published workers. Conductor handles ordered outbox processing, guaranteed delivery via retries, and a durable record of every publish attempt.**

Each worker represents a service boundary. Conductor manages cross-service orchestration, compensating transactions, timeout enforcement, and distributed tracing. your workers just make the service calls.

### What You Write: Workers

Four workers implement the transactional outbox: WriteWithOutboxWorker atomically persists the entity and an outbox entry, PollOutboxWorker reads unpublished events, PublishEventWorker delivers them to the broker, and MarkPublishedWorker prevents reprocessing.

| Worker | Task | What It Does |
|---|---|---|
| **MarkPublishedWorker** | `ob_mark_published` | Marks the outbox entry as published so it is not processed again. |
| **PollOutboxWorker** | `ob_poll_outbox` | Polls the outbox table for unpublished events and returns the event payload and destination topic. |
| **PublishEventWorker** | `ob_publish_event` | Publishes the event to the message broker (e.g., Kafka topic) and returns a message ID. |
| **WriteWithOutboxWorker** | `ob_write_with_outbox` | Writes the entity change and an outbox entry in a single atomic database transaction. |

the workflow coordination stays the same.

### The Workflow

```
ob_write_with_outbox
 │
 ▼
ob_poll_outbox
 │
 ▼
ob_publish_event
 │
 ▼
ob_mark_published

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
