# Implementing Retry with Jitter in Java with Conductor : Avoiding Thundering Herd on Retries

## The Problem

When a shared dependency fails, all workers retry at the same time. the dependency recovers, gets slammed by simultaneous retry requests, and fails again. This cycle repeats indefinitely. Jitter adds a random offset to each retry delay so workers spread their retries over time, giving the dependency a chance to handle them gradually.

Without orchestration, implementing jitter means adding Random.nextInt() to Thread.sleep() calculations in every retry loop. Each worker implements jitter differently (or not at all), the jitter range varies, and there's no visibility into the actual delays applied.

## The Solution

The worker makes the API call with a jitter delay built into its logic to spread concurrent retries. Conductor tracks each execution with timing, so you can verify that retries are spread across time rather than clustered. The thundering herd is avoided without complex coordination between workers. ### What You Write: Workers

JitterApiCallWorker adds a randomized delay before each API call to spread concurrent retries over time, preventing the thundering herd problem where multiple workers slam a recovering service simultaneously.

| Worker | Task | What It Does |
|---|---|---|
| **JitterApiCallWorker** | `jitter_api_call` | Worker for jitter_api_call. adds a deterministic jitter delay before processing to avoid the thundering herd problem.. |

Workers implement success and failure scenarios so you can observe the resilience pattern end-to-end. Swap in real service calls and the retry, compensation, and recovery behavior works identically.

### The Workflow

```
jitter_api_call

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
