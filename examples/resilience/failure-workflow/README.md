# Implementing Failure Workflow in Java with Conductor : Automatic Error Handling When a Workflow Fails

## The Problem

You have a processing pipeline that can fail. When it does, you need automatic cleanup. release held resources, rollback partial changes, notify the team. The cleanup must happen reliably even if the main process crashed unexpectedly. And you need it to happen automatically, not rely on someone manually triggering a recovery script.

Without orchestration, failure handling lives in finally blocks and shutdown hooks that may not run if the process crashes. Cleanup logic is interleaved with business logic, making both harder to understand and test. Nobody can tell whether cleanup ran for a given failure.

## The Solution

**You just write the processing logic and error cleanup handlers. Conductor handles automatic failure detection, triggering the cleanup workflow with full error context, retries on cleanup steps, and tracking of every failure with its cleanup outcome.**

The main workflow runs the processing step. Conductor's failure workflow feature automatically triggers a separate error handler workflow when the main one fails. running cleanup and notification workers without any manual intervention. The failure workflow receives the original workflow's context (what failed, why, at which step), so cleanup workers have full information.

### What You Write: Workers

ProcessWorker executes the main business logic, and when it fails, Conductor's failure workflow automatically triggers CleanupWorker to release resources and NotifyFailureWorker to alert the team, all with the original failure context.

| Worker | Task | What It Does |
|---|---|---|
| **CleanupWorker** | `fw_cleanup` | Cleanup worker for the failure handler workflow. Runs after the main workflow fails, performing cleanup operations. R... |
| **NotifyFailureWorker** | `fw_notify_failure` | Notification worker for the failure handler workflow. Sends a failure alert after cleanup is done. Returns { notified... |
| **ProcessWorker** | `fw_process` | Processes the main workflow task. Takes a shouldFail flag. if true, fails the task to trigger the failure workflow. .. |

Workers implement success and failure scenarios so you can observe the resilience pattern end-to-end. Swap in real service calls and the retry, compensation, and recovery behavior works identically.

### The Workflow

```
fw_process

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
