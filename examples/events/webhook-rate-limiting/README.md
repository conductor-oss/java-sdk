# Webhook Rate Limiting

A SaaS platform exposes a webhook endpoint that partners call to push data. Some partners send bursts of thousands of requests per second, overwhelming the processing pipeline. The rate limiter needs to track request counts per partner within a time window, reject excess requests with a 429 status, and queue accepted requests for processing.

## Pipeline

```
[wl_identify_sender]
     |
     v
[wl_check_rate]
     |
     v
     <SWITCH>
       |-- allowed -> [wl_process_allowed]
       |-- throttled -> [wl_queue_throttled]
```

**Workflow inputs:** `senderId`, `payload`, `rateLimit`

## Workers

**CheckRateWorker** (task: `wl_check_rate`)

Checks the current request rate for a sender against the rate limit.

- Parses strings to `int`
- Reads `senderId`, `rateLimit`. Writes `decision`, `currentRate`, `limit`, `retryAfterMs`

**IdentifySenderWorker** (task: `wl_identify_sender`)

Identifies the sender of an incoming webhook request.

- Reads `senderId`. Writes `senderId`, `ip`

**ProcessAllowedWorker** (task: `wl_process_allowed`)

Processes an allowed webhook request.

- Reads `senderId`. Writes `processed`, `senderId`

**QueueThrottledWorker** (task: `wl_queue_throttled`)

Queues a throttled webhook request for later retry.

- Parses strings to `int`
- Reads `senderId`, `retryAfterMs`. Writes `queued`, `retryAfterMs`

---

**34 tests** | Workflow: `webhook_rate_limiting` | Timeout: 60s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
