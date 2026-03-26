# Delayed Event

An e-commerce platform needs to send a follow-up email 24 hours after a purchase, cancel unpaid orders after 30 minutes, and send a review request 7 days after delivery. Each action must be scheduled with a precise delay, persisted durably so it survives restarts, and executed exactly once when the delay expires.

## Pipeline

```
[de_receive_event]
     |
     v
[de_compute_delay]
     |
     v
[de_apply_delay]
     |
     v
[de_process_event]
     |
     v
[de_log_completion]
```

**Workflow inputs:** `eventId`, `payload`, `delaySeconds`

## Workers

**ApplyDelayWorker** (task: `de_apply_delay`)

Applies the computed delay (deterministic. instant in mock).

- Reads `delayMs`, `eventId`. Writes `delayed`, `actualDelayMs`

**ComputeDelayWorker** (task: `de_compute_delay`)

Computes the delay duration in milliseconds from delaySeconds.

- Parses strings to `int`
- Reads `delaySeconds`, `eventId`. Writes `delayMs`, `delaySeconds`

**LogCompletionWorker** (task: `de_log_completion`)

Logs the completion of the delayed event processing.

- Reads `eventId`, `processedAt`. Writes `logged`

**ProcessEventWorker** (task: `de_process_event`)

Processes the delayed event.

- Sets `result` = `"success"`
- Reads `eventId`. Writes `processedAt`, `result`

**ReceiveEventWorker** (task: `de_receive_event`)

Receives an incoming event and marks it as received.

- Reads `eventId`. Writes `received`, `receivedAt`

---

**40 tests** | Workflow: `delayed_event` | Timeout: 60s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
