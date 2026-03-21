# Implementing Idempotent Workers in Java with Conductor : Safe Retry of Charges and Emails

## The Problem

Conductor may retry a worker that actually succeeded but failed to report back (network blip between worker and Conductor). If your charge worker runs twice, the customer is double-charged. If your email worker runs twice, the customer gets duplicate confirmation emails. Every worker that has side effects must be idempotent. producing the same result whether executed once or multiple times with the same input.

Without orchestration awareness, idempotency is an afterthought. Developers build workers without considering retries, then discover double-charge bugs in production. Each worker implements its own deduplication logic inconsistently. some check a database flag, some use a Redis lock, some don't handle it at all.

## The Solution

**You just write the idempotent charge and notification logic. Conductor handles safe retries knowing each worker is idempotent, sequencing charge-then-notify, and tracking every execution so you can verify retry attempts produced identical results.**

Each worker is designed to be idempotent from the start. The charge worker uses the order ID as an idempotency key. if it's already been charged, it returns the existing result. The email worker checks whether a confirmation was already sent for this order. Conductor safely retries any worker knowing the result will be the same. Every execution is tracked, so you can see retry attempts and confirm they produced identical results. ### What You Write: Workers

ChargeWorker uses the order ID as an idempotency key to prevent double charges on retry, and SendEmailWorker deduplicates confirmation emails so customers never receive duplicate notifications.

| Worker | Task | What It Does |
|---|---|---|
| **ChargeWorker** | `idem_charge` | Idempotent charge worker. check-before-act pattern with a Map cache. Uses orderId as the idempotency key. On the fir.. |
| **SendEmailWorker** | `idem_send_email` | Idempotent email worker. deduplication with a Set. Uses orderId:email as the dedup key. If a notification has alread.. |

Workers implement success and failure scenarios so you can observe the resilience pattern end-to-end. Swap in real service calls and the retry, compensation, and recovery behavior works identically.

### The Workflow

```
idem_charge
 │
 ▼
idem_send_email

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
