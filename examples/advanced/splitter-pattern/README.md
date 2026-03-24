# Splitter Pattern in Java Using Conductor : Receive Composite Message, Split, Process Parts in Parallel, Combine

## Composite Messages Contain Independent Items That Can Be Processed in Parallel

An order arrives with three line items. a laptop, a monitor, and a keyboard. Each item needs independent processing: inventory check, pricing lookup, tax calculation. Processing them sequentially triples the latency. Processing them in parallel requires splitting the order into individual items, dispatching each to its own processing pipeline, waiting for all three to complete, and reassembling the results into a single order response.

The splitter pattern decomposes a composite message into its constituent parts, processes each part independently (and in parallel when possible), then combines the results. This is fundamental to any system that receives batch requests, multi-item orders, or composite events.

## The Solution

**You write the per-item processing logic. Conductor handles the split, parallel execution, per-item retries, and recombination.**

`SplReceiveCompositeWorker` ingests the composite message. `SplSplitWorker` decomposes it into individual parts. A `FORK_JOIN` processes all three parts in parallel. `SplProcessPart1Worker`, `SplProcessPart2Worker`, and `SplProcessPart3Worker` each handle one item independently. The `JOIN` waits for all parts to finish. `SplCombineWorker` reassembles the per-part results into a single response. Conductor handles the split, parallel processing, and recombination, retrying any failed part without affecting the others.

### What You Write: Workers

Six workers implement the split-and-recombine pattern: composite message reception, item splitting, three parallel per-item processors, and result combination, each handling one line item independently.

| Worker | Task | What It Does |
|---|---|---|
| **SplCombineWorker** | `spl_combine` | Recombines the individually processed line items into a final order total and fulfillment count |
| **SplProcessPart1Worker** | `spl_process_part_1` | Fulfills the first line item (e.g., LAPTOP-15) and computes its subtotal |
| **SplProcessPart2Worker** | `spl_process_part_2` | Fulfills the second line item (e.g., MOUSE-WL) and computes its subtotal |
| **SplProcessPart3Worker** | `spl_process_part_3` | Fulfills the third line item (e.g.

### The Workflow

```
spl_receive_composite
 │
 ▼
spl_split
 │
 ▼
FORK_JOIN
 ├── spl_process_part_1
 ├── spl_process_part_2
 └── spl_process_part_3
 │
 ▼
JOIN (wait for all branches)
spl_combine

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
