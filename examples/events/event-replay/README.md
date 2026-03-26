# Event Replay

After deploying a bugfix to an event processor, the operations team needs to replay the last 24 hours of events through the corrected logic. The replay pipeline needs to load historical events from storage, re-process them through the fixed handler, and produce a report comparing old vs. new results.

## Pipeline

```
[ep_load_history]
     |
     v
[ep_filter_events]
     |
     v
[ep_replay_events]
     |
     v
[ep_generate_report]
```

**Workflow inputs:** `sourceStream`, `startTime`, `endTime`, `filterCriteria`

## Workers

**FilterEventsWorker** (task: `ep_filter_events`)

Filters events based on the provided filter criteria (status and eventType). With status="failed" and eventType="order.created", returns 2 matching events.

- Reads `events`, `filterCriteria`. Writes `filteredEvents`, `filteredCount`

**GenerateReportWorker** (task: `ep_generate_report`)

Generates a summary report from the replay results. Returns reportId, success rate, success/fail counts, and a fixed generatedAt timestamp.

- Reads `replayResults`, `totalReplayed`, `sourceStream`. Writes `reportId`, `successRate`, `successCount`, `failCount`, `generatedAt`

**LoadHistoryWorker** (task: `ep_load_history`)

Loads event history from the specified source stream within the given time range. Returns a fixed set of 6 events representing various order and payment events.

- Reads `sourceStream`, `startTime`, `endTime`. Writes `events`, `totalLoaded`

**ReplayEventsWorker** (task: `ep_replay_events`)

Replays filtered events. Each event is replayed with a deterministic success status. Returns replay results with replayStatus "success" and a fixed replayedAt timestamp.

- Reads `filteredEvents`. Writes `results`, `totalReplayed`, `successCount`

---

**34 tests** | Workflow: `event_replay_wf` | Timeout: 120s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
