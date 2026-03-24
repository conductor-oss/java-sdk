# Implementing Workflow Recovery in Java with Conductor: Durability Across Server Restarts

## The Problem

Your Conductor server restarts. Planned maintenance, crash, scaling event, OOM kill. In-flight workflows that were mid-execution must not be lost. A workflow that was between step 2 and step 3 of a 5-step pipeline should resume at step 3 after the restart, not re-run from the beginning or disappear entirely. If step 2 charged a credit card, re-running it would double-charge the customer.

### What Goes Wrong Without Durable State

Consider a nightly batch processing pipeline that processes 10,000 orders:

1. Workflow starts processing batch `batch-2024-001`
2. After 4,000 orders, the server crashes (OOM, hardware failure, bad deploy)
3. Server restarts

Without durable workflow state:
- **Best case**: The batch restarts from order 1. 4,000 orders are processed twice (duplicate charges, duplicate emails, duplicate inventory deductions)
- **Worst case**: The batch is silently lost. 6,000 orders are never processed, and nobody notices until customers complain

With Conductor's persistence, the workflow resumes at order 4,001 automatically. No duplicates, no lost work, no manual intervention.

## The Solution

Conductor persists every workflow execution to durable storage. When the server restarts, all in-flight workflows are recovered automatically and resume from their last completed task. No data is lost, no steps are re-executed, and no manual intervention is needed. The example demonstrates this by running a workflow, verifying it completes, then looking up the same workflow by ID to confirm the state was persisted.

### What You Write: Workers

DurableTaskWorker processes batches while Conductor persists every execution to durable storage, ensuring workflows survive server restarts and resume from their last completed step without re-executing or losing progress.

| Worker | Task | What It Does |
|---|---|---|
| **DurableTaskWorker** | `wr_durable_task` | Processes a batch by name. Accepts `{batch: "batch-001"}`, returns `{processed: true, batch: "batch-001"}`. Handles null/missing batch input gracefully by defaulting to empty string. |

The demo worker produces a realistic output shape so the workflow runs end-to-end. To go to production, replace the simulation with the real batch processing logic, the worker interface stays the same, and no workflow changes are needed.

### The Workflow

```
wr_durable_task
 |
 v
(workflow output persisted to durable storage)

```

The key insight is not the workflow structure. It is intentionally simple to highlight that **any** workflow, no matter how complex, gets durability. The persistence guarantee applies equally to a single-task workflow and a 50-task pipeline with forks, joins, and switches.

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
