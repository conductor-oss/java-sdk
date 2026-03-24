# Event Fanout

A single business event (e.g., "order placed") needs to trigger actions in multiple downstream systems simultaneously: update inventory, send a confirmation email, notify the warehouse, and log for analytics. Each consumer is independent and should not be blocked by a slow sibling.

## Pipeline

```
[fo_receive_event]
     |
     v
     +───────────────────────────────────────────────────+
     | [fo_analytics] | [fo_storage] | [fo_notification] |
     +───────────────────────────────────────────────────+
     [join]
     |
     v
[fo_aggregate]
```

**Workflow inputs:** `eventId`, `eventType`, `payload`

## Workers

**AggregateWorker** (task: `fo_aggregate`)

Aggregates results from the three parallel fan-out branches.

- Sets `status` = `"all_completed"`
- Reads `analyticsResult`, `storageResult`, `notifyResult`. Writes `status`, `processorResults`

**AnalyticsWorker** (task: `fo_analytics`)

Tracks event analytics and updates metrics.

- Sets `result` = `"tracked"`
- Reads `eventId`. Writes `result`, `metricsUpdated`

**NotificationWorker** (task: `fo_notification`)

Sends a notification about the event.

- Sets `result` = `"notified"`
- Reads `eventId`, `eventType`. Writes `result`, `channel`

**ReceiveEventWorker** (task: `fo_receive_event`)

Receives an incoming event and passes it through for fan-out processing.

- Reads `eventId`, `eventType`, `payload`. Writes `eventId`, `eventType`, `payload`

**StorageWorker** (task: `fo_storage`)

Stores the event payload to a data lake.

- Sets `result` = `"stored"`
- Reads `eventId`. Writes `result`, `storageLocation`

---

**41 tests** | Workflow: `event_fanout_wf` | Timeout: 120s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
