# Graceful Service Shutdown in Java with Conductor

Orchestrates graceful shutdown: stop accepting new work, drain in-flight tasks, checkpoint state, deregister from service registry, and terminate. ## The Problem

Shutting down a service instance without dropping in-flight requests requires a careful sequence: stop accepting new work, drain all in-flight tasks to completion, checkpoint any pending state so it can be resumed elsewhere, and deregister the instance from the service registry. Skipping any step leads to dropped requests or stale registry entries.

Without orchestration, shutdown hooks are implemented as JVM ShutdownHook callbacks that run in unpredictable order, with no guarantee that draining completes before deregistration. There is no record of whether the shutdown was clean or if tasks were lost.

## The Solution

**You just write the stop-accepting, drain, checkpoint, and deregister workers. Conductor handles ordered shutdown steps, guaranteed completion of each phase, and a durable record proving the shutdown was clean.**

Each worker represents a service boundary. Conductor manages cross-service orchestration, compensating transactions, timeout enforcement, and distributed tracing. your workers just make the service calls.

### What You Write: Workers

Four workers execute the shutdown sequence: StopAcceptingWorker halts new work intake, DrainTasksWorker waits for in-flight tasks to complete, CheckpointWorker persists pending state, and DeregisterWorker removes the instance from the service registry.

| Worker | Task | What It Does |
|---|---|---|
| **CheckpointWorker** | `gs_checkpoint` | Checkpoints the current state of the service instance before shutdown. |
| **DeregisterWorker** | `gs_deregister` | Deregisters the service instance from the service registry. |
| **DrainTasksWorker** | `gs_drain_tasks` | Drains in-flight tasks for the given instance, waiting for them to complete. |
| **StopAcceptingWorker** | `gs_stop_accepting` | Stops accepting new tasks for the given service instance. |

the workflow coordination stays the same.

### The Workflow

```
gs_stop_accepting
 │
 ▼
gs_drain_tasks
 │
 ▼
gs_checkpoint
 │
 ▼
gs_deregister

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
