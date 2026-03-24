# Event Batching

A notification service receives individual alert events, but sending one email per event would overwhelm users' inboxes. The system needs to collect events over a configurable time window, batch them into a single digest, and deliver the digest while ensuring no event is lost between windows.

## Pipeline

```
[eb_collect_events]
     |
     v
[eb_create_batches]
     |
     v
     +── loop ──────────────+
     |  [eb_process_batch]
     +───────────────────────+
```

**Workflow inputs:** `events`, `batchSize`

## Workers

**CollectEventsWorker** (task: `eb_collect_events`)

Collects incoming events and returns them along with a total count.

- Reads `events`. Writes `events`, `totalCount`

**CreateBatchesWorker** (task: `eb_create_batches`)

Creates batches of events from the collected events list.

- Clamps with `math.min()`
- Reads `events`, `batchSize`. Writes `batches`, `batchCount`

**ProcessBatchWorker** (task: `eb_process_batch`)

Processes a single batch of events by index.

- Reads `batches`, `iteration`. Writes `batchIndex`, `eventsProcessed`

---

**29 tests** | Workflow: `event_batching` | Timeout: 120s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
