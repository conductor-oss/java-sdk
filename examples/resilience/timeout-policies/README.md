# Implementing Timeout Policies in Java with Conductor : TIME_OUT_WF, RETRY, and ALERT Behaviors

## The Problem

Different tasks need different timeout behaviors. A critical payment task should fail the entire workflow if it times out (TIME_OUT_WF). you can't proceed without payment confirmation. A flaky enrichment task should be retried on timeout (RETRY), it usually works on the second attempt. An analytics task can silently timeout (ALERT), the data will be backfilled later. One-size-fits-all timeout handling doesn't work.

Without orchestration, implementing different timeout policies per task means wrapping each call in its own timeout handler with custom logic. one throws an exception, one retries, one logs and continues. The timeout behavior is buried in code rather than declared in configuration.

## The Solution

**You just write the task logic and declare timeout policies in the workflow definition. Conductor handles per-task timeout detection with configurable policy enforcement (TIME_OUT_WF, RETRY, ALERT_ONLY), and a record of every timeout event showing which policy was applied and what action was taken.**

Each task's timeout policy is declared in the task definition. `timeoutPolicy: TIME_OUT_WF` for critical tasks, `RETRY` for flaky tasks, and `ALERT_ONLY` for non-critical tasks. The workers just do their work; Conductor handles the timeout detection and policy enforcement. Changing a task's timeout behavior is a config change, not a code change.

### What You Write: Workers

CriticalWorker uses TIME_OUT_WF to fail the entire workflow on timeout, RetryableWorker uses RETRY to automatically retry on timeout, and OptionalWorker uses ALERT_ONLY to continue the pipeline while flagging the timeout event.

| Worker | Task | What It Does |
|---|---|---|
| **CriticalWorker** | `tp_critical` | Worker for the tp_critical task. The task definition uses timeoutPolicy: TIME_OUT_WF. If this worker does not respond... |
| **OptionalWorker** | `tp_optional` | Worker for the tp_optional task. The task definition uses timeoutPolicy: ALERT_ONLY. If this worker does not respond ... |
| **RetryableWorker** | `tp_retryable` | Worker for the tp_retryable task. The task definition uses timeoutPolicy: RETRY. If this worker does not respond with... |

Workers implement success and failure scenarios so you can observe the resilience pattern end-to-end. Swap in real service calls and the retry, compensation, and recovery behavior works identically.

### The Workflow

```
tp_critical

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
