# Implementing Response Timeout in Java with Conductor : Detecting Stuck Workers via Response Time Limits

## The Problem

A worker picks up a task but never returns a result. it's stuck in an infinite loop, waiting on a deadlocked resource, or blocked on a network call that will never complete. Without a response timeout, Conductor waits forever, the workflow hangs, and downstream steps never execute. You need to detect stuck workers and trigger retry or failure handling within a bounded time.

Without orchestration, detecting stuck workers requires external watchdogs. process-level timeouts, thread dumps, and manual restarts. The business logic must implement its own internal timeouts for every blocking call, and there's no centralized view of which workers are stuck across the system.

## The Solution

The task definition includes `responseTimeoutSeconds`. if a worker doesn't complete within that window after picking up the task, Conductor marks it as timed out and retries or fails it as configured. Stuck workers are detected automatically without any timeout logic in the worker code. Every timeout event is recorded with timing details.

### What You Write: Workers

RespTimeoutWorker processes tasks within the configured time limit, while Conductor's responseTimeoutSeconds setting automatically detects workers that pick up a task but never return a result due to deadlocks, infinite loops, or blocked network calls.

| Worker | Task | What It Does |
|---|---|---|
| **RespTimeoutWorker** | `resp_timeout_task` | Worker for the resp_timeout_task. Responds quickly within the 3-second response timeout. Tracks the attempt number ac... |

Workers implement success and failure scenarios so you can observe the resilience pattern end-to-end. Swap in real service calls and the retry, compensation, and recovery behavior works identically.

### The Workflow

```
resp_timeout_task

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
