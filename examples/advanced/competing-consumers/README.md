# Competing Consumers

A task queue receives work items faster than a single consumer can process them. Multiple consumer instances must pull from the same queue, process items in parallel, and ensure that each item is processed exactly once -- even when two consumers race to claim the same item.

## Pipeline

```
[ccs_publish]
     |
     v
[ccs_compete]
     |
     v
[ccs_process]
     |
     v
[ccs_acknowledge]
```

**Workflow inputs:** `taskPayload`, `queueName`, `consumerCount`

## Workers

**CcsAcknowledgeWorker** (task: `ccs_acknowledge`)

- Writes `acknowledged`, `removedFromQueue`

**CcsCompeteWorker** (task: `ccs_compete`)

- Captures `instant.now()` timestamps, uses randomization
- Reads `consumerCount`. Writes `winner`, `competitors`, `claimTimestamp`

**CcsProcessWorker** (task: `ccs_process`)

- Sets `result` = `"processed_by_"`
- Writes `result`, `processingTimeMs`

**CcsPublishWorker** (task: `ccs_publish`)

- Records wall-clock milliseconds
- Writes `messageId`, `published`

---

**16 tests** | Workflow: `ccs_competing_consumers` | Timeout: 60s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
