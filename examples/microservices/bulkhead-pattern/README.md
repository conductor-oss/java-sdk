# Bulkhead Pattern in Java with Conductor

It's Tuesday at 3 AM. The recommendation service starts returning 504s because a third-party ML endpoint is hanging. No big deal. except every request to the recommendation service is holding a thread from the shared pool. Within two minutes, the thread pool is exhausted. Now the payment service, the user service, and the search service can't get threads either. Customers can't check out, can't log in, can't search. Your entire platform is down because a non-critical recommendation feature got slow. This is the noisy-neighbor problem: without resource isolation, one misbehaving service starves everything else. This workflow implements the bulkhead pattern, classifying requests into isolated resource pools so a failure in one service never consumes resources needed by others. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers, you write the business logic, Conductor handles retries, failure routing, durability, and observability.

## The Problem

When multiple microservices share the same thread pool, a slow or failing service can exhaust all available threads and starve healthy services. The bulkhead pattern isolates each service into its own resource pool, so a failure in one service cannot consume resources needed by others.

Without orchestration, implementing bulkhead isolation means managing thread pools, semaphores, or connection limits in application code, with no centralized view of pool utilization. Tuning pool sizes requires redeployments, and there is no audit trail of which requests were throttled.

## The Solution

**You just write the request-classification, pool-allocation, and execution workers. Conductor handles pool-aware task ordering, per-request retries, and full visibility into slot utilization.**

Each worker represents a service boundary. Conductor manages cross-service orchestration, compensating transactions, timeout enforcement, and distributed tracing. Your workers just make the service calls.

### What You Write: Workers

The bulkhead pipeline uses four workers: ClassifyRequestWorker assigns incoming requests to resource pools, AllocatePoolWorker reserves a slot, ExecuteRequestWorker runs the bounded operation, and ReleasePoolWorker frees the slot.

| Worker | Task | What It Does |
|---|---|---|
| **AllocatePoolWorker** | `bh_allocate_pool` | Allocates a slot in the specified resource pool. |
| **ClassifyRequestWorker** | `bh_classify_request` | Classifies an incoming request into a resource pool based on priority. |
| **ExecuteRequestWorker** | `bh_execute_request` | Executes the request within the allocated pool. |
| **ReleasePoolWorker** | `bh_release_pool` | Releases the pool slot after request execution. |

### The Workflow

```
bh_classify_request
 │
 ▼
bh_allocate_pool
 │
 ▼
bh_execute_request
 │
 ▼
bh_release_pool

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
