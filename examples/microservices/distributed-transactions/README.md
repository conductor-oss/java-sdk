# Distributed Transactions in Java with Conductor

Distributed transactions with prepare-commit saga pattern. ## The Problem

An e-commerce order touches three services: order, payment, and inventory, each with its own database. All three must succeed or none should commit, but there is no single database transaction that spans them. This workflow uses a prepare-commit saga: all services prepare (reserve resources) in parallel, and only after all succeed does a commit step finalize them.

Without orchestration, distributed transactions are implemented with ad-hoc compensation logic scattered across services. If the payment prepare succeeds but the inventory prepare fails, the payment must be manually reversed, and forgetting a compensation path leads to data inconsistency.

## The Solution

**You just write the order-prepare, payment-prepare, inventory-prepare, and commit workers. Conductor handles parallel prepare execution, atomic commit-or-rollback routing, and durable transaction state.**

Each worker represents a service boundary. Conductor manages cross-service orchestration, compensating transactions, timeout enforcement, and distributed tracing. your workers just make the service calls.

### What You Write: Workers

Five workers implement a prepare-commit saga: PrepareOrderWorker, PreparePaymentWorker, and PrepareInventoryWorker each reserve resources in parallel, then CommitAllWorker finalizes or RollbackAllWorker compensates.

| Worker | Task | What It Does |
|---|---|---|
| **CommitAllWorker** | `dtx_commit_all` | Commits all three prepared transactions atomically using their transaction IDs. |
| **PrepareInventoryWorker** | `dtx_prepare_inventory` | Reserves the ordered items in the inventory service and returns a transaction ID. |
| **PrepareOrderWorker** | `dtx_prepare_order` | Prepares the order by reserving it in the order service and returns a transaction ID. |
| **PreparePaymentWorker** | `dtx_prepare_payment` | Authorizes the payment method and holds the charge, returning a transaction ID. |
| **RollbackAllWorker** | `dtx_rollback_all` | Rolls back all prepared transactions if any prepare step failed. |

the workflow coordination stays the same.

### The Workflow

```
FORK_JOIN
 ├── dtx_prepare_order
 ├── dtx_prepare_payment
 └── dtx_prepare_inventory
 │
 ▼
JOIN (wait for all branches)
dtx_commit_all

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
