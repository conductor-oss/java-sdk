# Event Split

A batch event contains multiple sub-events packed into a single payload (e.g., a daily transaction file with 1,000 individual transactions). The split pipeline needs to unpack the batch into individual events, assign each a unique ID, and emit them for independent downstream processing.

## Pipeline

```
[sp_receive_composite]
     |
     v
[sp_split_event]
     |
     v
     +──────────────────────────────────────────────────────────────+
     | [sp_process_sub_a] | [sp_process_sub_b] | [sp_process_sub_c] |
     +──────────────────────────────────────────────────────────────+
     [join]
     |
     v
[sp_combine_results]
```

**Workflow inputs:** `compositeEvent`

## Workers

**CombineResultsWorker** (task: `sp_combine_results`)

Combines results from all three parallel sub-event processors.

- Sets `status` = `"all_sub_events_processed"`
- Reads `resultA`, `resultB`, `resultC`. Writes `status`, `results`

**ProcessSubAWorker** (task: `sp_process_sub_a`)

Processes sub-event A (order details).

- Sets `result` = `"order_validated"`
- Reads `subEvent`. Writes `result`, `subType`

**ProcessSubBWorker** (task: `sp_process_sub_b`)

Processes sub-event B (customer info).

- Sets `result` = `"customer_verified"`
- Reads `subEvent`. Writes `result`, `subType`

**ProcessSubCWorker** (task: `sp_process_sub_c`)

Processes sub-event C (shipping info).

- Sets `result` = `"shipping_calculated"`
- Reads `subEvent`. Writes `result`, `subType`

**ReceiveCompositeWorker** (task: `sp_receive_composite`)

Receives a composite event and passes it through.

- Reads `compositeEvent`. Writes `event`

**SplitEventWorker** (task: `sp_split_event`)

Splits a composite event into three sub-events.

- Reads `event`. Writes `subEventA`, `subEventB`, `subEventC`, `subEventCount`

---

**48 tests** | Workflow: `event_split` | Timeout: 60s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
