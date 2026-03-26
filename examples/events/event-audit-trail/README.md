# Event Audit Trail

A regulated financial system must maintain an immutable audit trail of every event that flows through the platform. Each event needs timestamping, hash-chain linking to the previous entry, storage in an append-only log, and verification that the chain has not been tampered with.

## Pipeline

```
[at_log_received]
     |
     v
[at_validate_event]
     |
     v
[at_log_validated]
     |
     v
[at_process_event]
     |
     v
[at_log_processed]
     |
     v
[at_finalize_audit]
```

**Workflow inputs:** `eventId`, `eventType`, `eventData`

## Workers

**FinalizeAuditWorker** (task: `at_finalize_audit`)

Finalizes the audit trail.

- Reads `eventId`, `stages`. Writes `auditTrailId`, `totalStages`, `finalized`

**LogProcessedWorker** (task: `at_log_processed`)

Logs that an event has been processed.

- Sets `stage` = `"processed"`
- Reads `eventId`, `processResult`, `stage`. Writes `logged`, `stage`, `timestamp`

**LogReceivedWorker** (task: `at_log_received`)

Logs that an event has been received.

- Sets `stage` = `"received"`
- Reads `eventId`, `eventType`, `stage`. Writes `logged`, `stage`, `timestamp`

**LogValidatedWorker** (task: `at_log_validated`)

Logs that an event has been validated.

- Sets `stage` = `"validated"`
- Reads `eventId`, `validationResult`, `stage`. Writes `logged`, `stage`, `timestamp`

**ProcessEventWorker** (task: `at_process_event`)

Processes an event.

- Sets `result` = `"success"`
- Reads `eventId`, `eventData`. Writes `result`, `eventId`

**ValidateEventWorker** (task: `at_validate_event`)

Validates an incoming event.

- Reads `eventId`, `eventData`. Writes `valid`, `eventId`

---

**52 tests** | Workflow: `event_audit_trail_wf` | Timeout: 120s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
