# Message Aggregation in Java Using Conductor : Collect, Combine, and Forward Correlated Messages

## Why Message Aggregation Needs Orchestration

In distributed systems, a single business event often produces multiple messages. an order generates a payment confirmation, an inventory reservation, and a shipping request. These messages arrive at different times from different services, and you need to collect all of them before you can compute a meaningful aggregate (total order value, item count, combined status) and forward it to the next stage.

Without orchestration, you'd build a stateful collector service that tracks which messages have arrived, implements timeout logic for stragglers, handles duplicates, computes the aggregate once the set is complete, and retries the downstream forwarding if it fails. That's a lot of state management, concurrency control, and failure handling bolted onto what should be a straightforward collect-and-combine operation.

## The Solution

**You write the collection and aggregation logic. Conductor handles sequencing, retries, and execution tracking.**

Each stage of the aggregation pipeline is a simple, independent worker. `AgpCollectWorker` gathers incoming messages and counts them. `AgpCheckCompleteWorker` compares the collected count against the expected total to determine if the full set has arrived. `AgpAggregateWorker` computes the combined result. totals, summaries, timestamps. `AgpForwardWorker` delivers the aggregated payload to the downstream consumer. Conductor sequences them, retries any that fail, and tracks every execution so you can see exactly which messages were collected and what aggregate was produced.

### What You Write: Workers

Four workers form the collect-and-combine pipeline: collection, completeness check, aggregation, and forwarding, each handling one stage of the message lifecycle.### The Workflow

```
agp_collect
 │
 ▼
agp_check_complete
 │
 ▼
agp_aggregate
 │
 ▼
agp_forward

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
