# Implementing Error Classification in Java with Conductor : Retryable vs Non-Retryable Error Routing

## The Problem

You call an external API that returns different error codes. 429 (rate limited), 503 (service temporarily down), security-posture (bad request data). Each error type demands a different response: retryable errors should be retried with backoff, while non-retryable errors should be routed to an error handler that logs the issue, alerts the team, and prevents wasted retry attempts on requests that will never succeed.

Without orchestration, error classification is buried in nested if/else chains inside every API caller. Each service classifies errors differently, retries non-retryable errors wastefully, or fails to retry retryable ones. When a new error code appears, every caller must be updated independently.

## The Solution

**You just write the API call and error classification logic. Conductor handles SWITCH-based routing by error type, automatic retries for transient failures, and a full record of every classification decision showing which errors were retried and which were sent to the handler.**

The API call worker makes the request and classifies errors. Conductor's SWITCH task routes retryable errors back through retry logic and non-retryable errors to a dedicated error handler. Every error classification decision is recorded. you can see exactly which errors were retried, which were routed to the handler, and what the outcome was.

### What You Write: Workers

ApiCallWorker makes the external request and classifies the response error type, then Conductor's SWITCH task routes retryable errors back through retry logic and non-retryable errors to ErrorHandlerWorker for logging and alerting.

| Worker | Task | What It Does |
|---|---|---|
| **ApiCallWorker** | `ec_api_call` | Worker for ec_api_call. simulates different HTTP error codes based on the simulateError input parameter. Behavior by.. |
| **ErrorHandlerWorker** | `ec_handle_error` | Worker for ec_handle_error. logs the error type and details from the upstream API call, then completes with a handle.. |

Workers implement success and failure scenarios so you can observe the resilience pattern end-to-end. Swap in real service calls and the retry, compensation, and recovery behavior works identically.

### The Workflow

```
ec_api_call
 │
 ▼
SWITCH (error_type_switch_ref)
 ├── non_retryable: ec_handle_error

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
