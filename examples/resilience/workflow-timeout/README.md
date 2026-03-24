# Implementing Workflow Timeout in Java with Conductor : Bounding Total Workflow Execution Time

## The Problem

Individual task timeouts prevent hung workers, but you also need a ceiling on total workflow execution time. A workflow with 10 tasks might have each task complete in 5 seconds; but if something causes the workflow to loop or stall between tasks, it could run for hours. A workflow-level timeout ensures the entire execution completes within a bounded time.

Without orchestration, total execution time limits require starting a timer at the beginning and checking it between every step. If the timer fires while a task is running, there's no clean way to abort it. Workflow-level timeouts are nearly impossible to implement correctly in hand-rolled orchestration.

## The Solution

The workflow definition includes `timeoutSeconds: 30`. if the entire workflow hasn't completed within that window, Conductor marks it as timed out. This catches scenarios that per-task timeouts miss: long queues between tasks, stuck decision logic, or unexpected loops. The timeout is configured in the workflow definition, not in code.

### What You Write: Workers

FastWorker completes its processing quickly, while the workflow-level timeoutSeconds setting ensures the entire workflow execution is bounded. Catching scenarios that per-task timeouts miss, such as stuck logic or long queue delays between tasks.

| Worker | Task | What It Does |
|---|---|---|
| **FastWorker** | `wft_fast` | Fast worker that completes immediately. Returns { result: "done-{mode}" } based on the input mode. |

Workers implement success and failure scenarios so you can observe the resilience pattern end-to-end. Swap in real service calls and the retry, compensation, and recovery behavior works identically.

### The Workflow

```
wft_fast

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
