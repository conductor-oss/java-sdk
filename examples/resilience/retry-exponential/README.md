# Implementing Exponential Backoff Retry in Java with Conductor : Doubling Delays for Rate-Limited APIs

## The Problem

You call an API that enforces rate limits and returns 429 errors when you exceed them. Retrying immediately makes the situation worse. you burn through your rate limit quota even faster. Exponential backoff gives the API progressively more time to recover: wait 1 second after the first failure, 2 seconds after the second, 4 seconds after the third, and so on.

Without orchestration, exponential backoff means Thread.sleep() calls with manual delay calculations inside retry loops. Each API caller implements backoff differently, some use linear delays, some forget to cap the maximum delay, and none of them track the retry history for debugging.

## The Solution

The worker makes the API call and returns success or failure. Conductor handles the exponential backoff automatically. configured via retryDelaySeconds and backoffRate in the task definition. Every retry attempt is recorded with the exact delay applied, so you can see the backoff progression. Changing the backoff rate or max retries is a config change, not a code change. ### What You Write: Workers

RetryExpoTaskWorker makes the API call and reports success or failure, while Conductor automatically applies doubling delays (1s, 2s, 4s, 8s) between retries to give the rate-limited service time to recover.

| Worker | Task | What It Does |
|---|---|---|
| **RetryExpoTaskWorker** | `retry_expo_task` | Simulates an API that returns 429 (Too Many Requests) for the first 2 calls, then succeeds on the 3rd attempt. Conduc... |

Workers implement success and failure scenarios so you can observe the resilience pattern end-to-end. Swap in real service calls and the retry, compensation, and recovery behavior works identically.

### The Workflow

```
retry_expo_task

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
