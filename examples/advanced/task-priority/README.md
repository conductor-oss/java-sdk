# Task Priority Routing in Java Using Conductor : Classify Urgency, Route to Priority-Specific Queues

## Not All Tasks Are Equal : High Priority Must Jump the Queue

A production incident alert, a routine log rotation, and a monthly report request arrive at the same time. The incident needs immediate attention with dedicated resources. The log rotation can wait. The report can run overnight. Without priority classification and routing, all three compete for the same worker pool, and the incident sits behind the report in the queue.

Priority routing means classifying each task based on urgency and impact (a P1 incident is high/high, a log rotation is low/low), then routing it to the matching queue. high-priority tasks go to a dedicated fast lane with more workers and shorter timeouts, while low-priority tasks go to a batch queue that runs during off-peak hours.

## The Solution

**You write the classification and per-tier handling logic. Conductor handles priority routing, retries, and SLA tracking.**

`TprClassifyPriorityWorker` evaluates the task's urgency and impact to determine its priority level. high, medium, or low. A `SWITCH` task routes based on the classification: `TprRouteHighWorker` handles urgent tasks with dedicated resources and aggressive SLAs, `TprRouteMediumWorker` processes normal tasks with standard resources, and `TprRouteLowWorker` queues low-priority work for batch processing. Conductor's conditional routing makes the priority lanes declarative, and every execution records the classification rationale.

### What You Write: Workers

Four workers handle priority-based routing. urgency classification and three tier-specific handlers (high, medium, low), each queue operating with its own SLA and resource allocation.

### The Workflow

```
tpr_classify_priority
 │
 ▼
SWITCH (tpr_switch_ref)
 ├── high: tpr_route_high
 ├── medium: tpr_route_medium
 ├── low: tpr_route_low
 └── default: tpr_route_medium

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
