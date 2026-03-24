# Implementing Linear Backoff Retry in Java with Conductor : Linearly Increasing Delays Between Retries

## The Problem

You need retry behavior between fixed (same delay every time) and exponential (doubling delays). Linear backoff increases delays proportionally: 2s, 4s, 6s, 8s. This gives a recovering service progressively more time without the aggressive delay growth of exponential backoff, which can lead to very long waits after just a few retries.

Without orchestration, linear backoff means calculating delay * attemptNumber in each retry loop. Getting the math wrong means either too-aggressive retries (overwhelming the service) or too-conservative delays (unnecessary wait times).

## The Solution

The worker makes the call and returns success or failure. Conductor handles the linear backoff calculation automatically via the LINEAR_BACKOFF retry logic setting. Every retry attempt is recorded with the exact delay applied. Switching between linear, fixed, or exponential backoff is a config change.

### What You Write: Workers

RetryLinearWorker makes the service call and returns success or failure, while Conductor applies linearly increasing delays (2s, 4s, 6s, 8s) between retries, a middle ground between fixed and exponential backoff.

| Worker | Task | What It Does |
|---|---|---|
| **RetryLinearWorker** | `retry_linear_task` | Worker that simulates a service that is unavailable for the first 3 attempts and succeeds on the 4th attempt, demonst... |

Workers implement success and failure scenarios so you can observe the resilience pattern end-to-end. Swap in real service calls and the retry, compensation, and recovery behavior works identically.

### The Workflow

```
retry_linear_task

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
