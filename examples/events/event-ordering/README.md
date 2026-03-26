# Event Ordering

A financial ledger system receives transaction events that must be processed in strict chronological order. Out-of-order processing would produce incorrect balances. The pipeline needs to detect ordering, buffer out-of-sequence events, re-sort into the correct order, and process them sequentially with ordering guarantees.

## Pipeline

```
[oo_buffer_events]
     |
     v
[oo_sort_events]
     |
     v
     +── loop ──────────────+
     |  [oo_process_next]
     +───────────────────────+
```

**Workflow inputs:** `events`

## Workers

**BufferEventsWorker** (task: `oo_buffer_events`)

Buffers incoming events — accepts a list of events and outputs them as a buffered collection along with the total count.

- Reads `events`. Writes `buffered`, `count`

**ProcessNextWorker** (task: `oo_process_next`)

Processes the next event from the sorted list based on the current DO_WHILE iteration index. Gets the event at position (iteration) and outputs its sequence number along with the iteration.

- Reads `sortedEvents`, `iteration`. Writes `processedSeq`, `iteration`

**SortEventsWorker** (task: `oo_sort_events`)

Sorts buffered events by the "seq" field in ascending order. For determinism, always returns the fixed sorted order: seq 1 ("create"), seq 2 ("modify"), seq 3 ("update").

- Writes `sorted`, `sortField`

---

**26 tests** | Workflow: `event_ordering` | Timeout: 120s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
