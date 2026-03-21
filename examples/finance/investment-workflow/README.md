# Investment Workflow in Java with Conductor

Investment lifecycle: research, analyze, decide, execute, monitor. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## The Problem

You need to manage the full investment lifecycle for a security. This means researching the investment opportunity (fundamentals, market conditions), analyzing risk and return potential, making a buy/hold/pass decision, executing the trade if appropriate, and monitoring the position post-investment. Making investment decisions without research leads to uninformed bets; executing without analysis means ignoring risk-return tradeoffs.

Without orchestration, you'd build a single investment platform that pulls market data, runs analysis models, places trades, and monitors positions. manually tracking which opportunities are being researched, retrying failed market data API calls, and logging every decision for compliance with fiduciary duty requirements.

## The Solution

**You just write the investment workers. Opportunity research, risk-return analysis, buy/hold/pass decision, trade execution, and position monitoring. Conductor handles lifecycle ordering, automatic retries when market data APIs time out, and complete decision tracking for fiduciary compliance.**

Each investment concern is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of executing them in order (research, analyze, decide, execute, monitor), retrying if market data APIs time out, tracking every investment decision with full rationale, and resuming from the last step if the process crashes. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Five workers manage the investment lifecycle: ResearchWorker gathers fundamentals and market data, AnalyzeWorker evaluates risk-return profiles, DecideWorker makes the buy/hold/pass decision, ExecuteWorker places the trade, and MonitorWorker tracks the position.

| Worker | Task | What It Does |
|---|---|---|
| **AnalyzeWorker** | `ivt_analyze` | Analyzes risk-return profile for the investment opportunity |
| **DecideWorker** | `ivt_decide` | Decide. Computes and returns action, shares, price limit, order type |
| **ExecuteWorker** | `ivt_execute` | Executes the operation and computes trade id, executed price, total cost |
| **MonitorWorker** | `ivt_monitor` | Monitoring trade |
| **ResearchWorker** | `ivt_research` | Researches the ticker symbol and returns fundamental data (P/E ratio, revenue, earnings growth, dividend yield) and market data (price, volume, beta, 200-day SMA) along with sector classification |

Workers implement financial operations. risk assessment, compliance checks, settlement,  with realistic outputs. Replace with real financial system integrations and the workflow, audit trail, and compliance logic stay the same.

### The Workflow

```
ivt_research
    │
    ▼
ivt_analyze
    │
    ▼
ivt_decide
    │
    ▼
ivt_execute
    │
    ▼
ivt_monitor

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
java -jar target/investment-workflow-1.0.0.jar

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
java -jar target/investment-workflow-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow investment_workflow \
  --version 1 \
  --input '{"tickerSymbol": "sample-tickerSymbol", "investorId": "TEST-001", "maxInvestment": "sample-maxInvestment"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w investment_workflow -s COMPLETED -c 5

```

## How to Extend

Connect ResearchWorker to your market data provider (Bloomberg, Refinitiv), AnalyzeWorker to your portfolio analytics engine, and ExecuteWorker to your broker's order management system. The workflow definition stays exactly the same.

- **Researcher**: pull fundamental data (SEC filings via EDGAR, earnings reports) and market data (Bloomberg, Refinitiv, Alpha Vantage APIs)
- **Analyzer**: run DCF models, comparable analysis, or quantitative factor models for risk-adjusted return estimation
- **Decision maker**: apply investment policy constraints (concentration limits, sector allocation, ESG criteria) to make buy/hold/pass decisions
- **Trade executor**: place orders via your broker/custodian API (Interactive Brokers, Charles Schwab, FIX protocol)
- **Position monitor**: track P&L, unrealized gains, and trigger stop-loss/take-profit alerts

Connect each worker to real market data providers and your brokerage execution API while keeping the same output structure, and the investment workflow operates without modification.

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
investment-workflow/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/investmentworkflow/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── InvestmentWorkflowExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AnalyzeWorker.java
│       ├── DecideWorker.java
│       ├── ExecuteWorker.java
│       ├── MonitorWorker.java
│       └── ResearchWorker.java
└── src/test/java/investmentworkflow/workers/
    ├── DecideWorkerTest.java        # 2 tests
    └── MonitorWorkerTest.java        # 2 tests

```
