# Simple Plus System in Java with Conductor

Combines SIMPLE workers with INLINE system tasks.

## The Problem

You need to build a store sales report: fetch orders from a database, calculate statistics (total revenue, average order value, max order), generate a visual chart, and format a summary email. Some of these steps require external services (database queries, chart generation APIs) and need dedicated workers. Others are simple calculations or string formatting that can run as lightweight JavaScript on the server. Building a worker for every step. including trivial math and string formatting, adds unnecessary operational overhead.

Without orchestration, you'd write a single reporting class that mixes data fetching, statistical calculation, chart generation, and email formatting in one monolithic method. The lightweight calculations are buried alongside expensive API calls, making it impossible to retry just the chart generation if the charting service is down. There is no visibility into which step produced which intermediate result.

## The Solution

**You just write the data fetch and chart generation workers. Lightweight calculations and formatting run as server-side INLINE tasks. Conductor handles mixing SIMPLE workers with server-side INLINE tasks, retries for external service calls, and seamless data passing between worker and system task outputs.**

This example demonstrates mixing SIMPLE workers (external processing) with INLINE system tasks (server-side JavaScript) in the same workflow. FetchOrdersWorker (SIMPLE) queries order data for a storeId and date range from a database. this needs a real worker because it calls external services. The `calculate_stats` step (INLINE) computes total revenue, average order value, max order, and order count using JavaScript on the Conductor server, no worker needed for basic math. GenerateVisualReportWorker (SIMPLE) takes the computed stats and generates a chart, this needs a worker because it calls a charting service. The `format_summary` step (INLINE) builds a summary email body from the stats and chart URL, no worker needed for string formatting. You deploy workers only for steps that require external access.

### What You Write: Workers

Two SIMPLE workers handle external interactions. FetchOrdersWorker queries order data from a database, and GenerateVisualReportWorker calls a charting service. While lightweight statistics and formatting run as server-side INLINE JavaScript tasks.

| Worker | Task | What It Does |
|---|---|---|
| **FetchOrdersWorker** | `fetch_orders` | SIMPLE worker that fetches order data for a given store. In a real system this would call a database or API. Here it ... |
| **GenerateVisualReportWorker** | `generate_visual_report` | SIMPLE worker that generates a visual report (chart) from stats. In a real system this would call a charting service ... |

Workers implement their processing steps so you can see the pattern in action without external services. Replace the simulation with real processing logic. the task pattern and Conductor orchestration remain unchanged.

### The Workflow

```
fetch_orders
 │
 ▼
calculate_stats [INLINE]
 │
 ▼
generate_visual_report
 │
 ▼
format_summary [INLINE]

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
