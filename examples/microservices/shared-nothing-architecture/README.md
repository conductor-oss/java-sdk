# Shared Nothing Architecture in Java with Conductor

Shared nothing architecture with fully independent services. ## The Problem

In a shared-nothing architecture, each service owns its data and shares nothing with other services. No shared database, no shared file system, no shared memory. Services communicate only through explicit message passing. This workflow chains three independent services where each receives only the output of the previous one, and a final aggregation step combines all results.

Without orchestration, the calling service manually chains the calls, passing data between them. If service B fails, the data from service A is lost unless you build your own checkpointing. There is no visibility into which service in the chain is the bottleneck.

## The Solution

**You just write each isolated service worker and the aggregation worker. Conductor handles inter-service data passing, per-step crash recovery, and full visibility into the processing chain.**

Each worker represents a service boundary. Conductor manages cross-service orchestration, compensating transactions, timeout enforcement, and distributed tracing. your workers just make the service calls.

### What You Write: Workers

Four workers demonstrate complete data isolation: ServiceAWorker, ServiceBWorker, and ServiceCWorker each process data using their own private store, then AggregateWorker combines all outputs into a composite response.

| Worker | Task | What It Does |
|---|---|---|
| **AggregateWorker** | `sn_aggregate` | Combines results from all three services into a final aggregated response. |
| **ServiceAWorker** | `sn_service_a` | Processes the request independently with no shared state, using its own local data store. |
| **ServiceBWorker** | `sn_service_b` | Processes service A's output independently using its own isolated data store. |
| **ServiceCWorker** | `sn_service_c` | Processes service B's output independently using its own isolated data store. |

the workflow coordination stays the same.

### The Workflow

```
sn_service_a
 │
 ▼
sn_service_b
 │
 ▼
sn_service_c
 │
 ▼
sn_aggregate

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
