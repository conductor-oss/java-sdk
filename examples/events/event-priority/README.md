# Event Priority in Java Using Conductor

Event priority workflow that classifies events by priority and routes to the appropriate processing lane via a SWITCH task, then records the result. ## The Problem

You need to classify incoming events by priority and route each to the appropriate processing lane. Critical events require immediate processing, normal events go through standard pipelines, and low-priority events are handled in batch. The result of each processing lane must be recorded for auditing. Without priority-based routing, critical system alerts wait behind thousands of routine telemetry events.

Without orchestration, you'd build a priority classifier with a switch statement, manually managing separate thread pools or queues for each priority level, handling priority inversions, and recording processing results across different execution paths.

## The Solution

**You just write the priority-classification, per-lane processing, and result-recording workers. Conductor handles SWITCH-based priority routing, per-lane processing guarantees, and a complete audit of every priority decision.**

Each priority lane is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of classifying the event, routing via a SWITCH task to the appropriate lane (critical, normal, low), processing the event, recording the result, and retrying if processing fails. ### What You Write: Workers

Five workers implement priority lanes: ClassifyPriorityWorker assigns a level, then ProcessUrgentWorker, ProcessNormalWorker, or ProcessBatchWorker handles the event in its lane, and RecordProcessingWorker logs the outcome for audit.

| Worker | Task | What It Does |
|---|---|---|
| **ClassifyPriorityWorker** | `pr_classify_priority` | Classifies an event into a priority level based on its type. |
| **ProcessBatchWorker** | `pr_process_batch` | Processes low-priority events in the batch lane (default case). |
| **ProcessNormalWorker** | `pr_process_normal` | Processes medium-priority events in the normal lane. |
| **ProcessUrgentWorker** | `pr_process_urgent` | Processes high-priority events in the urgent lane. |
| **RecordProcessingWorker** | `pr_record_processing` | Records the processing result for auditing. |

Workers implement event processing with realistic payloads so you can trace the full event flow without external message brokers. Replace the simulation with real event sources. the workflow and routing logic stay the same.

### The Workflow

```
pr_classify_priority
 │
 ▼
SWITCH (priority_switch_ref)
 ├── high: pr_process_urgent
 ├── medium: pr_process_normal
 └── default: pr_process_batch
 │
 ▼
pr_record_processing

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
