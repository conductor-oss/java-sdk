# Database Per Service in Java with Conductor

Database per service pattern with parallel queries and view composition. ## The Problem

When each microservice owns its own database, building a unified view that spans multiple services (e.g., a user dashboard showing profile, orders, and product recommendations) requires querying each service's database independently and then composing the results. These queries are independent and should run in parallel for performance.

Without orchestration, the calling service manually fans out HTTP calls to each service, collects responses, handles partial failures, and merges the data, all in application code with no visibility into which query is slow or failed.

## The Solution

**You just write the per-database query workers and the view-composition worker. Conductor handles parallel fan-out, per-query retries and timeouts, and automatic join before composition.**

Each worker represents a service boundary. Conductor manages cross-service orchestration, compensating transactions, timeout enforcement, and distributed tracing. your workers just make the service calls.

### What You Write: Workers

Four workers span isolated databases: QueryUserDbWorker, QueryOrderDbWorker, and QueryProductDbWorker each hit their own data store in parallel, then ComposeViewWorker merges the results into a unified dashboard view.

| Worker | Task | What It Does |
|---|---|---|
| **ComposeViewWorker** | `dps_compose_view` | Merges user, order, and product data into a single unified view. |
| **QueryOrderDbWorker** | `dps_query_order_db` | Queries the order database for order count and latest order ID by userId. |
| **QueryProductDbWorker** | `dps_query_product_db` | Queries the product database for recently viewed products. |
| **QueryUserDbWorker** | `dps_query_user_db` | Queries the user database for profile data (name, tier) by userId. |

the workflow coordination stays the same.

### The Workflow

```
FORK_JOIN
 ├── dps_query_user_db
 ├── dps_query_order_db
 └── dps_query_product_db
 │
 ▼
JOIN (wait for all branches)
dps_compose_view

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
