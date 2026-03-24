# Webhook Retry in Java Using Conductor

Webhook delivery workflow with DO_WHILE retry loop. Prepares the webhook, attempts delivery up to 3 times, checks each result, and records the final outcome.

## The Problem

You need to deliver webhook payloads to external URLs with automatic retry on failure. The workflow prepares the webhook payload, attempts delivery to the target URL, and retries up to a configurable number of times with backoff if delivery fails (network error, timeout, non-2xx response). After all attempts, the final outcome is recorded. Without retry, transient network issues cause permanent webhook delivery failures.

Without orchestration, you'd build a retry loop with exponential backoff, manually tracking attempt counts, handling different failure modes (connection refused vs. 500 vs: timeout), and persisting delivery status. hoping the retry process itself does not crash and lose track of pending deliveries.

## The Solution

**You just write the payload-prepare, delivery-attempt, result-check, and outcome-recording workers. Conductor handles DO_WHILE retry loops, durable delivery state across attempts, and a complete record of every delivery attempt and final outcome.**

Each delivery concern is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of preparing the payload, attempting delivery in a DO_WHILE retry loop, checking each result, and recording the final outcome, with durable state that survives crashes and full visibility into every delivery attempt.

### What You Write: Workers

Four workers manage retry-based delivery: PrepareWebhookWorker packages the payload, AttemptDeliveryWorker makes the HTTP call, CheckResultWorker evaluates the response, and RecordOutcomeWorker logs the final delivery status.

| Worker | Task | What It Does |
|---|---|---|
| **AttemptDeliveryWorker** | `wr_attempt_delivery` | Attempts to deliver the webhook payload to the target URL. Simulates transient failures for early attempts and succes... |
| **CheckResultWorker** | `wr_check_result` | Checks the result of a webhook delivery attempt. |
| **PrepareWebhookWorker** | `wr_prepare_webhook` | Prepares webhook delivery by validating and packaging the URL and payload. |
| **RecordOutcomeWorker** | `wr_record_outcome` | Records the final outcome of the webhook delivery process. |

Workers implement event processing with realistic payloads so you can trace the full event flow without external message brokers. Replace the simulation with real event sources. the workflow and routing logic stay the same.

### The Workflow

```
wr_prepare_webhook
 │
 ▼
DO_WHILE
 └── wr_attempt_delivery
 └── wr_check_result
 │
 ▼
wr_record_outcome

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
