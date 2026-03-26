# At Least Once

A payment notification system must guarantee that every payment event is processed, even if the worker crashes mid-execution. The pipeline needs message acknowledgment only after successful processing, automatic redelivery on failure, and duplicate detection so that a retried event does not charge a customer twice.

## Pipeline

```
[alo_receive]
     |
     v
[alo_process]
     |
     v
[alo_acknowledge]
     |
     v
[alo_verify_delivery]
```

**Workflow inputs:** `messageId`, `payload`, `topic`

## Workers

**AloAcknowledgeWorker** (task: `alo_acknowledge`)

- Captures `instant.now()` timestamps
- Writes `acknowledged`, `ackTimestamp`

**AloProcessWorker** (task: `alo_process`)

- Writes `processed`, `result`

**AloReceiveWorker** (task: `alo_receive`)

- Records wall-clock milliseconds
- Writes `receiptHandle`, `deliveryCount`, `visibilityTimeout`

**AloVerifyDeliveryWorker** (task: `alo_verify_delivery`)

- Reads `acknowledged`. Writes `deliveryVerified`, `deliveryGuarantee`

---

**16 tests** | Workflow: `alo_at_least_once` | Timeout: 60s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
