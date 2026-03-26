# Event Dedup

A message broker delivers events with at-least-once semantics, meaning consumers regularly see the same event twice. The deduplication pipeline needs to compute a fingerprint for each event, check it against a seen-set, pass through only first-seen events, and report the duplicate rate.

## Pipeline

```
[dd_compute_hash]
     |
     v
[dd_check_seen]
     |
     v
     <SWITCH>
       |-- new -> [dd_process_event]
       |-- duplicate -> [dd_skip_event]
       +-- default -> [dd_skip_event]
```

**Workflow inputs:** `eventId`, `eventPayload`

## Workers

**CheckSeenWorker** (task: `dd_check_seen`)

Checks whether a given hash has been seen before (deterministic.lookup). Always returns "duplicate" to perform a previously-seen event.

- Reads `hash`. Writes `status`, `checkedAt`

**ComputeHashWorker** (task: `dd_compute_hash`)

Computes a deterministic hash of the event payload for deduplication. Uses a fixed hash value for demonstration purposes.

- Reads `eventId`, `payload`. Writes `hash`

**ProcessEventWorker** (task: `dd_process_event`)

Processes a new (non-duplicate) event.

- Reads `eventId`, `payload`, `hash`. Writes `processed`, `eventId`

**SkipEventWorker** (task: `dd_skip_event`)

Skips a duplicate (or unknown-status) event.

- Reads `eventId`, `hash`, `reason`. Writes `skipped`, `eventId`, `reason`

---

**33 tests** | Workflow: `event_dedup` | Timeout: 120s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
