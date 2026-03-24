# Stakeholder Reporting in Java with Conductor : Collect Updates, Aggregate, Format, and Distribute

## The Problem

You need to produce regular stakeholder reports for a project. Every reporting period, someone has to gather updates from engineering (sprint velocity, blockers), finance (burn rate, forecast), and program management (milestone status, risks). Those raw updates need to be aggregated into a single coherent summary, formatted into a professional report (PDF, slide deck, or dashboard), and then distributed to the right audience. executives get the high-level view, team leads get the details.

Without orchestration, this becomes a manual, error-prone process. Someone writes a script that queries Jira, pulls from Sheets, formats a PDF, and sends emails. all in one monolithic block. If the Jira API times out, the whole report fails. If the email step breaks, you don't know whether the report was generated. Nobody can tell which reporting period was last completed successfully.

## The Solution

**You just write the update collection, data aggregation, report formatting, and stakeholder distribution logic. Conductor handles data aggregation retries, report formatting, and distribution audit trails.**

Each step in the reporting pipeline is a simple, independent worker. one collects raw updates, one aggregates them into a summary, one formats the report, one distributes it. Conductor takes care of executing them in sequence, retrying if a data source is temporarily unavailable, tracking every report generation with full audit history, and resuming if the process crashes mid-generation.

### What You Write: Workers

Data aggregation, insight generation, report formatting, and distribution workers each handle one phase of keeping stakeholders informed.

| Worker | Task | What It Does |
|---|---|---|
| **CollectUpdatesWorker** | `shr_collect_updates` | Gathers raw project updates (sprint progress, budget status, milestone completion) for the given reporting period |
| **AggregateWorker** | `shr_aggregate` | Consolidates raw updates into a structured summary with key metrics, blockers, and highlights |
| **FormatWorker** | `shr_format` | Transforms the aggregated summary into a formatted report (executive brief, detailed breakdown) |
| **DistributeWorker** | `shr_distribute` | Delivers the finished report to stakeholders via email, Slack, or dashboard publication |

### The Workflow

```
shr_collect_updates
 │
 ▼
shr_aggregate
 │
 ▼
shr_format
 │
 ▼
shr_distribute

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
