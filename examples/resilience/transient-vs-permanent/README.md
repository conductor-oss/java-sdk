# Implementing Transient vs Permanent Error Detection in Java with Conductor : Smart Error Classification for Retry Decisions

## The Problem

Not all errors are equal. A network timeout is transient. retry and it will probably work. A 404 Not Found is permanent, retrying wastes time and resources. Your workers need to classify errors and signal to Conductor whether a retry makes sense. Retrying permanent errors wastes resources and delays failure detection. Not retrying transient errors causes unnecessary failures.

Without orchestration, error classification lives inside retry loops. Each caller decides independently whether to retry, using inconsistent criteria. Some retry 404s (wasteful), some don't retry 503s (premature failure). There's no centralized view of which errors are transient and which are permanent across the system.

## The Solution

The smart worker classifies each error as transient or permanent. For transient errors, it returns FAILED with a retryable flag so Conductor retries with backoff. For permanent errors, it returns FAILED_WITH_TERMINAL_ERROR so Conductor skips retries and fails immediately. This prevents wasted retries on permanent errors while ensuring transient errors get proper retry treatment.

### What You Write: Workers

SmartTaskWorker classifies each error as transient (retryable via FAILED) or permanent (skip retries via FAILED_WITH_TERMINAL_ERROR), enabling Conductor to retry network timeouts and 503s while immediately failing on 404s and authentication errors.

| Worker | Task | What It Does |
|---|---|---|
| **SmartTaskWorker** | `tvp_smart_task` | Smart task worker (tvp_smart_task) that classifies errors as transient or permanent. Behavior based on the "errorType... |

Workers implement success and failure scenarios so you can observe the resilience pattern end-to-end. Swap in real service calls and the retry, compensation, and recovery behavior works identically.

### The Workflow

```
tvp_smart_task

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
