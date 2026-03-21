# Risk Assessment in Java with Conductor

Risk assessment workflow with parallel market, credit, and operational risk analysis via FORK_JOIN. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## The Problem

You need to assess the total risk exposure of a portfolio across multiple risk dimensions simultaneously. Market risk (price volatility, interest rate sensitivity), credit risk (counterparty default probability), and operational risk (process failures, fraud) must all be analyzed in parallel since they are independent calculations. The combined results provide a holistic view of the portfolio's risk profile. Running these assessments sequentially wastes time when each can take minutes to compute.

Without orchestration, you'd spawn threads for each risk model, synchronize completion with barriers, aggregate results from different risk engines, and handle partial failures when one model crashes while others succeed. all while ensuring each risk calculation uses consistent market data as of the same assessment date.

## The Solution

**You just write the risk analysis workers. Factor collection and parallel market, credit, and operational risk scoring, plus aggregation. Conductor handles parallel FORK_JOIN execution of market, credit, and operational risk analyses, automatic retries on any failed dimension, and complete assessment tracking.**

Each risk dimension is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of running market, credit, and operational risk analyses in parallel via FORK_JOIN, waiting for all to complete, aggregating the combined risk profile, retrying any failed analysis independently, and tracking every assessment. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Five workers span the risk assessment: CollectFactorsWorker gathers portfolio risk factors, then MarketRiskWorker, CreditRiskWorker, and OperationalRiskWorker run in parallel via FORK_JOIN, and AggregateWorker combines the results into a holistic risk profile.

| Worker | Task | What It Does |
|---|---|---|
| **AggregateWorker** | `rsk_aggregate` | Aggregates market, credit, and operational risk scores into an overall assessment. |
| **CollectFactorsWorker** | `rsk_collect_factors` | Collects risk factors for the portfolio. |
| **CreditRiskWorker** | `rsk_credit_risk` | Analyzes credit risk using default rates and concentration data. |
| **MarketRiskWorker** | `rsk_market_risk` | Analyzes market risk using volatility, beta, and correlation data. |
| **OperationalRiskWorker** | `rsk_operational_risk` | Analyzes operational risk from incident, control gap, and maturity data. |

Workers implement financial operations. risk assessment, compliance checks, settlement,  with realistic outputs. Replace with real financial system integrations and the workflow, audit trail, and compliance logic stay the same.

### The Workflow

```
rsk_collect_factors
    │
    ▼
FORK_JOIN
    ├── rsk_market_risk
    ├── rsk_credit_risk
    └── rsk_operational_risk
    │
    ▼
JOIN (wait for all branches)
rsk_aggregate

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
java -jar target/risk-assessment-1.0.0.jar

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
java -jar target/risk-assessment-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow risk_assessment_workflow \
  --version 1 \
  --input '{"portfolioId": "TEST-001", "assessmentDate": "2026-01-01T00:00:00Z"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w risk_assessment_workflow -s COMPLETED -c 5

```

## How to Extend

Connect MarketRiskWorker to your market data and VaR model, CreditRiskWorker to your counterparty default analysis, and OperationalRiskWorker to your incident and controls database. The workflow definition stays exactly the same.

- **Market risk analyzer**: compute VaR, Expected Shortfall, and Greeks using your risk engine (RiskMetrics, MSCI Barra, custom Monte Carlo simulations)
- **Credit risk analyzer**: calculate PD, LGD, EAD using credit risk models (Moody's Analytics, S&P Capital IQ); run portfolio-level credit VaR
- **Operational risk analyzer**: assess operational risk using loss distribution approach, scenario analysis, or key risk indicators from your GRC platform
- **Risk aggregator**: combine risk measures across dimensions, compute diversification benefits, and generate enterprise risk dashboards

Point each risk worker at your real risk models and market data feeds while maintaining the same output structure, and the assessment workflow: including FORK_JOIN parallelism, runs without modification.

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
risk-assessment/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/riskassessment/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── RiskAssessmentExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AggregateWorker.java
│       ├── CollectFactorsWorker.java
│       ├── CreditRiskWorker.java
│       ├── MarketRiskWorker.java
│       └── OperationalRiskWorker.java
└── src/test/java/riskassessment/workers/
    ├── AggregateWorkerTest.java        # 5 tests
    └── CollectFactorsWorkerTest.java        # 2 tests

```
