# Task Definitions in Java with Conductor

Task definitions test. runs td_fast_task to verify task definition configuration. ## The Problem

You need to configure per-task behavior: retry counts, retry strategies (FIXED vs EXPONENTIAL_BACKOFF), timeout durations, and response timeouts, independently from the workflow definition. Task definitions let you set these policies once and have them apply everywhere the task is used, across multiple workflows.

Without task definitions, retry and timeout policies are either hardcoded in the workflow JSON or scattered across worker code. Changing a timeout means editing every workflow that uses the task. Task definitions centralize these policies so they are consistent and easy to update.

## The Solution

**You just write the task worker. Conductor handles retries, timeouts, and backoff policies based on the task definition configuration.**

This example registers a task definition for `td_fast_task` with specific retry, timeout, and backoff policies, then runs a workflow that uses it. The FastTaskWorker is intentionally trivial: it just returns `{ done: true }`, because the point is the task definition, not the worker logic. The example code demonstrates creating a TaskDef with `retryCount`, `retryLogic` (FIXED or EXPONENTIAL_BACKOFF), `retryDelaySeconds`, `timeoutSeconds`, and `responseTimeoutSeconds`, registering it via the metadata API, and then running a workflow whose task inherits those policies automatically. Change the task definition once, and every workflow using that task picks up the new behavior.

### What You Write: Workers

One intentionally trivial worker demonstrates task definition configuration: FastTaskWorker returns `{ done: true }` so the focus stays on how retry counts, backoff strategies, and timeout policies are declared in the task definition rather than in worker code.

| Worker | Task | What It Does |
|---|---|---|
| **FastTaskWorker** | `td_fast_task` | Fast task worker for the task_def_test workflow. Simply returns { done: true } to confirm the task definition is work... |

Workers implement their processing steps so you can see the pattern in action without external services. Replace the simulation with real processing logic. the task pattern and Conductor orchestration remain unchanged.

### The Workflow

```
td_fast_task

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
