# Credit Scoring in Java with Conductor

Credit scoring: collect data, calculate factors, compute score, classify applicant. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## The Problem

You need to compute a credit score for a loan applicant. This requires collecting financial data (credit history, income, debt), calculating individual scoring factors (payment history, utilization ratio, length of credit, new inquiries), computing a composite score, and classifying the applicant into a risk tier (excellent, good, fair, poor). Each scoring factor depends on the collected data, and the classification depends on the composite score.

Without orchestration, you'd build a single scoring engine that queries credit bureaus, computes factors inline, weights them, and classifies. manually handling bureau API timeouts, caching expensive credit pulls, and logging every factor to explain scores to regulators and applicants who dispute their rating.

## The Solution

**You just write the scoring workers. Financial data collection, factor calculation, composite scoring, and risk classification. Conductor handles pipeline ordering, automatic retries when a credit bureau is slow, and complete factor-level logging for Fair Lending compliance.**

Each scoring concern is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of executing them in order (collect data, calculate factors, compute score, classify), retrying if a credit bureau is slow, tracking every scoring decision with full factor breakdown for Fair Lending compliance, and resuming from the last step if the process crashes. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Four workers form the scoring pipeline: CollectDataWorker pulls credit history, CalculateFactorsWorker computes weighted scoring components, ScoreWorker produces the composite score, and ClassifyWorker assigns the risk tier.

| Worker | Task | What It Does |
|---|---|---|
| **CalculateFactorsWorker** | `csc_calculate_factors` | Calculates weighted credit score factors from credit history. |
| **ClassifyWorker** | `csc_classify` | Classifies an applicant based on credit score. |
| **CollectDataWorker** | `csc_collect_data` | Collects credit history data for an applicant. |
| **ScoreWorker** | `csc_score` | Computes a weighted credit score from factor data. |

Workers implement financial operations. risk assessment, compliance checks, settlement,  with realistic outputs. Replace with real financial system integrations and the workflow, audit trail, and compliance logic stay the same.

### The Workflow

```
csc_collect_data
    │
    ▼
csc_calculate_factors
    │
    ▼
csc_score
    │
    ▼
csc_classify

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
java -jar target/credit-scoring-1.0.0.jar

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
java -jar target/credit-scoring-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow credit_scoring_workflow \
  --version 1 \
  --input '{"applicantId": "TEST-001", "ssn": "sample-ssn"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w credit_scoring_workflow -s COMPLETED -c 5

```

## How to Extend

Connect CollectDataWorker to a credit bureau API (Experian, TransUnion, Equifax), CalculateFactorsWorker to your scoring model, and ClassifyWorker to your lending decision engine. The workflow definition stays exactly the same.

- **Data collector**: pull credit reports from Experian/Equifax/TransUnion, income data from payroll APIs, and bank statements from Plaid
- **Factor calculator**: compute FICO-style factors: payment history (35%), utilization (30%), credit age (15%), new inquiries (10%), credit mix (10%)
- **Score computer**: apply your proprietary scoring model or integrate with FICO, VantageScore, or custom ML models
- **Classifier**: map scores to risk tiers with configurable thresholds; generate adverse action notices for declined applicants per FCRA/ECOA

Replace demo bureau data with real credit bureau API calls while maintaining the same output structure, and the scoring pipeline operates without modification.

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
credit-scoring/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/creditscoring/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── CreditScoringExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CalculateFactorsWorker.java
│       ├── ClassifyWorker.java
│       ├── CollectDataWorker.java
│       └── ScoreWorker.java
└── src/test/java/creditscoring/workers/
    ├── CalculateFactorsWorkerTest.java        # 8 tests
    ├── ClassifyWorkerTest.java        # 11 tests
    ├── CollectDataWorkerTest.java        # 8 tests
    └── ScoreWorkerTest.java        # 8 tests

```
