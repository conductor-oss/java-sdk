# Event Filtering in Java Using Conductor

Event filtering workflow that receives events, classifies them by priority, and routes to urgent, standard, or drop handlers via a SWITCH task. ## The Problem

You need to filter incoming events by priority and route each to the appropriate processing lane. High-severity events must be handled urgently with fast-track processing and immediate alerting. Standard events go through normal processing. Low-priority or noise events are dropped to avoid wasting resources. Treating all events equally means critical alerts are delayed by a backlog of low-priority noise.

Without orchestration, you'd build a priority classifier with if/else chains, manually routing events to different processing queues, handling misclassified events that end up in the wrong lane, and tuning severity thresholds with hard-coded constants.

## The Solution

**You just write the event-receive, priority-classification, urgent-handler, standard-handler, and drop workers. Conductor handles priority-based SWITCH routing, per-lane retry policies, and complete visibility into every filter decision.**

Each processing lane is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of receiving the event, classifying its priority, routing via a SWITCH task to the correct lane (urgent, standard, or drop), retrying failed processing, and tracking every event's classification and outcome. ### What You Write: Workers

Five workers implement priority-based filtering: ReceiveEventWorker ingests events, ClassifyPriorityWorker assigns severity, then UrgentHandlerWorker fast-tracks critical events, StandardHandlerWorker queues normal events, and DropEventWorker discards noise.

| Worker | Task | What It Does |
|---|---|---|
| **ClassifyPriorityWorker** | `ef_classify_priority` | Classifies an event into a priority level based on severity. |
| **DropEventWorker** | `ef_drop_event` | Handles events that are dropped due to unknown severity levels. |
| **ReceiveEventWorker** | `ef_receive_event` | Receives an incoming event and enriches it with metadata. |
| **StandardHandlerWorker** | `ef_standard_handler` | Handles standard (medium/low severity) events by queuing for batch processing. |
| **UrgentHandlerWorker** | `ef_urgent_handler` | Handles urgent (critical/high severity) events with immediate alerting. |

Workers implement event processing with realistic payloads so you can trace the full event flow without external message brokers. Replace the simulation with real event sources. the workflow and routing logic stay the same.

### The Workflow

```
ef_receive_event
 │
 ▼
ef_classify_priority
 │
 ▼
SWITCH (switch_ref)
 ├── urgent: ef_urgent_handler
 ├── standard: ef_standard_handler
 └── default: ef_drop_event

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
