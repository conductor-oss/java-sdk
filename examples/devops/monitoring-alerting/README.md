# Monitoring Alerting in Java with Conductor

Orchestrates a monitoring and alerting pipeline using [Conductor](https://github.com/conductor-oss/conductor). Incoming alerts are evaluated for severity, deduplicated against recent history, enriched with deployment context, and routed to the appropriate on-call channel.

## When Every Alert Counts

A metric threshold is breached and a raw alert fires. Before anyone gets paged, the system needs to decide whether this alert is real or a duplicate, determine its severity, pull in deployment context (was there a recent deploy?), and route the notification to the right Slack channel or PagerDuty escalation. Doing this manually means flapping alerts wake people up at 3 AM for problems that already have open incidents.

Without orchestration, you'd wire all of this together in a single monolithic class. managing execution order manually, writing try/catch blocks around every step, building retry loops with backoff, and adding logging to understand what happened when things go wrong. That code becomes brittle, hard to test, and impossible to observe at scale.

## The Solution

**You write the alert evaluation and routing logic. Conductor handles deduplication sequencing, severity-based routing, and delivery tracking.**

Each worker automates one operational step. Conductor manages execution sequencing, rollback on failure, timeout enforcement, and full audit logging. your workers call the infrastructure APIs.

### What You Write: Workers

These four workers form the alert-processing pipeline, from severity evaluation through deduplication, enrichment, and channel routing.

| Worker | Task | What It Does |
|---|---|---|
| **DeduplicateWorker** | `ma_deduplicate` | Checks whether this alert is a duplicate of a recently-fired alert and suppresses it if so |
| **EnrichWorker** | `ma_enrich` | Adds deployment context (recent deploys, config changes) to the alert payload |
| **EvaluateWorker** | `ma_evaluate` | Evaluates the incoming alert name and metric value to determine severity (info/warning/critical) |
| **RouteWorker** | `ma_route` | Routes the enriched alert to the correct notification channel based on severity (e.g., Slack for warnings, PagerDuty for critical) |

the workflow and rollback logic stay the same.

### The Workflow

```
ma_evaluate
 │
 ▼
ma_deduplicate
 │
 ▼
ma_enrich
 │
 ▼
ma_route

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
