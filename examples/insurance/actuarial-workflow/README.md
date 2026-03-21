# Actuarial Workflow in Java with Conductor :  Collect Data, Build Model, Run Simulations, Analyze, Report

A Java Conductor workflow example demonstrating actuarial-workflow Actuarial Workflow. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## Actuarial Analysis Combines Statistics, Simulation, and Reporting

Setting insurance rates requires actuarial analysis: collect 10 years of loss data for a line of business, build a frequency-severity model, run 10,000 Monte Carlo simulations to estimate the loss distribution, analyze the results (mean, percentiles, confidence intervals), and produce a report for rate filings and reserve setting.

The simulation step is computationally intensive. 10,000 iterations of a stochastic model can take hours. If it crashes at iteration 8,000, you need to restart from the simulation step with the model intact, not recollect the data. The analysis step needs all simulation results before computing the aggregate statistics. And the report must meet regulatory standards for rate filing documentation.

## The Solution

**You just write the data collection, model building, simulation execution, result analysis, and report generation logic. Conductor handles modeling retries, reserve calculation sequencing, and actuarial audit trails.**

`CollectDataWorker` gathers historical loss data. claim frequencies, severities, development patterns, and exposure measures for the specified line of business and analysis period. `ModelWorker` builds the actuarial model,  fitting frequency and severity distributions, calibrating parameters, and validating against holdout data. `RunSimulationsWorker` executes Monte Carlo simulations using the fitted model,  generating thousands of loss scenarios to estimate the full loss distribution. `AnalyzeWorker` computes summary statistics from the simulation results,  mean, percentiles (p75, p90, p99), confidence intervals, and tail risk measures. `ReportWorker` generates the actuarial report formatted for rate filing submission or reserve setting. Conductor tracks each analysis run for reproducibility.

### What You Write: Workers

Data extraction, loss modeling, reserve calculation, and report generation workers each own one step of the actuarial analysis process.

| Worker | Task | What It Does |
|---|---|---|
| **CollectDataWorker** | `act_collect_data` | Gathers historical loss data. collects claim frequencies, severities, and exposure measures for the specified line of business and analysis year |
| **ModelWorker** | `act_model` | Builds the actuarial model. fits frequency and severity distributions to the collected dataset (45K records), calibrates parameters, and outputs the modelId for simulation |
| **RunSimulationsWorker** | `act_run_simulations` | Runs Monte Carlo simulations. executes 10,000 iterations using the fitted model to generate the full loss distribution for the line of business |
| **AnalyzeWorker** | `act_analyze` | Computes summary statistics. calculates expected loss, tail risk measures (percentiles, confidence intervals), and aggregate risk metrics from the simulation results |
| **ReportWorker** | `act_report` | Generates the actuarial report. produces the rate filing documentation with risk projections, reserve recommendations, and supporting exhibits for the line of business |

Workers implement insurance operations. claim intake, assessment, settlement,  with realistic outputs. Replace with real claims management and underwriting integrations and the workflow stays the same.

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

## Running It

### Prerequisites

- **Java 21+**: verify with `java -version`
- **Maven 3.8+**: verify with `mvn -version`
- **Docker**: to run Conductor

### Option 1: Docker Compose (everything included)

```bash
docker compose up --build

```

Starts Conductor on port 8080 and runs the example automatically.

If port 8080 is already taken:

```bash
CONDUCTOR_PORT=9090 docker compose up --build

```

### Option 2: Run locally

```bash
# Start Conductor
docker run -d -p 8080:8080 -p 1234:5000 orkesio/orkes-conductor-standalone:1.2.3

# Wait for Conductor to be ready
until curl -sf http://localhost:8080/health > /dev/null; do sleep 2; done

# Build and run
mvn package -DskipTests
java -jar target/actuarial-workflow-1.0.0.jar

```

### Option 3: Use the run script

```bash
./run.sh

# Or on a custom port:
CONDUCTOR_PORT=9090 ./run.sh

# Or pointing at an existing Conductor:
CONDUCTOR_BASE_URL=http://localhost:9090/api ./run.sh

```

## Configuration

| Environment Variable | Default | Description |
|---|---|---|
| `CONDUCTOR_BASE_URL` | `http://localhost:8080/api` | Conductor server URL |
| `CONDUCTOR_PORT` | `8080` | Host port for Conductor (Docker Compose only) |

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/actuarial-workflow-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow act_actuarial_workflow \
  --version 1 \
  --input '{"lineOfBusiness": "sample-lineOfBusiness", "analysisYear": "sample-analysisYear", "modelType": "standard"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w act_actuarial_workflow -s COMPLETED -c 5

```

## How to Extend

Connect each worker to your real actuarial tools. your data warehouse for loss history, your modeling platform for Monte Carlo simulations, your BI tools for reserve and rate reports, and the workflow runs identically in production.

- **RunSimulationsWorker** (`act_run_simulations`): use R (via Rserve) or Python (scipy, actuarial libraries) for real Monte Carlo simulation with configurable iteration counts and parallel execution
- **ModelWorker** (`act_model`): implement chain-ladder development, Bornhuetter-Ferguson, or Cape Cod methods using actuarial libraries (chainladder-python, R actuar package)
- **ReportWorker** (`act_report`): generate NAIC-format actuarial reports, Statement of Actuarial Opinion documents, and rate filing exhibits with regulatory-compliant formatting

Swap loss models or reserve methodologies and the actuarial pipeline continues without restructuring.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):

## Project Structure

```
actuarial-workflow-actuarial-workflow/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/actuarialworkflow/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ActuarialWorkflowExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AnalyzeWorker.java
│       ├── CollectDataWorker.java
│       ├── ModelWorker.java
│       ├── ReportWorker.java
│       └── RunSimulationsWorker.java
└── src/test/java/actuarialworkflow/workers/
    ├── ModelWorkerTest.java        # 1 tests
    └── ReportWorkerTest.java        # 1 tests

```
