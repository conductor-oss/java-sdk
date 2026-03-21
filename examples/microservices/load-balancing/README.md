# Load Balancing in Java with Conductor

Distribute requests across service instances in parallel. ## The Problem

Processing a large batch of requests efficiently requires distributing the work across multiple service instances in parallel, collecting results from each instance, and aggregating them into a single response. The work is partitioned so each instance handles a subset, and the aggregation waits for all partitions.

Without orchestration, fan-out/fan-in patterns are implemented with manual thread management, CompletableFuture chains, and custom aggregation logic. Handling a failed partition (retrying just that instance) is complex, and there is no visibility into which partition is slow.

## The Solution

**You just write the instance-call and result-aggregation workers. Conductor handles parallel partition dispatch, per-instance retry on failure, and automatic join before aggregation.**

Each worker represents a service boundary. Conductor manages cross-service orchestration, compensating transactions, timeout enforcement, and distributed tracing. your workers just make the service calls.

### What You Write: Workers

Two worker types implement fan-out/fan-in: CallInstanceWorker processes a partition on a specific service instance, and AggregateResultsWorker merges all partition results into a single response.

| Worker | Task | What It Does |
|---|---|---|
| **AggregateResultsWorker** | `lb_aggregate_results` | Aggregates results from all parallel instance calls. |
| **CallInstanceWorker** | `lb_call_instance` | Processes a partition of a batch on a specific instance. |

the workflow coordination stays the same.

### The Workflow

```
FORK_JOIN
 ├── lb_call_instance
 ├── lb_call_instance
 └── lb_call_instance
 │
 ▼
JOIN (wait for all branches)
lb_aggregate_results

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
