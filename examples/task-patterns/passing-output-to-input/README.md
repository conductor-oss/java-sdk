# Passing Output To Input in Java with Conductor

Shows all the ways to pass data between tasks.

## The Problem

You need to build a regional sales report in three stages: generate raw metrics and top products for a region and time period, enrich the report with computed growth rates and health insights, then summarize by combining the original metrics with the enriched data. The second step needs access to the first step's output (metrics object, individual revenue field, top products list). The third step needs data from both previous steps. the original metrics from step 1 and the enriched insights and growth rate from step 2. Each step consumes data wired from different upstream tasks.

Without orchestration, you'd pass data between functions manually. storing intermediate results in shared state, passing objects through method signatures, or writing to a database between steps. When the data shape changes in step 1, you have to trace through all downstream consumers to update them. There is no way to inspect what data each step received without adding debug logging at every boundary.

## The Solution

**You just write the report generation, enrichment, and summarization workers. Conductor handles all the data wiring between tasks via input template expressions.**

This example demonstrates all the ways Conductor wires data between tasks using `${taskRef.output}` expressions. GenerateReportWorker produces structured output (nested metrics object, top products array) for a given region and period. EnrichReportWorker receives data three different ways: the entire metrics object (`${report_ref.output.metrics}`), a specific nested field (`${report_ref.output.metrics.revenue}`), and the top products array, plus a workflow input value (`${workflow.input.region}`). SummarizeReportWorker pulls from two upstream tasks. original metrics from the generate step and enriched insights plus growth rate from the enrich step. Conductor records every input/output mapping, so you can see exactly what data each task received and produced.

### What You Write: Workers

Three workers build a sales report pipeline: GenerateReportWorker produces regional metrics and top products, EnrichReportWorker adds growth rates and insights using data wired from multiple sources, and SummarizeReportWorker combines outputs from both previous steps.

| Worker | Task | What It Does |
|---|---|---|
| **EnrichReportWorker** | `enrich_report` | Enriches a report by computing growth rate and health insights. Receives the entire metrics object, individual fields... |
| **GenerateReportWorker** | `generate_report` | Generates a report with metrics and top products for a given region and period. Outputs structured data (nested objec... |
| **SummarizeReportWorker** | `summarize_report` | Summarizes a report using data from both previous tasks. Demonstrates accessing outputs from multiple upstream task r... |

Workers implement their processing steps so you can see the pattern in action without external services. Replace the simulation with real processing logic. the task pattern and Conductor orchestration remain unchanged.

### The Workflow

```
generate_report
 │
 ▼
enrich_report
 │
 ▼
summarize_report

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
