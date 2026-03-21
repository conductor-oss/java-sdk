# Dynamic Fork in Java with Conductor

You need to process N items in parallel, but N is only known at runtime. a user submits 3 URLs today and 300 tomorrow. Hardcoding 5 parallel branches means you can't scale to 50 or handle 1 without wasted resources. Rolling your own thread pool means managing `CompletableFuture` objects, writing barrier logic, and losing all progress if the process crashes after finishing 8 of 10 fetches. This example uses [Conductor](https://github.com/conductor-oss/conductor) `FORK_JOIN_DYNAMIC` to fan out to exactly N parallel branches at runtime, join when all complete, and aggregate the results, with per-branch retries and crash recovery built in.

## The Problem

You need to fetch multiple URLs in parallel, but you don't know how many URLs there are until runtime. A user submits a list of 3, 10, or 100 URLs, and each one needs to be fetched concurrently. . Not sequentially. After all fetches complete, the results must be aggregated into a single response with total size and per-URL metadata. If one fetch fails, it shouldn't block the others, and you need to know exactly which URLs succeeded and which didn't.

Without orchestration, you'd spin up a thread pool, submit each URL as a callable, manage a CompletableFuture for each, write barrier logic to wait for all completions, and aggregate results manually. If the process crashes after fetching 8 of 10 URLs, you lose all progress and start over. Adding a new URL to the list means the same code handles 3 threads or 300, with no visibility into which fetches are in flight.

## The Solution

**You just write the task preparation, per-URL fetch, and aggregation workers. Conductor handles the dynamic fanout, parallel execution, and join.**

This example demonstrates Conductor's FORK_JOIN_DYNAMIC task, runtime-determined parallelism. The PrepareTasksWorker inspects the input URL list and generates a dynamic task definition for each URL (one `df_fetch_url` task per URL with a unique reference name). Conductor fans out to N parallel branches, where N is determined entirely at runtime. Each FetchUrlWorker branch fetches its assigned URL independently. A JOIN task waits for every branch to complete, then the AggregateWorker collects all results from the join output into a single summary with total count and cumulative size. If one URL times out, Conductor retries just that branch, the other fetches are unaffected.

### What You Write: Workers

Three workers implement the dynamic fanout: PrepareTasksWorker generates one task definition per URL at runtime, FetchUrlWorker fetches each URL independently in its own parallel branch, and AggregateWorker collects all results after the join.

| Worker | Task | What It Does |
|---|---|---|
| **AggregateWorker** | `df_aggregate` | Aggregates results from all dynamic fork branches. Takes the joinOutput map (keyed by taskReferenceName) produced by |
| **FetchUrlWorker** | `df_fetch_url` | Fetches a URL and returns metadata. In a real application, this would make an HTTP request. Here it returns determini |
| **PrepareTasksWorker** | `df_prepare_tasks` | Prepares the dynamic task list and input map for FORK_JOIN_DYNAMIC. Takes a list of URLs and generates: - dynamicTask |

Workers implement their processing steps so you can see the pattern in action without external services. Replace the simulation with real processing logic, the task pattern and Conductor orchestration remain unchanged.

### The Workflow

```
df_prepare_tasks
 │
 ▼
FORK_JOIN_DYNAMIC (parallel, determined at runtime)
 │
 ▼
JOIN (wait for all branches)
df_aggregate

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
