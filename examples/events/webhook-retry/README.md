# Webhook Retry

A webhook delivery system sends event notifications to customer endpoints, but customer servers are frequently unavailable. Failed deliveries need exponential backoff retry (1s, 2s, 4s, 8s...), a maximum retry count, dead-letter routing after exhausting retries, and delivery status tracking.

## Pipeline

```
[wr_prepare_webhook]
     |
     v
     +── loop ──────────────+
     |  [wr_attempt_delivery]
     |  [wr_check_result]
     +───────────────────────+
     |
     v
[wr_record_outcome]
```

**Workflow inputs:** `webhookUrl`, `payload`

## Workers

**AttemptDeliveryWorker** (task: `wr_attempt_delivery`)

Attempts to deliver the webhook payload to the target URL. perform  transient failures for early attempts and success on attempt >= 3.

- Reads `attempt`. Writes `statusCode`, `attempt`, `backoffMs`

**CheckResultWorker** (task: `wr_check_result`)

Checks the result of a webhook delivery attempt.

- Reads `statusCode`. Writes `success`, `statusCode`

**PrepareWebhookWorker** (task: `wr_prepare_webhook`)

Prepares webhook delivery by validating and packaging the URL and payload.

- Reads `webhookUrl`, `payload`. Writes `url`, `payload`, `preparedAt`

**RecordOutcomeWorker** (task: `wr_record_outcome`)

Records the final outcome of the webhook delivery process.

- Reads `totalAttempts`, `webhookUrl`. Writes `outcome`, `totalAttempts`

---

**32 tests** | Workflow: `webhook_retry_wf` | Timeout: 120s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
