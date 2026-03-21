# Actuarial Workflow in Java with Conductor : Collect Data, Build Model, Run Simulations, Analyze, Report

## Actuarial Analysis Combines Statistics, Simulation, and Reporting

Setting insurance rates requires actuarial analysis: collect 10 years of loss data for a line of business, build a frequency-severity model, run 10,000 Monte Carlo simulations to estimate the loss distribution, analyze the results (mean, percentiles, confidence intervals), and produce a report for rate filings and reserve setting.

The simulation step is computationally intensive. 10,000 iterations of a stochastic model can take hours. If it crashes at iteration 8,000, you need to restart from the simulation step with the model intact, not recollect the data. The analysis step needs all simulation results before computing the aggregate statistics. And the report must meet regulatory standards for rate filing documentation.

## The Solution

**You just write the data collection, model building, simulation execution, result analysis, and report generation logic. Conductor handles modeling retries, reserve calculation sequencing, and actuarial audit trails.**

`CollectDataWorker` gathers historical loss data. claim frequencies, severities, development patterns, and exposure measures for the specified line of business and analysis period. `ModelWorker` builds the actuarial model, fitting frequency and severity distributions, calibrating parameters, and validating against holdout data. `RunSimulationsWorker` executes Monte Carlo simulations using the fitted model, generating thousands of loss scenarios to estimate the full loss distribution. `AnalyzeWorker` computes summary statistics from the simulation results, mean, percentiles (p75, p90, p99), confidence intervals, and tail risk measures. `ReportWorker` generates the actuarial report formatted for rate filing submission or reserve setting. Conductor tracks each analysis run for reproducibility.

### What You Write: Workers

Data extraction, loss modeling, reserve calculation, and report generation workers each own one step of the actuarial analysis process.

| Worker | Task | What It Does |
|---|---|---|
| **CollectDataWorker** | `act_collect_data` | Gathers historical loss data. collects claim frequencies, severities, and exposure measures for the specified line of business and analysis year |
| **ModelWorker** | `act_model` | Builds the actuarial model. fits frequency and severity distributions to the collected dataset (45K records), calibrates parameters, and outputs the modelId for simulation |
| **RunSimulationsWorker** | `act_run_simulations` | Runs Monte Carlo simulations. executes 10,000 iterations using the fitted model to generate the full loss distribution for the line of business |
| **AnalyzeWorker** | `act_analyze` | Computes summary statistics. calculates expected loss, tail risk measures (percentiles, confidence intervals), and aggregate risk metrics from the simulation results |
| **ReportWorker** | `act_report` | Generates the actuarial report. produces the rate filing documentation with risk projections, reserve recommendations, and supporting exhibits for the line of business |

### The Workflow

```
act_collect_data
 │
 ▼
act_model
 │
 ▼
act_run_simulations
 │
 ▼
act_analyze
 │
 ▼
act_report

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
