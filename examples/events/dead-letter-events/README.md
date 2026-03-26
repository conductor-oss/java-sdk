# Dead Letter Events

A message processing system occasionally encounters events it cannot handle: malformed payloads, unknown event types, or transient downstream failures. Instead of silently dropping these events, the system needs to route them to a dead-letter queue with failure metadata, support manual inspection, and allow replay after the root cause is fixed.

## Pipeline

```
[dl_receive_event]
     |
     v
[dl_attempt_process]
     |
     v
     <SWITCH>
       |-- success -> [dl_finalize_success]
       +-- default -> [dl_route_to_dlq]
       +-- default -> [dl_send_alert]
```

**Workflow inputs:** `eventId`, `eventType`, `payload`, `retryCount`

## Workers

**DlAttemptProcessWorker** (task: `dl_attempt_process`)

Attempts to process an event. If the payload contains a "requiredField" key, processing succeeds. Otherwise it fails with a descriptive error reason.

- Reads `eventId`, `eventType`, `payload`, `retryCount`. Writes `processingResult`, `errorReason`, `failedAt`, `resultData`

**DlFinalizeSuccessWorker** (task: `dl_finalize_success`)

Finalizes a successfully processed event by stamping a finalized timestamp.

- Reads `eventId`, `result`. Writes `finalized`, `finalizedAt`

**DlReceiveEventWorker** (task: `dl_receive_event`)

Receives an incoming event, normalizes retryCount to an integer, and stamps a receivedAt timestamp.

- Parses strings to `int`
- Reads `eventId`, `eventType`, `payload`, `retryCount`. Writes `eventId`, `eventType`, `payload`, `retryCount`, `receivedAt`

**DlRouteToDlqWorker** (task: `dl_route_to_dlq`)

Routes a failed event to the dead letter queue, producing a DLQ entry with all relevant details.

- Reads `eventId`, `eventType`, `payload`, `errorReason`, `retryCount`. Writes `dlqId`, `dlqEntry`, `routedToDlq`

**DlSendAlertWorker** (task: `dl_send_alert`)

Sends an alert notification when an event is routed to the dead letter queue.

- Reads `eventId`, `dlqId`, `errorReason`. Writes `alertId`, `alertSent`, `recipients`, `sentAt`

---

**5 tests** | Workflow: `dead_letter_events_wf` | Timeout: 120s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
