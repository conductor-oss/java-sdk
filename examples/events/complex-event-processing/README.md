# Complex Event Processing in Java Using Conductor

Complex event processing workflow that ingests events, detects sequences, absences, and timing violations, then routes via SWITCH to trigger alerts or log normal activity. ## The Problem

You need to analyze streams of events for patterns that signal anomalies. This means ingesting a batch of events, checking whether expected sequences occurred (e.g., login before purchase), detecting the absence of required events (e.g., missing confirmation), and identifying timing violations where gaps between events exceed acceptable thresholds. When any pattern is anomalous, the system must trigger an alert; otherwise, it logs normal activity. Each detection pass depends on the ingested events, and the final routing depends on the combined results.

Without orchestration, you'd build a monolithic event processor that reads from a stream, runs sequence/absence/timing checks in a single loop, and manually routes to alerting or logging with if/else chains. handling timeouts when the event store is slow, catching exceptions from individual detectors without crashing the whole pipeline, and logging every detection result to investigate false positives.

## The Solution

**You just write the event-ingestion, sequence-detection, absence-detection, timing-detection, and alert workers. Conductor handles multi-detector sequencing, SWITCH-based alert routing, and a complete record of every analysis run.**

Each detection concern is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of executing them in order (ingest, detect sequence, detect absence, detect timing), then routing via a SWITCH task to either trigger an alert or log normal activity, retrying if a detector fails, tracking every analysis run, and resuming from the last step if the process crashes. ### What You Write: Workers

Six workers analyze event streams: IngestEventsWorker accepts a batch, DetectSequenceWorker checks for expected ordering, DetectAbsenceWorker flags missing events, DetectTimingWorker catches gap violations, and TriggerAlertWorker or LogNormalWorker handles the outcome.

| Worker | Task | What It Does |
|---|---|---|
| **DetectAbsenceWorker** | `cp_detect_absence` | Detects the absence of a "confirmation" event in the event list. |
| **DetectSequenceWorker** | `cp_detect_sequence` | Detects whether "login" appears before "purchase" in the event sequence. |
| **DetectTimingWorker** | `cp_detect_timing` | Checks if any gap between consecutive event timestamps exceeds maxGapMs. |
| **IngestEventsWorker** | `cp_ingest_events` | Ingests a list of events and passes them through with a count. |
| **LogNormalWorker** | `cp_log_normal` | Logs normal activity when no anomalous patterns are detected. |
| **TriggerAlertWorker** | `cp_trigger_alert` | Triggers an alert when anomalous patterns are detected. |

Workers implement event processing with realistic payloads so you can trace the full event flow without external message brokers. Replace the simulation with real event sources. the workflow and routing logic stay the same.

### The Workflow

```
cp_ingest_events
 │
 ▼
cp_detect_sequence
 │
 ▼
cp_detect_absence
 │
 ▼
cp_detect_timing
 │
 ▼
SWITCH (switch_ref)
 ├── pattern_found: cp_trigger_alert
 └── default: cp_log_normal

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
