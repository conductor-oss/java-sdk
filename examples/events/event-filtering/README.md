# Event Filtering

A high-volume event stream includes both actionable events and noise (health checks, debug traces, duplicate heartbeats). Before downstream processing, the stream needs rule-based filtering that passes events matching specific criteria and drops the rest, with metrics on how many were filtered.

## Pipeline

```
[ef_receive_event]
     |
     v
[ef_classify_priority]
     |
     v
     <SWITCH>
       |-- urgent -> [ef_urgent_handler]
       |-- standard -> [ef_standard_handler]
       +-- default -> [ef_drop_event]
```

**Workflow inputs:** `eventId`, `eventType`, `severity`, `payload`

## Workers

**ClassifyPriorityWorker** (task: `ef_classify_priority`)

Classifies an event into a priority level based on severity.

- Reads `eventType`, `severity`. Writes `priority`, `filterResult`, `dropReason`

**DropEventWorker** (task: `ef_drop_event`)

Handles events that are dropped due to unknown severity levels.

- Sets `handler` = `"drop"`
- Reads `eventId`, `reason`. Writes `handled`, `handler`, `reason`

**ReceiveEventWorker** (task: `ef_receive_event`)

Receives an incoming event and enriches it with metadata.

- Reads `eventId`, `eventType`, `severity`, `payload`. Writes `eventType`, `severity`, `payload`, `metadata`

**StandardHandlerWorker** (task: `ef_standard_handler`)

Handles standard (medium/low severity) events by queuing for batch processing.

- Sets `handler` = `"standard"`
- Reads `eventId`. Writes `handled`, `handler`, `queued`, `processedAt`

**UrgentHandlerWorker** (task: `ef_urgent_handler`)

Handles urgent (critical/high severity) events with immediate alerting.

- Sets `handler` = `"urgent"`
- Reads `eventId`. Writes `handled`, `handler`, `alertSent`, `escalated`, `processedAt`

---

**42 tests** | Workflow: `event_filtering_wf` | Timeout: 60s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
