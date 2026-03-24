# Event Priority

A support ticket system receives events at different priority levels. Critical production outages must be processed before low-priority feature requests, even if the feature requests arrived first. The pipeline needs priority classification, queue assignment by priority tier, and processing that always drains the highest-priority queue first.

## Pipeline

```
[pr_classify_priority]
     |
     v
     <SWITCH>
       |-- high -> [pr_process_urgent]
       |-- medium -> [pr_process_normal]
       +-- default -> [pr_process_batch]
     |
     v
[pr_record_processing]
```

**Workflow inputs:** `eventId`, `eventType`

## Workers

**ClassifyPriorityWorker** (task: `pr_classify_priority`)

Classifies an event into a priority level based on its type.

- `PRIORITY_MAP` (Map)
- Reads `eventType`. Writes `priority`, `eventType`

**ProcessBatchWorker** (task: `pr_process_batch`)

Processes low-priority events in the batch lane (default case).

- Reads `eventId`. Writes `processed`, `lane`

**ProcessNormalWorker** (task: `pr_process_normal`)

Processes medium-priority events in the normal lane.

- Reads `eventId`. Writes `processed`, `lane`

**ProcessUrgentWorker** (task: `pr_process_urgent`)

Processes high-priority events in the urgent lane.

- Reads `eventId`. Writes `processed`, `lane`

**RecordProcessingWorker** (task: `pr_record_processing`)

Records the processing result for auditing.

- Reads `eventId`, `priority`. Writes `recorded`

---

**42 tests** | Workflow: `event_priority_wf` | Timeout: 120s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
