# Event Handlers in Java with Conductor

Workflow triggered by external events. Processes the event type and payload. ## The Problem

You need to trigger a workflow automatically whenever an external event arrives. a webhook fires, a message lands on a queue, or a system emits a lifecycle event. The event carries a type (e.g., `order.created`, `user.signup`, `payment.failed`) and an arbitrary JSON payload. Your processing logic must dispatch based on the event type, parse the payload, execute the appropriate business action, and confirm that the event was handled successfully.

Without orchestration, you'd write a message consumer or webhook handler that processes events inline, with no retry logic if processing fails, no record of which events were handled, and no way to replay a missed event. If the handler crashes mid-processing, the event is lost or redelivered with no idempotency guarantee. Debugging which events were processed, and what the handler did with each one. requires digging through application logs.

## The Solution

**You just write the event processing worker. Conductor handles the event-to-workflow triggering, retries, and audit trail.**

This example demonstrates Conductor's event handler mechanism. external events trigger workflow executions automatically. When an event arrives with an `eventType` and `payload`, Conductor starts the `event_triggered_workflow`, which routes the event to the ProcessEventWorker. The worker parses the event type and payload, executes the appropriate processing logic, and returns a confirmation. Conductor tracks every event-triggered execution with its input event, processing result, and timing, giving you a complete audit trail of event handling. If the worker fails to process an event, Conductor retries it automatically without losing the event data.

### What You Write: Workers

A single ProcessEventWorker parses the event type and payload, executes the appropriate business logic, and returns a processing confirmation. Conductor triggers this worker automatically when external events arrive.

| Worker | Task | What It Does |
|---|---|---|
| **ProcessEventWorker** | `eh_process_event` | Processes an incoming event. Takes an eventType and payload, returns a result message confirming processing. |

Workers implement their processing steps so you can see the pattern in action without external services. Replace the simulation with real processing logic. the task pattern and Conductor orchestration remain unchanged.

### The Workflow

```
eh_process_event

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
