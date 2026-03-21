# Implementing Worker Health Checks in Java with Conductor : Monitoring Worker Availability and Performance

## The Problem

You have workers deployed across multiple hosts or containers. You need to know: are they running? Are they polling? How fast are they processing tasks? If a worker stops polling (process crashed, deployment failed, network issue), tasks queue up and workflows stall. You need visibility into worker health before it becomes a production incident.

Without orchestration, worker health monitoring requires custom infrastructure. process supervisors, heartbeat endpoints, and separate monitoring dashboards. Each worker must implement its own health reporting, and there's no unified view of worker fleet health.

## The Solution

Conductor tracks worker health automatically. poll timestamps, task completion rates, and queue depths are all available via Conductor's APIs. The example demonstrates querying these APIs to build health dashboards and set up alerting. Every worker's polling behavior and task execution is recorded without any health-check code in the workers themselves. ### What You Write: Workers

WhcWorker processes tasks while tracking health metrics (poll counts, completion rates), and Conductor's APIs provide unified visibility into worker availability, queue depths, and execution performance across the fleet.

| Worker | Task | What It Does |
|---|---|---|
| **WhcWorker** | `whc_task` | Worker for whc_task. processes tasks and tracks health metrics. Maintains thread-safe poll and completed counters th.. |

Workers implement success and failure scenarios so you can observe the resilience pattern end-to-end. Swap in real service calls and the retry, compensation, and recovery behavior works identically.

### The Workflow

```
whc_task

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
