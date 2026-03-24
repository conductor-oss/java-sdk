# Webhook Callback

A payment gateway notifies your system of transaction outcomes via webhook callbacks. Each callback needs signature verification, payload parsing, idempotent processing (gateways retry on timeout), and acknowledgment. A missed acknowledgment means the gateway will retry, potentially causing duplicate processing.

## Pipeline

```
[wc_receive_request]
     |
     v
[wc_process_data]
     |
     v
[wc_notify_callback]
```

**Workflow inputs:** `requestId`, `data`, `callbackUrl`

## Workers

**NotifyCallbackWorker** (task: `wc_notify_callback`)

Sends a callback notification to the partner's webhook URL with the processing result and completion status.

- Reads `callbackUrl`, `requestId`, `result`, `status`. Writes `callbackSent`, `responseStatus`, `callbackPayload`, `sentAt`

**ProcessDataWorker** (task: `wc_process_data`)

Processes the parsed data from the received webhook request. Produces a processing result with record counts and status.

- Reads `requestId`, `parsedData`. Writes `requestId`, `result`, `processingStatus`

**ReceiveRequestWorker** (task: `wc_receive_request`)

Receives an incoming webhook request, validates it, and parses the payload into a structured format for downstream processing.

- Reads `requestId`, `data`. Writes `requestId`, `parsedData`, `receivedAt`

---

**28 tests** | Workflow: `webhook_callback_wf` | Timeout: 120s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
