# Splitter Pattern

An order contains multiple line items that must be fulfilled independently. The splitter breaks the order into individual items, processes each one (inventory check, pricing, packaging), and sends the results to the aggregator that reconstructs the complete fulfillment plan.

## Pipeline

```
[spl_receive_composite]
     |
     v
[spl_split]
     |
     v
     +────────────────────────────────────────────────────────────────────+
     | [spl_process_part_1] | [spl_process_part_2] | [spl_process_part_3] |
     +────────────────────────────────────────────────────────────────────+
     [join]
     |
     v
[spl_combine]
```

**Workflow inputs:** `compositeMessage`

## Workers

**SplCombineWorker** (task: `spl_combine`)

- Writes `combinedResult`, `allPartsProcessed`

**SplProcessPart1Worker** (task: `spl_process_part_1`)

- Writes `result`

**SplProcessPart2Worker** (task: `spl_process_part_2`)

- Writes `result`

**SplProcessPart3Worker** (task: `spl_process_part_3`)

- Writes `result`

**SplReceiveCompositeWorker** (task: `spl_receive_composite`)

- Writes `compositeMessage`

**SplSplitWorker** (task: `spl_split`)

- Writes `parts`, `partCount`

---

**24 tests** | Workflow: `spl_splitter_pattern` | Timeout: 60s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
