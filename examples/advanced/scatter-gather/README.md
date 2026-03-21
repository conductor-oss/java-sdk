# Scatter-Gather in Java Using Conductor: Broadcast Query, Gather from Sources in Parallel, Aggregate

You need a price quote from five vendors. Vendor A responds in 200ms. Vendor B in 400ms. Vendor C is having a bad day and takes 30 seconds. If you query them sequentially, every customer waits 31 seconds for a price comparison page. If you query them in parallel, you're managing thread pools, countdown latches, partial failure handling (vendor C timed out but A and B are fine), and response merging, all hand-rolled. And when vendor D starts returning prices in euros instead of dollars, your aggregation logic silently produces garbage. This example broadcasts a query to multiple sources in parallel using Conductor's `FORK_JOIN`, gathers responses from each, and aggregates them into a single ranked result. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers, you write the business logic, Conductor handles retries, failure routing, durability, and observability.

## Best Prices Require Querying Multiple Sources Simultaneously

A travel search needs to query multiple airline APIs. Delta, United, American, for flight prices on the same route. Querying them sequentially takes 3x as long. Querying them in parallel means managing concurrent HTTP calls, handling partial failures (United is down but Delta and American responded), setting per-source timeouts, and merging heterogeneous response formats into a single ranked list sorted by price.

The scatter-gather pattern broadcasts the same query to all sources simultaneously, gathers responses from each (tolerating failures), and aggregates them into a unified result. Building this by hand means thread pools, countdown latches, exception aggregation, and response merging, all of which Conductor provides declaratively.

## The Solution

**You write the per-source query logic. Conductor handles parallel scatter, per-source retries, and result aggregation.**

`SgrBroadcastWorker` prepares the query for distribution to all configured sources. A `FORK_JOIN` scatters the query to three gatherers in parallel. `SgrGather1Worker`, `SgrGather2Worker`, and `SgrGather3Worker` each query their respective data source and return results. The `JOIN` waits for all three to respond. `SgrAggregateWorker` merges the per-source results into a single ranked output (e.g., sorted by price, relevance, or latency). Conductor handles the parallel scatter, retries any slow or failed gatherer, and records per-source response times and result counts.

### What You Write: Workers

Five workers implement the scatter-gather: query broadcasting, three parallel source gatherers (one per pricing API), and best-price aggregation, each source queried independently and in parallel.

| Worker | Task | What It Does |
|---|---|---|
| **SgrAggregateWorker** | `sgr_aggregate` | Collects all price responses and selects the best (lowest) price across sources |
| **SgrBroadcastWorker** | `sgr_broadcast` | Broadcasts the price query to all source services in parallel |
| **SgrGather1Worker** | `sgr_gather_1` | Queries price service A and returns its price quote with currency |
| **SgrGather2Worker** | `sgr_gather_2` | Queries price service B and returns its price quote with currency |
| **SgrGather3Worker** | `sgr_gather_3` | Queries price service C and returns its price quote with currency |

### The Workflow

```
sgr_broadcast
 │
 ▼
FORK_JOIN
 ├── sgr_gather_1
 ├── sgr_gather_2
 └── sgr_gather_3
 │
 ▼
JOIN (wait for all branches)
sgr_aggregate

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
