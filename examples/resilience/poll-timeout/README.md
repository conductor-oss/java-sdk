# Implementing Poll Timeout in Java with Conductor : Detecting Absent Workers via Queue Wait Limits

## The Problem

You schedule a task, but no worker picks it up. the worker process crashed, the deployment failed, or the worker is polling a different task queue. Without a poll timeout, the task sits in the queue indefinitely and the workflow hangs forever. You need to detect when no worker is available within a reasonable time window and take action (alert, fail the workflow, route to a fallback).

Without orchestration, detecting absent workers requires custom health check infrastructure. heartbeat monitoring, process supervisors, and manual alerting when tasks are stuck. Each task queue needs its own monitoring, and the detection logic is separate from the task definition.

## The Solution

The task definition includes `pollTimeoutSeconds`. if no worker picks up the task within that window, Conductor automatically marks it as timed out. This triggers retry logic or failure handling as configured. Every poll timeout event is recorded, so you can see exactly when and why a task was not picked up. ### What You Write: Workers

PollNormalTaskWorker processes tasks normally, while Conductor's pollTimeoutSeconds setting automatically detects when no worker picks up a task within the configured window. Indicating crashed or missing workers.

| Worker | Task | What It Does |
|---|---|---|
| **PollNormalTaskWorker** | `poll_normal_task` | Worker for the poll_normal_task task. Picks up the task and processes it immediately. Takes a mode input and returns ... |

Workers implement success and failure scenarios so you can observe the resilience pattern end-to-end. Swap in real service calls and the retry, compensation, and recovery behavior works identically.

### The Workflow

```
poll_normal_task

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
