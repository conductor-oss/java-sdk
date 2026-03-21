# Chaining HTTP Tasks in Java with Conductor

Chain HTTP system tasks for API orchestration. ## The Problem

You need to call multiple external APIs in sequence, where each call depends on the previous one's response. A prepare step builds the request parameters, an HTTP system task calls the first API, the response feeds into the next HTTP call, and finally a worker processes the combined results. If any API call fails or returns an error, the chain must handle the failure without leaving data in an inconsistent state.

Without orchestration, you'd write nested HTTP client calls with try/catch blocks around each one, manually passing response data between calls, and implementing retry logic for transient API failures. Error handling becomes deeply nested, and there is no record of which API calls succeeded before the chain broke.

## The Solution

**You just write the request preparation and response processing workers. Conductor handles the HTTP calls, retries, and chaining.**

This example demonstrates Conductor's HTTP system tasks chained together for multi-step API orchestration. A PrepareRequest worker builds the request context, HTTP system tasks make the actual API calls (no worker code needed for these), and a ProcessResponse worker handles the final result. Conductor manages retries for each HTTP call independently, tracks every request/response pair, and resumes from the failed call if the process crashes mid-chain.

### What You Write: Workers

Two workers bookend the HTTP chain: PrepareRequestWorker builds the request context before the system HTTP tasks, and ProcessResponseWorker handles the final API result after the chain completes.

| Worker | Task | What It Does |
|---|---|---|
| **PrepareRequestWorker** | `http_prepare_request` | Worker that prepares an HTTP request by extracting the search term from the query input. Runs before the HTTP system ... |
| **ProcessResponseWorker** | `http_process_response` | Worker that processes the HTTP response from the API call. Receives the status code and body from the preceding HTTP ... |

Workers implement their processing steps so you can see the pattern in action without external services. Replace the simulation with real processing logic. the task pattern and Conductor orchestration remain unchanged.

### The Workflow

```
register_task_via_http [HTTP]
 │
 ▼
verify_task_via_http [HTTP]
 │
 ▼
format_http_result [INLINE]

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
