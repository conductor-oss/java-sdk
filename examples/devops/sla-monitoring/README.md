# SLA Monitoring in Java with Conductor

Automates SLA/SLO monitoring using [Conductor](https://github.com/conductor-oss/conductor). This workflow collects service level indicators (availability, latency), calculates error budget burn rate, evaluates compliance against SLO targets, and generates stakeholder reports.

## Burning Through Your Error Budget

Your payment-api promises 99.95% availability and p99 latency under 200ms. How much error budget do you have left this month? Are you burning it faster than expected? This workflow answers those questions by collecting SLIs, computing the remaining error budget (e.g., 21.6 minutes of allowed downtime left), and flagging when you are at risk of violating your SLA before the month ends.

Without orchestration, you'd query Prometheus manually, open a spreadsheet to calculate remaining error budget, eyeball whether the burn rate is accelerating, and send a monthly email to stakeholders with numbers you copied by hand. If the SLI collection fails or returns stale data, the budget calculation is wrong, and you either miss a breach or cry wolf. There's no audit trail of which SLOs were evaluated, what the burn rate looked like over time, or when the team was notified.

## The Solution

**You write the SLI collection and budget calculation logic. Conductor handles the measurement-to-report pipeline and compliance audit history.**

Each stage of the SLA monitoring pipeline is a simple, independent worker. The SLI collector gathers availability and latency measurements for the target service over the monitoring window. Uptime percentage, request success rate, P50/P95/P99 latency. The budget calculator computes how much error budget remains (e.g., 73% remaining, 21.6 minutes of allowed downtime left this month) and whether the current burn rate will exhaust it before the period ends. The compliance evaluator compares actual SLIs against contractual SLO targets and flags violations or at-risk services. The reporter generates a stakeholder-ready summary with per-SLO compliance status, budget remaining, and trend indicators. Conductor executes them in strict sequence, ensures budget calculations only run on fresh SLI data, retries if the metrics backend is temporarily unavailable, and tracks every monitoring run so you can audit SLA compliance history.

### What You Write: Workers

Four workers run the SLA monitoring cycle. Measuring service level indicators, computing error budget burn rate, evaluating compliance, and generating stakeholder reports.

| Worker | Task | What It Does |
|---|---|---|
| **CalculateBudgetWorker** | `sla_calculate_budget` | Computes remaining error budget and burn rate (e.g., 73% remaining, 21.6 min left this month) |
| **CollectSlisWorker** | `sla_collect_slis` | Measures current service level indicators: availability percentage and p99 latency |
| **EvaluateComplianceWorker** | `sla_evaluate_compliance` | Determines whether the service is currently within SLA compliance based on error budget |
| **ReportWorker** | `sla_report` | Generates an SLA compliance report for stakeholders with budget status and trend data |

the workflow and rollback logic stay the same.

### The Workflow

```
sla_collect_slis
 │
 ▼
sla_calculate_budget
 │
 ▼
sla_evaluate_compliance
 │
 ▼
sla_report

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
