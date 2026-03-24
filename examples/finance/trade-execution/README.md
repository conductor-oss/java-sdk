# Trade Execution in Java with Conductor

Trade execution workflow that validates orders, checks compliance, routes to optimal exchange, executes, and confirms.

## The Problem

You need to execute a securities trade from order validation to confirmation. An order is validated for completeness and market hours, checked against compliance rules (position limits, restricted lists, wash sale prevention), routed to the optimal exchange or dark pool, executed at the best available price, and confirmed with fill details. Executing without compliance checks violates regulations; routing to the wrong venue results in worse execution prices.

Without orchestration, you'd build a single trade pipeline that validates orders, queries compliance databases, implements smart order routing, sends FIX messages to exchanges, and processes fill reports. manually handling partial fills, order cancellations, and the microsecond-level timing requirements of modern markets.

## The Solution

**You just write the trade workers. Order validation, compliance checking, smart order routing, exchange execution, and fill confirmation. Conductor handles pipeline sequencing, automatic retries when an exchange connection drops, and full order lifecycle tracking for best execution reporting.**

Each trade execution concern is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of executing them in order (validate, check compliance, route, execute, confirm), retrying if an exchange connection drops, tracking every order's full lifecycle for best execution reporting, and resuming from the last step if the process crashes.

### What You Write: Workers

Five workers form the trade pipeline: ValidateOrderWorker checks order completeness and buying power, CheckComplianceWorker screens against regulatory rules, RouteWorker selects the optimal exchange, ExecuteWorker places the order, and ConfirmWorker delivers fill details.

| Worker | Task | What It Does |
|---|---|---|
| **CheckComplianceWorker** | `trd_check_compliance` | Checks regulatory compliance for the trade. |
| **ConfirmWorker** | `trd_confirm` | Sends trade confirmation to the client. |
| **ExecuteWorker** | `trd_execute` | Executes the trade on the routed exchange. |
| **RouteWorker** | `trd_route` | Routes the trade to the optimal exchange for best execution. |
| **ValidateOrderWorker** | `trd_validate_order` | Validates a trade order for required fields and buying power. |

Replace with real financial system integrations and the workflow, audit trail, and compliance logic stay the same.

### The Workflow

```
trd_validate_order
 │
 ▼
trd_check_compliance
 │
 ▼
trd_route
 │
 ▼
trd_execute
 │
 ▼
trd_confirm

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
