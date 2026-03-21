# Rate Limiting in Java with Conductor

Rate limiting demo. demonstrates task-level rate limiting with concurrency and frequency constraints configured entirely in the task definition. The worker code contains zero throttling logic. Conductor enforces the limits across all workflow instances automatically. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers, you write the business logic, Conductor handles retries, failure routing, durability, and observability.

## The Problem

You need to call an external API that enforces rate limits. Say, 5 requests per 10-second window with a maximum of 2 concurrent connections. When hundreds of workflow instances are running simultaneously, each one trying to call the same API, you need to throttle at the task level so the API is not overwhelmed. Without throttling, you get 429 (Too Many Requests) errors, the API blocks your client, and workflows fail in bursts.

Without orchestration, you'd implement a token bucket or sliding window rate limiter in your application code, shared across all threads via a concurrent data structure or Redis. That rate limiter must survive process restarts, handle distributed deployments where multiple instances share the same limit, and be tuned per-API. Building and maintaining a example-grade distributed rate limiter is a significant engineering effort.

## The Solution

**You just write the API call worker with zero throttling logic. Conductor enforces concurrency and frequency limits via the task definition.**

This example demonstrates Conductor's task-level rate limiting. concurrency and frequency constraints configured in the task definition, not in your code. The task definition for `rl_api_call` sets `rateLimitPerFrequency: 5` (max 5 executions per window), `rateLimitFrequencyInSeconds: 10` (10-second window), and `concurrentExecLimit: 2` (max 2 running at once). The RlApiCallWorker simply takes a batchId and returns a deterministic result string ("batch-{batchId}-done"). When many workflow instances start simultaneously, Conductor queues excess tasks and releases them only when the rate limit permits. The worker code contains zero throttling logic, it just makes the call and returns the result.

### What You Write: Workers

A single worker demonstrates zero-code rate limiting: RlApiCallWorker processes a batch and returns a result string, with no throttling logic whatsoever. Conductor enforces the concurrency cap and frequency window entirely through the task definition.

| Worker | Task | What It Does |
|---|---|---|
| **RlApiCallWorker** | `rl_api_call` | Takes a batchId (string or number) and returns result="batch-{batchId}-done". Defaults batchId to "unknown" if null or blank. The rate limiting (5 per 10s, max 2 concurrent) is enforced by Conductor via the task definition, the worker has no throttling logic. |

The demo worker produces a realistic output shape so the workflow runs end-to-end. To go to production, replace the simulation with the real API call, the worker interface stays the same, and no workflow changes are needed.

### The Workflow

```
rl_api_call (rate-limited: 5 per 10s, max 2 concurrent)

```

### Rate Limit Configuration

The rate limiting is set on the **task definition**, not in the workflow or worker code:

```json
{
 "name": "rl_api_call",
 "rateLimitPerFrequency": 5,
 "rateLimitFrequencyInSeconds": 10,
 "concurrentExecLimit": 2
}

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
