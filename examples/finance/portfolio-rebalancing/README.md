# Portfolio Rebalancing in Java with Conductor

Portfolio rebalancing workflow that analyzes drift, determines trades, executes, verifies, and reports. ## The Problem

You need to rebalance an investment portfolio back to its target allocation. The workflow analyzes how far the current holdings have drifted from the target allocation, determines the trades needed to bring allocations back in line, executes those trades, verifies the resulting positions, and generates a rebalancing report. Without periodic rebalancing, a portfolio's risk profile drifts away from the investor's strategy as different asset classes outperform or underperform.

Without orchestration, you'd build a rebalancing script that calculates drift, generates trade lists, submits orders, and checks fills. manually handling partial fills, tax-loss harvesting opportunities, wash sale rules, and logging every trade for compliance with the client's investment policy statement.

## The Solution

**You just write the rebalancing workers. Drift analysis, trade determination, execution, position verification, and reporting. Conductor handles step sequencing, automatic retries on failed trade executions, and a complete rebalancing audit trail for investment policy compliance.**

Each rebalancing concern is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of executing them in order (analyze drift, determine trades, execute, verify, report), retrying failed trade executions, tracking the entire rebalancing operation with audit trail, and resuming from the last step if the process crashes. ### What You Write: Workers

Five workers manage the rebalancing process: AnalyzeDriftWorker measures allocation drift, DetermineTradesWorker calculates required trades, ExecuteTradesWorker submits orders, VerifyWorker confirms resulting positions, and ReportWorker generates the rebalancing summary.

| Worker | Task | What It Does |
|---|---|---|
| **AnalyzeDriftWorker** | `prt_analyze_drift` | Analyzing drift for portfolio |
| **DetermineTradesWorker** | `prt_determine_trades` | Rebalancing trades needed |
| **ExecuteTradesWorker** | `prt_execute_trades` | Execute Trades. Computes and returns executed trades, trade count |
| **ReportWorker** | `prt_report` | Generates a rebalancing report summarizing the number of trades executed and verification status, producing a report ID and timestamp |
| **VerifyWorker** | `prt_verify` | Verifies and computes verified, new allocations |

Replace with real financial system integrations and the workflow, audit trail, and compliance logic stay the same.

### The Workflow

```
prt_analyze_drift
 │
 ▼
prt_determine_trades
 │
 ▼
prt_execute_trades
 │
 ▼
prt_verify
 │
 ▼
prt_report

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
