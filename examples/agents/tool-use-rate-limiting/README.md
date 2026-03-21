# Tool Use Rate Limiting in Java Using Conductor : Check Limits, Execute-or-Queue, Delayed Execution

Tool Use Rate Limiting. checks API rate limits before tool execution, queuing and delaying requests when throttled. ## APIs Have Rate Limits : Respect Them

Most external APIs enforce rate limits: OpenAI allows a certain number of requests per minute, Google Maps has a daily quota, and many services return 429 (Too Many Requests) when you exceed the limit. Hitting rate limits causes errors, potential API key suspension, and degraded user experience.

Proactive rate limiting checks the current request count before making the call. If you're under the limit, execute immediately. If you've hit the limit, queue the request and execute it after a delay (when the rate window resets). This is better than reactive rate limiting (retry after 429) because it avoids the error entirely and maintains consistent response times for queued requests.

## The Solution

**You write the rate-limit checking, queuing, and delayed execution logic. Conductor handles the allowed/throttled routing, queue management, and throttle-rate analytics.**

`CheckRateLimitWorker` checks the current request count against the configured limit for the tool and API key, returning whether the request is allowed or throttled. Conductor's `SWITCH` routes accordingly: allowed requests go to `ExecuteToolWorker` for immediate execution. Throttled requests (default case) go to `QueueRequestWorker` which schedules the request for later, then `DelayedExecuteWorker` which runs the tool after the rate window resets. Conductor records the rate limit decision for each request, enabling you to track throttle rates and adjust limits.

### What You Write: Workers

Four workers manage rate limits. Checking the quota, routing allowed requests to immediate execution, and routing throttled requests through a queue for delayed execution.

| Worker | Task | What It Does |
|---|---|---|
| **CheckRateLimitWorker** | `rl_check_rate_limit` | Checks the rate limit for the given tool/API key combination. Simulates near-limit conditions: quotaUsed=98, quotaLim... |
| **DelayedExecuteWorker** | `rl_delayed_execute` | Executes a previously queued tool request after a delay. Returns the same translation result with executedImmediately... |
| **ExecuteToolWorker** | `rl_execute_tool` | Executes the tool immediately when the rate limit allows it. Returns a demo translation result with executedImme... |
| **QueueRequestWorker** | `rl_queue_request` | Queues a throttled request for later execution. Returns a fixed queueId and the estimated wait time. |

Workers implement agent decisions and tool calls with realistic outputs so you can see the routing and handoff patterns without live LLM calls. Add your API keys to switch to live mode. the agent workflow stays the same.

### The Workflow

```
rl_check_rate_limit
 │
 ▼
SWITCH (rate_limit_decision_ref)
 ├── allowed: rl_execute_tool
 └── default: rl_queue_request -> rl_delayed_execute

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
