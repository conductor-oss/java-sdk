# Automated Testing Pipeline in Java with Conductor : Setup Environment, Parallel Test Suites, Aggregate Results

Orchestrates a test suite: setup environment, run unit/integration/e2e tests in parallel, aggregate results.

## Parallel Testing Cuts Pipeline Time by 3x

A test suite takes 30 minutes to run sequentially: 10 minutes for unit tests, 15 minutes for integration tests, 5 minutes for performance tests. Running all three in parallel brings it down to 15 minutes (the slowest suite). But parallel execution requires a shared environment setup, independent test runners, and a final aggregation step that merges results from all three suites.

If integration tests fail, unit test results are still valid. you don't need to re-run them. If the environment setup fails, no tests should run. After all suites complete, the aggregation step needs to produce a single pass/fail verdict with per-suite breakdowns, coverage metrics, and failure details.

## The Solution

**You write the test runners and environment setup. Conductor handles parallel execution, result aggregation, and per-suite failure isolation.**

`SetupEnvWorker` provisions the test environment. spinning up test databases, mock services, and test fixtures for the specified branch. `FORK_JOIN` dispatches parallel test suites: unit tests, integration tests, and performance tests run simultaneously in isolated runners. After `JOIN` collects all results, `AggregateResultsWorker` merges the per-suite reports into a unified test report with pass/fail counts, coverage metrics, failure details, and execution times. Conductor runs all suites in parallel and records each suite's results independently.

### What You Write: Workers

Five workers run the test pipeline. Setting up the environment, executing unit/integration/e2e suites in parallel, and aggregating results into a unified report.

| Worker | Task | What It Does |
|---|---|---|
| **AggregateResults** | `at_aggregate_results` | Aggregates test results from all suites. |
| **RunE2e** | `at_run_e2e` | Runs end-to-end tests. |
| **RunIntegration** | `at_run_integration` | Runs integration tests. |
| **RunUnit** | `at_run_unit` | Runs unit tests. |
| **SetupEnv** | `at_setup_env` | Sets up the test environment. |

the workflow and rollback logic stay the same.

### The Workflow

```
at_setup_env
 │
 ▼
FORK_JOIN
 ├── at_run_unit
 ├── at_run_integration
 └── at_run_e2e
 │
 ▼
JOIN (wait for all branches)
at_aggregate_results

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
