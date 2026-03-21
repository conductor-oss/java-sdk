# Population Health Management in Java Using Conductor :  Data Aggregation, Risk Stratification, Care Gap Identification, and Intervention

A Java Conductor workflow example for population health management. aggregating clinical and claims data across a patient cohort, stratifying members by risk level, identifying care gaps (missed screenings, overdue vaccinations, uncontrolled chronic conditions), and triggering targeted interventions. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## The Problem

You need to manage population health for a patient cohort with a specific condition during a reporting period. Clinical and claims data must be aggregated from multiple sources. EHRs, claims feeds, pharmacy records, and lab results. Members must be risk-stratified into tiers (low, moderate, high, rising risk) based on HCC scores, utilization patterns, and social determinants. Care gaps must be identified. patients overdue for A1C tests, mammograms, colonoscopies, or flu vaccines per HEDIS/STAR quality measures. Targeted interventions must then be triggered,  outreach calls, care coordinator assignments, appointment scheduling, or pharmacy consultations. Each step depends on the previous one,  you cannot identify care gaps without risk stratification, and you cannot intervene without knowing which gaps exist.

Without orchestration, you'd build a monolithic population health analytics engine that queries your data warehouse, runs the risk model, scans for gaps, and generates outreach lists. If the claims data feed is delayed, the entire pipeline stalls. If the system crashes after risk stratification but before gap identification, you have risk scores but no actionable gaps. CMS quality programs (STARS, MIPS, MSSP) require documentation of every population health activity for performance reporting.

## The Solution

**You just write the population health workers. Data aggregation, risk stratification, care gap identification, and intervention triggering. Conductor handles pipeline ordering, automatic retries when a data feed is delayed, and complete documentation for CMS quality program reporting.**

Each stage of the population health pipeline is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of aggregating data before stratification, identifying gaps only after risk levels are assigned, triggering interventions only for identified gaps, and maintaining a complete audit trail for CMS quality reporting. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Four workers drive the population health pipeline: AggregateDataWorker pulls clinical and claims data, StratifyRiskWorker assigns risk tiers, IdentifyGapsWorker finds missed screenings and overdue interventions, and InterveneWorker triggers targeted outreach.

| Worker | Task | What It Does |
|---|---|---|
| **AggregateDataWorker** | `pop_aggregate_data` | Aggregates clinical, claims, pharmacy, and lab data for the specified cohort and reporting period |
| **StratifyRiskWorker** | `pop_stratify_risk` | Assigns risk tiers (low, moderate, high, rising risk) based on HCC scores, utilization, and SDOH factors |
| **IdentifyGapsWorker** | `pop_identify_gaps` | Scans for care gaps. overdue screenings, missing labs, uncontrolled chronic conditions per HEDIS/STARS measures |
| **InterveneWorker** | `pop_intervene` | Triggers targeted interventions. outreach calls, care coordinator assignments, appointment scheduling |

Workers implement clinical and administrative operations with realistic outputs so you can see the care workflow end-to-end. Replace with real EHR and system integrations. the workflow and compliance logic stay the same.

### The Workflow

```
pop_aggregate_data
    │
    ▼
pop_stratify_risk
    │
    ▼
pop_identify_gaps
    │
    ▼
pop_intervene

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
java -jar target/population-health-1.0.0.jar

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
java -jar target/population-health-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow population_health_workflow \
  --version 1 \
  --input '{"cohortId": "TEST-001", "condition": "sample-condition", "reportingPeriod": "sample-reportingPeriod"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w population_health_workflow -s COMPLETED -c 5

```

## How to Extend

Connect AggregateDataWorker to your data warehouse and HIE feeds, StratifyRiskWorker to your risk model (Johns Hopkins ACG, Milliman), and IdentifyGapsWorker to your HEDIS/STARS quality measure engine. The workflow definition stays exactly the same.

- **AggregateDataWorker** → query your clinical data warehouse, claims feeds, and HIE for real member data across the reporting period
- **StratifyRiskWorker** → run real risk models (Johns Hopkins ACG, Milliman, 3M CRGs) against aggregated member profiles
- **IdentifyGapsWorker** → evaluate HEDIS, STARS, or custom quality measures against clinical data to find real care gaps
- **InterveneWorker** → trigger real outreach via your IVR system, SMS campaigns, care coordinator task queues, or patient portal messages
- Add a **SDOHScreenWorker** after data aggregation to identify social determinants of health barriers (food insecurity, transportation, housing)
- Add a **ReportWorker** at the end to generate CMS STARS or MIPS quality measure submission files

Replace demo data sources with real warehouse queries and care management platforms while keeping the same output fields, and the population health pipeline needs no workflow changes.

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
population-health/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/populationhealth/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── PopulationHealthExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AggregateDataWorker.java
│       ├── IdentifyGapsWorker.java
│       ├── InterveneWorker.java
│       └── StratifyRiskWorker.java
└── src/test/java/populationhealth/workers/
    ├── AggregateDataWorkerTest.java        # 2 tests
    ├── IdentifyGapsWorkerTest.java        # 2 tests
    ├── InterveneWorkerTest.java        # 2 tests
    └── StratifyRiskWorkerTest.java        # 2 tests

```
