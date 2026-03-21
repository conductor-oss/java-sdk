# End-to-End Supply Chain Management in Java with Conductor : Plan, Source, Make, Deliver, and Return

## The Problem

You need to orchestrate the complete supply chain from demand to delivery. A production plan must be created specifying what to build and how much. Raw materials must be sourced from approved suppliers with the right lead times. Manufacturing must execute the production plan using the sourced materials. The finished goods must be shipped to the customer or distribution center. Finally, the return policy and reverse logistics path must be configured for the delivery. Each step depends on the previous one. you cannot manufacture without sourced materials, and you cannot ship without finished goods.

Without orchestration, each department runs its own silo: planning uses a spreadsheet, procurement sends emails to suppliers, manufacturing tracks jobs on a whiteboard, and logistics manages shipping in a separate TMS. When sourcing delays push back the manufacturing schedule, planning doesn't find out until the factory calls. When a delivery fails and needs a return, the return process has no context about the original production plan or sourcing decisions.

## The Solution

**You just write the SCOR workers. Planning, sourcing, manufacturing, delivery, and returns. Conductor handles cross-department sequencing, supplier retries, and end-to-end visibility from plan to delivery.**

Each stage of the SCOR supply chain model is a simple, independent worker. a plain Java class that does one thing. Conductor sequences them so the plan drives sourcing decisions, sourced materials enable manufacturing, manufacturing output triggers delivery, and delivery configuration includes return handling. If the sourcing worker fails to reach a supplier, Conductor retries without re-creating the production plan. Every plan, sourcing decision, manufacturing record, shipment, and return policy is captured end-to-end for supply chain visibility and analytics.

### What You Write: Workers

Five workers follow the SCOR model: PlanWorker creates production plans, SourceWorker procures raw materials, MakeWorker handles manufacturing, DeliverWorker ships finished goods, and ReturnWorker configures reverse logistics.

| Worker | Task | What It Does |
|---|---|---|
| **DeliverWorker** | `scm_deliver` | Ships the batch to the destination. |
| **MakeWorker** | `scm_make` | Manufactures the product. |
| **PlanWorker** | `scm_plan` | Creates a production plan based on product and quantity. |
| **ReturnWorker** | `scm_return` | Configures the return policy for a delivery. |
| **SourceWorker** | `scm_source` | Sources materials from suppliers. |

### The Workflow

```
scm_plan
 │
 ▼
scm_source
 │
 ▼
scm_make
 │
 ▼
scm_deliver
 │
 ▼
scm_return

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
