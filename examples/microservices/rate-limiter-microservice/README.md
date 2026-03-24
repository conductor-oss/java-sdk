# Rate Limiter Microservice in Java with Conductor

Distributed rate limiting workflow that checks quotas, processes or rejects requests, and updates counters per client.

## The Problem

API rate limiting protects backend services from being overwhelmed. Each incoming request must have its client quota checked, and based on the result, the request is either processed (with the counter incremented) or rejected with a retry-after hint. The check and update must be consistent to avoid exceeding the limit.

Without orchestration, rate-limiting logic is embedded in API gateway middleware with no visibility into per-client quota usage. Changing rate limits requires redeploying the gateway, and there is no audit trail of rejected requests.

## The Solution

**You just write the quota-check, request-processing, rejection, and counter-update workers. Conductor handles conditional allow/reject routing via SWITCH, per-client retry policies, and an audit trail of every rate-limit decision.**

Each worker represents a service boundary. Conductor manages cross-service orchestration, compensating transactions, timeout enforcement, and distributed tracing. your workers just make the service calls.

### What You Write: Workers

Four workers implement distributed rate limiting: CheckQuotaWorker evaluates per-client quotas, ProcessRequestWorker handles allowed requests, RejectRequestWorker returns 429 responses, and UpdateCounterWorker increments the usage counter.

| Worker | Task | What It Does |
|---|---|---|
| **CheckQuotaWorker** | `rl_check_quota` | Checks the rate limit quota for a client on a given endpoint. |
| **ProcessRequestWorker** | `rl_process_request` | Processes the request when quota is available. |
| **RejectRequestWorker** | `rl_reject_request` | Rejects a request when the rate limit quota is exceeded. |
| **UpdateCounterWorker** | `rl_update_counter` | Updates the rate limit counter after processing a request. |

the workflow coordination stays the same.

### The Workflow

```
rl_check_quota
 │
 ▼
SWITCH (decision_ref)
 ├── false: rl_reject_request
 └── default: rl_process_request -> rl_update_counter

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
