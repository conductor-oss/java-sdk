# Complex Event Processing

A fraud detection system receives thousands of transaction events per second. Individual events are benign, but specific patterns -- three failed logins followed by a large transfer within 10 minutes -- indicate fraud. The system needs to detect multi-event patterns, correlate across time windows, and trigger alerts only when a complex rule matches.

## Pipeline

```
[cp_ingest_events]
     |
     v
[cp_detect_sequence]
     |
     v
[cp_detect_absence]
     |
     v
[cp_detect_timing]
     |
     v
     <SWITCH>
       |-- pattern_found -> [cp_trigger_alert]
       +-- default -> [cp_log_normal]
```

**Workflow inputs:** `events`, `patternRules`

## Workers

**DetectAbsenceWorker** (task: `cp_detect_absence`)

Detects the absence of a "confirmation" event in the event list.

- Reads `events`, `rule`. Writes `detected`, `rule`

**DetectSequenceWorker** (task: `cp_detect_sequence`)

Detects whether "login" appears before "purchase" in the event sequence.

- Reads `events`, `rule`. Writes `detected`, `rule`

**DetectTimingWorker** (task: `cp_detect_timing`)

Checks if any gap between consecutive event timestamps exceeds maxGapMs.

- Reads `events`, `maxGapMs`. Writes `violation`, `overallResult`

**IngestEventsWorker** (task: `cp_ingest_events`)

Ingests a list of events and passes them through with a count.

- Reads `events`. Writes `events`, `count`

**LogNormalWorker** (task: `cp_log_normal`)

Logs normal activity when no anomalous patterns are detected.

- Reads `message`. Writes `logged`

**TriggerAlertWorker** (task: `cp_trigger_alert`)

Triggers an alert when anomalous patterns are detected.

- Reads `sequenceDetected`, `absenceDetected`, `timingViolation`. Writes `alerted`, `severity`

---

**51 tests** | Workflow: `complex_event_processing` | Timeout: 60s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
