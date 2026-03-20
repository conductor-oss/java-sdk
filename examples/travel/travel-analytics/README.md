# Travel Analytics in Java with Conductor

Travel analytics: collect, aggregate, analyze, report. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers .  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## The Problem

You need to generate a travel analytics report for a department and time period. Collecting all booking, expense, and reimbursement data, aggregating spending across categories (flights, hotels, car rentals, meals), analyzing trends to identify cost-saving opportunities (preferred vendor compliance, advance booking rates, policy exception frequency), and producing a report for management. Each transformation depends on the previous one's output.

If aggregation miscounts hotel expenses as meals, the category breakdown is wrong and management makes decisions based on incorrect data. If the analysis step finds cost-saving opportunities but the report generation fails, those insights never reach the people who can act on them. Without orchestration, you'd build a batch analytics script that mixes data collection queries, aggregation logic, trend analysis, and report formatting. Making it impossible to add new data sources, test analysis algorithms independently, or schedule reports on different cadences for different departments.

## The Solution

**You just write the data collection, spending aggregation, trend analysis, and report generation logic. Conductor handles data aggregation retries, trend analysis, and travel spend audit trails.**

CollectWorker gathers travel data for the specified department and period. Bookings, expenses, reimbursements, and policy exceptions. AggregateWorker groups the data by category (airfare, lodging, ground transport, meals) and computes totals, averages, and per-trip costs. AnalyzeWorker identifies cost-saving opportunities by comparing actual spending against negotiated rates, measuring advance booking compliance, and flagging departments with high exception rates. ReportWorker generates the analytics dashboard with charts, trend lines, and actionable recommendations for management. Each worker is a standalone Java class. Conductor handles the sequencing, retries, and crash recovery.

### What You Write: Workers

Data aggregation, spend analysis, trend identification, and report generation workers each process one dimension of corporate travel intelligence.

| Worker | Task | What It Does |
|---|---|---|
| **AggregateWorker** | `tan_aggregate` | Aggregated bookings across categories |
| **AnalyzeWorker** | `tan_analyze` | Identified 3 cost-saving opportunities |
| **CollectWorker** | `tan_collect` | Collects and validates and computes raw data |
| **ReportWorker** | `tan_report` | Analytics dashboard updated with new insights |

Workers simulate travel operations .  booking, approval, itinerary generation ,  with realistic outputs. Replace with real GDS and travel API integrations and the workflow stays the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
tan_collect
    │
    ▼
tan_aggregate
    │
    ▼
tan_analyze
    │
    ▼
tan_report
```

## Example Output

```
=== Example 550: Travel Analytics ===

Step 1: Registering task definitions...
  Registered: tan_collect, tan_aggregate, tan_analyze, tan_report

Step 2: Registering workflow 'tan_travel_analytics'...
  Workflow registered.

Step 3: Starting workers...
  4 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [aggregate] Aggregated bookings across categories
  [analyze] Identified 3 cost-saving opportunities
  [collect] Gathering travel data for
  [report] Analytics dashboard updated with new insights

  Status: COMPLETED
  Output: {aggregated=..., insights=..., rawData=..., reportId=...}

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
java -jar target/travel-analytics-1.0.0.jar
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
java -jar target/travel-analytics-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow tan_travel_analytics \
  --version 1 \
  --input '{"period": "sample-period", "2024-Q1": "sample-2024-Q1", "department": "sample-department"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w tan_travel_analytics -s COMPLETED -c 5
```

## How to Extend

Connect each worker to your real data sources .  your booking system for travel records, your data warehouse for aggregation, a BI tool like Tableau for report generation, and the workflow runs identically in production.

- **CollectWorker** (`tan_collect`): pull travel data from your TMS (SAP Concur, Navan), expense system (Expensify, Brex), and corporate card transaction feeds for the specified period
- **AggregateWorker** (`tan_aggregate`): run aggregation queries in your data warehouse (Snowflake, BigQuery, Redshift) to compute category totals, per-trip averages, and department-level spending breakdowns
- **AnalyzeWorker** (`tan_analyze`): compare actual spending against negotiated vendor rates, compute policy compliance percentages, and identify top cost-saving opportunities using your analytics engine
- **ReportWorker** (`tan_report`): generate the analytics dashboard in your BI platform (Tableau, Power BI, Looker) or produce a PDF report with charts and distribute it to department heads via email

Change data sources or reporting tools and the analytics pipeline processes them without restructuring.

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
travel-analytics-travel-analytics/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/travelanalytics/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── TravelAnalyticsExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AggregateWorker.java
│       ├── AnalyzeWorker.java
│       ├── CollectWorker.java
│       └── ReportWorker.java
└── src/test/java/travelanalytics/workers/
    ├── AggregateWorkerTest.java        # 2 tests
    ├── AnalyzeWorkerTest.java        # 2 tests
    ├── CollectWorkerTest.java        # 2 tests
    └── ReportWorkerTest.java        # 2 tests
```
