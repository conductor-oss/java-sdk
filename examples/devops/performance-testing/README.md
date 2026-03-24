# Load Testing at 1000 RPS with SLO Verification

Before a major release, you need proof the service handles production-level traffic without
breaching SLO thresholds. This workflow provisions a load test environment, runs a test at
1000 requests per second for 5 minutes, analyzes the results against SLO targets, and
generates a report with trend analysis.

## Workflow

```
service, targetRps, duration
           |
           v
+------------------+     +--------------------+     +---------------------+     +---------------------+
| pt_prepare_env   | --> | pt_run_load_test   | --> | pt_analyze_results  | --> | pt_generate_report  |
+------------------+     +--------------------+     +---------------------+     +---------------------+
  PREPARE_ENV-1430        1000 RPS for 5m            within SLO thresholds      report with trend
  environment ready                                                              analysis
```

## Workers

**PrepareEnvWorker** -- Provisions the load test environment. Returns
`prepare_envId: "PREPARE_ENV-1430"`.

**RunLoadTestWorker** -- Runs the load test at 1000 RPS for 5 minutes. Returns
`run_load_test: true`.

**AnalyzeResultsWorker** -- Confirms performance is within SLO thresholds. Returns
`analyze_results: true`.

**GenerateReportWorker** -- Generates a performance report with trend analysis. Returns
`generate_report: true`.

## Tests

2 unit tests cover the performance testing pipeline.

## Running

See [../../RUNNING.md](../../RUNNING.md) for setup and execution instructions.
