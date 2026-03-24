# Request Aggregation in Java with Conductor

Aggregates data from multiple microservices in parallel using FORK_JOIN, then merges results into a single response.

## The Problem

A single client request (e.g., loading a user dashboard) often requires data from multiple backend services. User profiles, order history, and personalized recommendations. These fetches are independent and should run in parallel for performance, then the results must be merged into a single response.

Without orchestration, the API layer manually fans out HTTP calls using CompletableFuture or RxJava, handles partial failures (one service down), and merges results in application code. Adding a new data source means modifying the fan-out logic and the merge step.

## The Solution

**You just write the user-fetch, orders-fetch, recommendations-fetch, and merge workers. Conductor handles parallel fan-out to all data sources, per-source timeout isolation, and automatic join before merging.**

Each worker represents a service boundary. Conductor manages cross-service orchestration, compensating transactions, timeout enforcement, and distributed tracing. your workers just make the service calls.

### What You Write: Workers

Four workers serve the aggregation pipeline: FetchUserWorker, FetchOrdersWorker, and FetchRecommendationsWorker each retrieve data from independent services in parallel, then MergeResultsWorker combines them into a single dashboard response.

| Worker | Task | What It Does |
|---|---|---|
| **FetchOrdersWorker** | `agg_fetch_orders` | Fetches recent orders for a user. |
| **FetchRecommendationsWorker** | `agg_fetch_recommendations` | Generates recommendations for a user. |
| **FetchUserWorker** | `agg_fetch_user` | Fetches user profile data. |
| **MergeResultsWorker** | `agg_merge_results` | Merges results from multiple services into a single response. |

the workflow coordination stays the same.

### The Workflow

```
FORK_JOIN
 ├── agg_fetch_user
 ├── agg_fetch_orders
 └── agg_fetch_recommendations
 │
 ▼
JOIN (wait for all branches)
agg_merge_results

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
