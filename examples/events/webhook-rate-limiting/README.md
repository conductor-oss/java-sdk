# Webhook Rate Limiting in Java Using Conductor

Rate limit incoming webhooks per sender. Identifies the sender, checks their request rate, and uses SWITCH to allow processing or queue for throttling. ## The Problem

You need to rate-limit incoming webhooks per sender to protect your system from webhook floods. Each incoming webhook must be identified by sender, checked against their rate limit, and either allowed through for processing or queued for throttled delivery. Without rate limiting, a misbehaving sender can overwhelm your processing pipeline and degrade service for all other senders.

Without orchestration, you'd implement a rate limiter with token buckets or sliding windows in middleware, manually tracking per-sender request counts, handling Redis failures gracefully, and balancing between rejecting requests (losing data) and queuing them (risking memory exhaustion).

## The Solution

**You just write the sender-identification, rate-check, process, and throttle-queue workers. Conductor handles SWITCH-based allow/throttle routing, per-sender rate tracking, and a full audit of every rate-limit decision.**

Each rate-limiting concern is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of identifying the sender, checking their rate, routing via a SWITCH task to process or throttle, and tracking every webhook's rate-limit decision. ### What You Write: Workers

Four workers enforce per-sender rate limits: IdentifySenderWorker extracts the sender identity, CheckRateWorker evaluates their request count, ProcessAllowedWorker handles permitted requests, and QueueThrottledWorker defers excess traffic.

| Worker | Task | What It Does |
|---|---|---|
| **CheckRateWorker** | `wl_check_rate` | Checks the current request rate for a sender against the rate limit. |
| **IdentifySenderWorker** | `wl_identify_sender` | Identifies the sender of an incoming webhook request. |
| **ProcessAllowedWorker** | `wl_process_allowed` | Processes an allowed webhook request. |
| **QueueThrottledWorker** | `wl_queue_throttled` | Queues a throttled webhook request for later retry. |

Workers implement event processing with realistic payloads so you can trace the full event flow without external message brokers. Replace the simulation with real event sources. the workflow and routing logic stay the same.

### The Workflow

```
wl_identify_sender
 │
 ▼
wl_check_rate
 │
 ▼
SWITCH (switch_ref)
 ├── allowed: wl_process_allowed
 ├── throttled: wl_queue_throttled

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
