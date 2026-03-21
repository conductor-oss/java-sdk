# Travel Request Approval in Java with Conductor

An engineer submits a $4,000 conference trip request. Their manager approves it by replying "looks good" to an email; but the reply lands in a spam filter. The engineer assumes it's approved (no rejection came back either), books the flights and hotel on their corporate card, and flies to Austin. Finance catches it during month-end reconciliation: no approval on file, $4,000 expensed against a cost center that already blew its Q3 travel budget. Now the manager has to retroactively justify it, the engineer gets flagged for policy violation, and nobody is sure whether the reply-based approval system ever worked. This example uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate approval routing as independent workers. You write the business logic, Conductor handles retries, failure routing, durability, and observability.

## The Problem

You need to approve employee travel requests with different approval paths based on estimated cost. An employee submits a travel request with destination, purpose, and estimated cost. The workflow estimates the total trip cost (flights, hotel, per diem). If the estimate falls below the auto-approval threshold ($1,000), the request is approved instantly. If it exceeds the threshold, the request routes to the employee's manager for manual review.

Without orchestration, you'd hardcode the approval threshold in the submission handler, mix cost estimation logic with approval routing, and lose visibility into which requests were auto-approved vs, manager-approved. Changing the threshold or adding a VP-level approval tier means rewriting the entire flow. A SWITCH task cleanly separates the routing decision from the approval logic.

## The Solution

**You just write the request submission, cost estimation, and approval routing logic. Conductor handles policy check retries, approval routing, and authorization audit trails.**

SubmitWorker captures the travel request details (employee, destination, purpose, dates). EstimateWorker calculates the total estimated cost and determines the approval type. Auto-approve for requests under the threshold, manager review for higher amounts. A SWITCH task routes based on the approval type: AutoApproveWorker instantly approves compliant requests, while ManagerApproveWorker routes to the manager for review and records who approved it. Each worker is a standalone Java class. Conductor handles the routing, retries, and execution tracking.

### What You Write: Workers

Request submission, policy check, manager review, and approval notification workers each handle one gate in the corporate travel authorization process.

| Worker | Task | What It Does |
|---|---|---|
| **SubmitWorker** | `tva_submit` | Captures travel request details (employee ID, destination) and returns a unique request ID |
| **EstimateWorker** | `tva_estimate` | Evaluates the estimated cost against the $1,000 policy threshold and returns the approval type (`auto_approve` or `manager`) and total estimate |
| **AutoApproveWorker** | `tva_auto_approve` | Marks requests under the threshold as approved with method `auto` |
| **ManagerApproveWorker** | `tva_manager_approve` | Routes requests over the threshold to a manager for review and records the approver identity |

### The Workflow

```
tva_submit
 │
 ▼
tva_estimate
 │
 ▼
SWITCH (tva_switch_ref)
 ├── auto_approve: tva_auto_approve
 ├── manager: tva_manager_approve

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
