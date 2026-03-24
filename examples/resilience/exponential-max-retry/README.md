# Implementing Exponential Backoff with Max Retries in Java with Conductor : Unreliable API Calls with Dead Letter Fallback

## The Problem

You call an external API that intermittently fails. rate limits, temporary outages, network blips. Retrying immediately causes a thundering herd that makes things worse. You need exponential backoff (1s, 2s, 4s, 8s) to give the service time to recover. But you also need a maximum retry limit, if the API is down for hours, you can't retry forever. After exhausting retries, the failed request must be captured in a dead letter handler rather than silently lost.

Without orchestration, exponential backoff means writing retry loops with Thread.sleep(), tracking attempt counts, and manually routing to a dead letter queue after max retries. Each API caller implements backoff slightly differently, some forget the max retry cap, and dead letter routing is often missing entirely.

## The Solution

**You just write the API call and dead letter capture logic. Conductor handles exponential backoff retries with configurable delay and max attempts, automatic dead-letter routing when retries are exhausted, and a complete log of every retry attempt with timing and error details.**

The unreliable API worker makes the call and reports success or failure. Conductor handles exponential backoff retries automatically. configured per task with retry count, delay, and backoff rate. When retries are exhausted, the failure workflow routes to the dead letter handler. Every retry attempt is tracked with timing and error details.

### What You Write: Workers

UnreliableApiWorker makes the external call and reports success or failure, while DeadLetterLogWorker captures permanently failed requests after all retry attempts are exhausted.

| Worker | Task | What It Does |
|---|---|---|
| **DeadLetterLogWorker** | `emr_dead_letter_log` | Worker for emr_dead_letter_log. logs details of a failed workflow. Receives failed workflow details (workflowId, rea.. |
| **UnreliableApiWorker** | `emr_unreliable_api` | Worker for emr_unreliable_api. simulates an unreliable API call. If shouldSucceed=true, returns success with status=.. |

Workers implement success and failure scenarios so you can observe the resilience pattern end-to-end. Swap in real service calls and the retry, compensation, and recovery behavior works identically.

### The Workflow

```
emr_unreliable_api

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
