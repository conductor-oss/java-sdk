# User Analytics in Java Using Conductor

## The Problem

Your product team needs a periodic user analytics report covering engagement and retention metrics. The pipeline must collect raw user events (logins, page views, clicks) for a date range, aggregate them by day and user segment, compute key metrics like DAU, MAU, retention rate, and churn rate, and publish an updated analytics dashboard. Each step depends on the previous one's output.

Without orchestration, you'd wire all of this together in a single monolithic class. managing execution order manually, writing try/catch blocks around every step, building retry loops with backoff, and adding logging to understand what happened when things go wrong. That code becomes brittle, hard to test, and impossible to observe at scale.

## The Solution

**You just write the event-collection, aggregation, metrics-computation, and dashboard-publishing workers. Conductor handles the analytics pipeline and data flow.**

Each worker handles one user lifecycle step. Conductor manages the onboarding sequence, verification wait states, timeout escalation, and user state tracking.

### What You Write: Workers

CollectEventsWorker gathers login, page view, and click events, AggregateWorker groups them by day and segment, ComputeMetricsWorker calculates DAU, retention, and churn, and AnalyticsReportWorker publishes the dashboard.

| Worker | Task | What It Does |
|---|---|---|
| **AggregateWorker** | `uan_aggregate` | Aggregates raw events by day and user segment, computing unique user counts |
| **AnalyticsReportWorker** | `uan_report` | Publishes the computed metrics to the analytics dashboard with a generated report URL |
| **CollectEventsWorker** | `uan_collect_events` | Collects user events (login, page_view, click) for the specified date range, returning total event count |
| **ComputeMetricsWorker** | `uan_compute_metrics` | Computes DAU, MAU, retention rate, average session duration, and churn rate from aggregated data |

Replace with real identity provider and database calls and ### The Workflow

```
uan_collect_events
 │
 ▼
uan_aggregate
 │
 ▼
uan_compute_metrics
 │
 ▼
uan_report

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
