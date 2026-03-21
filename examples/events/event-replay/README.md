# Event Replay in Java Using Conductor

Event Replay Workflow. load event history, filter by criteria, replay failed events, and generate a summary report. ## The Problem

You need to replay failed or historical events from an event stream. The workflow loads event history from a source stream for a given time range, filters events by criteria (e.g., only failed events, specific event types), replays the filtered events through your processing pipeline, and generates a summary report of replay outcomes. Without replay capability, failed events are lost forever and historical reprocessing requires manual intervention.

Without orchestration, you'd write a one-off script to query your event store, filter events, resubmit them to the processing queue, and track which ones succeeded. manually handling idempotency so replayed events do not cause duplicates, and logging everything to prove the replay was complete.

## The Solution

**You just write the history-load, event-filter, replay, and report-generation workers. Conductor handles replay sequencing, per-event retry during reprocessing, and a durable report of every replay operation.**

Each replay concern is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of loading history, filtering, replaying, and reporting, retrying individual event replays that fail, tracking the entire replay operation, and providing a complete audit trail. ### What You Write: Workers

Four workers manage event replay: LoadHistoryWorker reads from the event store, FilterEventsWorker selects events by criteria, ReplayEventsWorker reprocesses the filtered set, and GenerateReportWorker summarizes replay outcomes.

| Worker | Task | What It Does |
|---|---|---|
| **FilterEventsWorker** | `ep_filter_events` | Filters events based on the provided filter criteria (status and eventType). With status="failed" and eventType="orde... |
| **GenerateReportWorker** | `ep_generate_report` | Generates a summary report from the replay results. Returns reportId, success rate, success/fail counts, and a fixed ... |
| **LoadHistoryWorker** | `ep_load_history` | Loads event history from the specified source stream within the given time range. Returns a fixed set of 6 events rep... |
| **ReplayEventsWorker** | `ep_replay_events` | Replays filtered events. Each event is replayed with a deterministic success status. Returns replay results with repl... |

Workers implement event processing with realistic payloads so you can trace the full event flow without external message brokers. Replace the simulation with real event sources. the workflow and routing logic stay the same.

### The Workflow

```
ep_load_history
 │
 ▼
ep_filter_events
 │
 ▼
ep_replay_events
 │
 ▼
ep_generate_report

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
