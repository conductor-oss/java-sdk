# Dead Letter Events in Java Using Conductor

A `payment.charge` event hits your processor with a malformed payload. The catch block logs a warning. Nobody reads the log. Three days later, a customer calls: "Where's my order?" It's not stuck in a retry loop. It's not in a dead letter queue. It's gone. silently swallowed by a `catch (Exception e) { log.warn(...) }` that your team wrote at 4 PM on a Friday. There are 47 more just like it, and you only know about the one customer who bothered to call. This example builds a dead letter event pipeline with Conductor: receive the event, attempt processing, and route failures to a DLQ with alerting via a `SWITCH` task, so no failed event is ever silently dropped. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers, you write the business logic, Conductor handles retries, failure routing, durability, and observability.

## The Problem

You need to handle events that fail processing. When an event is received and processing fails (malformed payload, missing required fields, downstream service unavailable), it must be routed to a dead letter queue rather than silently dropped, and an alert must be sent so engineers can investigate. Silently losing failed events means data loss; not alerting means failures pile up unnoticed.

Without orchestration, you'd wrap your event processor in try/catch blocks, manually push failed events to a separate queue, send alert emails inline, and manage retry counts with ad-hoc counters. Hoping the DLQ push itself does not fail, and logging everything to debug why events are disappearing.

## The Solution

**You just write the event-receive, processing-attempt, DLQ-routing, and alert workers. Conductor handles failure-path SWITCH routing, guaranteed DLQ delivery via retries, and a full audit of every event's fate.**

Each dead-letter concern is a simple, independent worker, a plain Java class that does one thing. Conductor takes care of receiving the event, attempting processing, routing failures via a SWITCH task to the DLQ path with alerting or routing successes to finalization, retrying transient failures automatically, and tracking every event's fate.

### What You Write: Workers

Five workers manage failed-event routing: DlReceiveEventWorker ingests the event, DlAttemptProcessWorker tries to handle it, DlRouteToDlqWorker moves failures to the dead-letter queue, DlSendAlertWorker notifies engineers, and DlFinalizeSuccessWorker stamps successful completions.

| Worker | Task | What It Does |
|---|---|---|
| **DlAttemptProcessWorker** | `dl_attempt_process` | Attempts to process an event. If the payload contains a "requiredField" key, processing succeeds. Otherwise it fails |
| **DlFinalizeSuccessWorker** | `dl_finalize_success` | Finalizes a successfully processed event by stamping a finalized timestamp. |
| **DlReceiveEventWorker** | `dl_receive_event` | Receives an incoming event, normalizes retryCount to an integer, and stamps a receivedAt timestamp. |
| **DlRouteToDlqWorker** | `dl_route_to_dlq` | Routes a failed event to the dead letter queue, producing a DLQ entry with all relevant details. |
| **DlSendAlertWorker** | `dl_send_alert` | Sends an alert notification when an event is routed to the dead letter queue. |

Workers implement event processing with realistic payloads so you can trace the full event flow without external message brokers. Replace the simulation with real event sources, the workflow and routing logic stay the same.

### The Workflow

```
dl_receive_event
 │
 ▼
dl_attempt_process
 │
 ▼
SWITCH (result_switch_ref)
 ├── success: dl_finalize_success
 └── default: dl_route_to_dlq -> dl_send_alert

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
