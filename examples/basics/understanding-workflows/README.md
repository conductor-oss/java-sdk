# Understanding Workflows in Java with Conductor: A 3-Step Order Pipeline

Your team calls everything a "workflow", the JIRA board, the CI pipeline, the Slack approval chain. But a Conductor workflow is something specific: a directed acyclic graph of typed tasks with durable state, automatic retries, and observable data flow between steps. Understanding that distinction is the first step toward building reliable orchestration. This example makes it concrete with a three-step order pipeline (validate, calculate total, send confirmation) that shows how workflow input flows into the first task, how JSONPath expressions pass data between tasks, and how Conductor handles the execution lifecycle.

## Learning by Building an Order Pipeline

Workflow concepts (tasks, sequencing, input/output mapping) are easier to understand with a real-world example. This order pipeline shows how workflow input (`orderId`, `customerEmail`, `items`) flows into the first task, how each task's output becomes the next task's input via JSONPath expressions, and how the workflow's final output is assembled from task results.

The three steps are intentionally simple so you can focus on how the workflow connects them rather than on complex business logic.

## The Solution

**You just write the order validation, total calculation, and confirmation logic. Conductor handles sequencing, data passing, and the execution lifecycle.**

Three workers handle the order lifecycle: validation (checking order data), total calculation (summing prices and quantities), and confirmation (notifying the customer). The workflow definition in JSON declares the sequence and data flow. Each worker is independent, it doesn't know about the others.

### What You Write: Workers

Three workers model a simple order pipeline: validate, process, ship, to demonstrate how Conductor sequences tasks and passes data between them.

| Worker | Task | What It Does |
|---|---|---|
| **ValidateOrderWorker** | `validate_order` | Iterates through each item in the order, marks it as `validated: true` and `inStock: true`, and returns the enriched list with an item count. |
| **CalculateTotalWorker** | `calculate_total` | Sums `price * qty` for each validated item, applies 8% tax (rounded to cents), and returns subtotal, tax, and total. |
| **SendConfirmationWorker** | `send_confirmation` | Logs a confirmation message for the customer email and order total. Returns `emailSent: true` and the recipient address. |

### The Workflow

```
validate_order
 │
 ▼
calculate_total
 │
 ▼
send_confirmation

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
