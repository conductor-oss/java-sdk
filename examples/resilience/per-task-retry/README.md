# Implementing Per-Task Retry Configuration in Java with Conductor : Different Retry Strategies per Step

## The Problem

Different steps in your pipeline have different failure characteristics. A payment API needs exponential backoff (to avoid hammering a rate-limited endpoint), a validation step needs just 1 fixed retry (data errors won't fix themselves), and a notification step needs 3 retries with short delays (email servers recover quickly). A one-size-fits-all retry policy wastes time on some steps and gives up too early on others.

Without orchestration, per-step retry configuration means building separate retry loops for every step, each with its own delay calculation, max attempt tracking, and backoff strategy. When retry requirements change, you modify business logic code rather than configuration.

## The Solution

**You just write the task logic and declare each task's retry strategy in config. Conductor handles per-task retry timing with configurable strategy (FIXED or EXPONENTIAL_BACKOFF), attempt counting, backoff calculation, and a complete retry history for each step showing delays and outcomes.**

Each task defines its own retry configuration in the workflow definition. retry count, retry logic (FIXED or EXPONENTIAL_BACKOFF), and delay seconds. The workers just do their job and return success or failure. Conductor handles the retry timing, attempt counting, and backoff calculation per task. Changing a step's retry strategy is a JSON config change, not a code change.

### What You Write: Workers

PtrValidate uses 1 fixed retry for data errors that won't self-resolve, PtrPayment uses 5 retries with exponential backoff for rate-limited payment APIs, and PtrNotify uses 3 fixed retries with short delays for transient email delivery failures.

| Worker | Task | What It Does |
|---|---|---|
| **PtrNotify** | `ptr_notify` | Sends notification for an order. Configured with retryCount:3, FIXED retry, 2s delay. Returns { result: "email_sent",... |
| **PtrPayment** | `ptr_payment` | Processes payment for an order. Configured with retryCount:5, EXPONENTIAL_BACKOFF, 1s base delay. Returns { result: "... |
| **PtrValidate** | `ptr_validate` | Validates an order. Configured with retryCount:1, FIXED retry, 1s delay. Returns { result: "valid", orderId: ... } |

Workers implement success and failure scenarios so you can observe the resilience pattern end-to-end. Swap in real service calls and the retry, compensation, and recovery behavior works identically.

### The Workflow

```
ptr_validate
 │
 ▼
ptr_payment
 │
 ▼
ptr_notify

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
