# Performance Testing in Java with Conductor : Environment Prep, Load Testing, Results Analysis, and Report Generation

Orchestrates automated performance testing using [Conductor](https://github.com/conductor-oss/conductor). This workflow prepares a test environment, runs a load test against the target service at a specified requests-per-second rate for a configured duration, analyzes the results (P50/P95/P99 latency, error rates, throughput), and generates a performance report with pass/fail against SLO targets.

## Can Your Service Handle the Load?

Before launching a marketing campaign that will 5x your traffic, you need to know if your API can handle 10,000 requests per second without latency spiking above 200ms. Performance testing requires a clean test environment (isolated from production traffic), a load generator that ramps to the target RPS over the configured duration, analysis of the results (did P99 latency stay under 500ms? did error rate stay below 1%?), and a report that tells the team whether it's safe to launch. Each step depends on the previous one: you can't analyze results before the test runs, and the test can't run until the environment is ready.

Without orchestration, you'd SSH into a load testing box, run a k6 or JMeter script, eyeball the Grafana dashboard, and send a Slack message saying "looks good." There's no structured report, no comparison against SLO targets, no record of which test ran at what RPS for how long, and no audit trail for the next person who asks "did we performance test this release?"

## The Solution

**You write the load generation and analysis logic. Conductor handles environment-to-report sequencing, SLO comparison, and test result tracking.**

Each stage of the performance testing pipeline is a simple, independent worker. The environment preparer provisions or configures the test environment. Spinning up isolated infrastructure, warming caches, and seeding test data. The load tester generates traffic at the target RPS for the specified duration, recording response times and error counts. The results analyzer computes percentile latencies (P50, P95, P99), throughput, error rates, and compares them against SLO thresholds. The report generator produces a structured performance report with pass/fail verdicts and trend comparisons against previous runs. Conductor executes them in strict sequence, ensures the load test only runs after the environment is ready, retries if environment provisioning fails, and tracks all test parameters and results.

### What You Write: Workers

Three workers execute the performance test. Preparing the environment, running the load test at target RPS, and analyzing results against SLO thresholds.

| Worker | Task | What It Does |
|---|---|---|
| **AnalyzeResultsWorker** | `pt_analyze_results` | Compares load test results against SLO thresholds (P99 latency, error rates) to determine pass/fail |
| **GenerateReportWorker** | `pt_generate_report` | Produces a performance report with percentile latencies, throughput, and trend analysis |
| **PrepareEnvWorker** | `pt_prepare_env` | Provisions and configures an isolated load test environment (warm caches, seed data) |

the workflow and rollback logic stay the same.

### The Workflow

```
pt_prepare_env
 │
 ▼
pt_run_load_test
 │
 ▼
pt_analyze_results
 │
 ▼
pt_generate_report

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
