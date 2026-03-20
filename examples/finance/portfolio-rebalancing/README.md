# Portfolio Rebalancing in Java with Conductor

Portfolio rebalancing workflow that analyzes drift, determines trades, executes, verifies, and reports. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers .  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## The Problem

You need to rebalance an investment portfolio back to its target allocation. The workflow analyzes how far the current holdings have drifted from the target allocation, determines the trades needed to bring allocations back in line, executes those trades, verifies the resulting positions, and generates a rebalancing report. Without periodic rebalancing, a portfolio's risk profile drifts away from the investor's strategy as different asset classes outperform or underperform.

Without orchestration, you'd build a rebalancing script that calculates drift, generates trade lists, submits orders, and checks fills .  manually handling partial fills, tax-loss harvesting opportunities, wash sale rules, and logging every trade for compliance with the client's investment policy statement.

## The Solution

**You just write the rebalancing workers. Drift analysis, trade determination, execution, position verification, and reporting. Conductor handles step sequencing, automatic retries on failed trade executions, and a complete rebalancing audit trail for investment policy compliance.**

Each rebalancing concern is a simple, independent worker .  a plain Java class that does one thing. Conductor takes care of executing them in order (analyze drift, determine trades, execute, verify, report), retrying failed trade executions, tracking the entire rebalancing operation with audit trail, and resuming from the last step if the process crashes. You get all of that for free, without writing a single line of orchestration code.

### What You Write: Workers

Five workers manage the rebalancing process: AnalyzeDriftWorker measures allocation drift, DetermineTradesWorker calculates required trades, ExecuteTradesWorker submits orders, VerifyWorker confirms resulting positions, and ReportWorker generates the rebalancing summary.

| Worker | Task | What It Does |
|---|---|---|
| **AnalyzeDriftWorker** | `prt_analyze_drift` | Analyzing drift for portfolio |
| **DetermineTradesWorker** | `prt_determine_trades` | Rebalancing trades needed |
| **ExecuteTradesWorker** | `prt_execute_trades` | Execute Trades. Computes and returns executed trades, trade count |
| **ReportWorker** | `prt_report` | Generates a rebalancing report summarizing the number of trades executed and verification status, producing a report ID and timestamp |
| **VerifyWorker** | `prt_verify` | Verifies and computes verified, new allocations |

Workers simulate financial operations .  risk assessment, compliance checks, settlement ,  with realistic outputs. Replace with real financial system integrations and the workflow, audit trail, and compliance logic stay the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
prt_analyze_drift
    │
    ▼
prt_determine_trades
    │
    ▼
prt_execute_trades
    │
    ▼
prt_verify
    │
    ▼
prt_report
```

## Example Output

```
=== Example 496: Portfolio Rebalancing ===

Step 1: Registering task definitions...
  Registered: prt_analyze_drift, prt_determine_trades, prt_execute_trades, prt_verify, prt_report

Step 2: Registering workflow 'portfolio_rebalancing_workflow'...
  Workflow registered.

Step 3: Starting workers...
  5 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [drift] Analyzing drift for portfolio
  [trades]
  [execute] Executing
  [report] Rebalancing report
  [verify]

  Status: COMPLETED
  Output: {driftAnalysis=..., maxDrift=..., rebalanceNeeded=..., trades=...}

Result: PASSED
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
docker run -d -p 8080:8080 -p 1234:5000 orkesio/orkes-conductor-standalone:latest

# Wait for Conductor to be ready
until curl -sf http://localhost:8080/health > /dev/null; do sleep 2; done

# Build and run
mvn package -DskipTests
java -jar target/portfolio-rebalancing-1.0.0.jar
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
java -jar target/portfolio-rebalancing-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow portfolio_rebalancing_workflow \
  --version 1 \
  --input '{"portfolioId": "PORT-INST-001", "PORT-INST-001": "accountId", "accountId": "ACCT-FIN-5501", "ACCT-FIN-5501": "strategy", "strategy": "60_20_15_5", "60_20_15_5": "sample-60-20-15-5"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w portfolio_rebalancing_workflow -s COMPLETED -c 5
```

## How to Extend

Connect AnalyzeDriftWorker to your portfolio accounting system, DetermineTradesWorker to your rebalancing optimizer, and ExecuteTradesWorker to your broker's order management system. The workflow definition stays exactly the same.

- **Drift analyzer**: calculate asset allocation drift against target weights using live portfolio data from your custodian (Schwab, Fidelity, Pershing)
- **Trade determiner**: generate optimal trade list considering tax-loss harvesting, wash sale windows, transaction costs, and minimum trade sizes
- **Trade executor**: place orders via FIX protocol or broker API (Interactive Brokers, TD Ameritrade) with proper order types
- **Position verifier**: confirm post-rebalancing positions match target allocation within tolerance bands
- **Report generator**: produce rebalancing reports for compliance and client review with before/after allocations and trade details

Swap in your real portfolio management and order execution systems while keeping the same output fields, and the rebalancing workflow needs no modifications.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):

```xml
<dependency>
    <groupId>org.conductoross</groupId>
    <artifactId>conductor-client</artifactId>
    <version>5.0.1</version>
</dependency>
```

## Project Structure

```
portfolio-rebalancing/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/portfoliorebalancing/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── PortfolioRebalancingExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AnalyzeDriftWorker.java
│       ├── DetermineTradesWorker.java
│       ├── ExecuteTradesWorker.java
│       ├── ReportWorker.java
│       └── VerifyWorker.java
└── src/test/java/portfoliorebalancing/workers/
    ├── AnalyzeDriftWorkerTest.java        # 2 tests
    └── ReportWorkerTest.java        # 2 tests
```
