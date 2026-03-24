# Implementing Fixed Retry in Java with Conductor : Constant-Delay Retries for Transient Failures

## The Problem

You have a task that fails due to transient issues. a database connection dropped, a file lock is temporarily held, a service is restarting. The recovery time is predictable (1-2 seconds), so exponential backoff would waste time with unnecessarily long delays. You need simple, fixed-interval retries: try, wait 1 second, try again.

Without orchestration, fixed retries mean a for-loop with Thread.sleep(1000) inside every task that might fail. The retry count is hardcoded, changing the delay requires a code change, and there's no record of how many retries were needed for a given execution.

## The Solution

The worker does its job and returns failure if the transient issue persists. Conductor retries with a fixed 1-second delay as configured in the task definition. Every retry attempt is tracked with timing. Changing the delay or retry count is a JSON config change.

### What You Write: Workers

RetryFixedWorker performs the task and reports success or failure, while Conductor handles fixed-interval retries with a constant 1-second delay between attempts. Ideal for transient errors with predictable recovery times.

| Worker | Task | What It Does |
|---|---|---|
| **RetryFixedWorker** | `retry_fixed_task` | Worker for retry_fixed_task. simulates transient failures. Uses an attempt counter keyed by workflow ID. Fails the f.. |

Workers implement success and failure scenarios so you can observe the resilience pattern end-to-end. Swap in real service calls and the retry, compensation, and recovery behavior works identically.

### The Workflow

```
retry_fixed_task

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
