# Property Valuation in Java with Conductor :  Comparable Sales, Market Analysis, Appraisal, and Report

A Java Conductor workflow example for automated property valuation .  collecting comparable sales data (comps), analyzing market trends, generating an appraisal estimate, and producing a formal valuation report. Uses [Conductor](https://github.

## The Problem

You need to determine the market value of a property. An accurate valuation requires gathering recent comparable sales in the same neighborhood (same size, age, features), analyzing how the comps compare to the subject property (adjusting for differences in square footage, lot size, upgrades), producing an appraised value based on the analysis, and generating a formal report that lenders and buyers can rely on. Each step depends on the previous one .  the analysis needs comps, the appraisal needs the analysis, and the report needs everything.

Without orchestration, property valuations are manual and inconsistent. Appraisers search for comps in multiple MLS systems, perform adjustments on paper or in spreadsheets, and write reports from scratch. When the MLS data source is slow, the entire valuation stalls. When an appraiser is unavailable, nobody knows which step the valuation reached. Lenders waiting on the appraisal have no visibility into progress.

## The Solution

**You just write the comparable sales collection, market analysis, appraisal estimation, and report generation logic. Conductor handles comparable search retries, adjustment calculations, and appraisal audit trails.**

Each valuation step is a simple, independent worker .  one collects comparable sales, one performs market analysis with adjustments, one produces the appraisal estimate, one generates the formal report. Conductor takes care of executing them in order, retrying if the MLS data feed times out, and tracking every valuation from comps collection through report delivery. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Data collection, comparable analysis, adjustment calculation, and appraisal report workers each contribute one layer to determining property value.

| Worker | Task | What It Does |
|---|---|---|
| **CollectCompsWorker** | `pvl_collect_comps` | Gathers recent comparable sales near the subject property .  similar size, age, and features |
| **AnalyzeWorker** | `pvl_analyze` | Performs market analysis on comps, adjusting for differences in square footage, lot size, and upgrades |
| **AppraiseWorker** | `pvl_appraise` | Produces the appraisal estimate based on the adjusted comp analysis |
| **ReportWorker** | `pvl_report` | Generates the formal valuation report with comps, adjustments, and final estimated value |

Workers simulate property transaction steps .  listing, inspection, escrow, closing ,  with realistic outputs. Replace with real MLS and escrow service integrations and the workflow stays the same.

### The Workflow

```
pvl_collect_comps
    │
    ▼
pvl_analyze
    │
    ▼
pvl_appraise
    │
    ▼
pvl_report

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
java -jar target/property-valuation-1.0.0.jar

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
java -jar target/property-valuation-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow pvl_property_valuation \
  --version 1 \
  --input '{"propertyId": "TEST-001", "address": "sample-address"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w pvl_property_valuation -s COMPLETED -c 5

```

## How to Extend

Connect each worker to your real valuation sources. MLS APIs for comparable sales, your market analytics platform for trend analysis, your appraisal report generator for formal documentation, and the workflow runs identically in production.

- **CollectCompsWorker** (`pvl_collect_comps`): query MLS data via RETS/RESO APIs, pull from Zillow/Redfin data feeds, or search county assessor records
- **AnalyzeWorker** (`pvl_analyze`): implement real adjustment calculations (price per square foot, age depreciation, location premiums) or use an AVM (automated valuation model)
- **AppraiseWorker** (`pvl_appraise`): apply USPAP-compliant appraisal methodology, or integrate with a third-party AVM like HouseCanary or CoreLogic
- **ReportWorker** (`pvl_report`): generate URAR (Uniform Residential Appraisal Report) PDFs, or post results to your lender's loan origination system

Change comparable data sources or adjustment models and the valuation pipeline continues operating identically.

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
property-valuation/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/propertyvaluation/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── PropertyValuationExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AnalyzeWorker.java
│       ├── AppraiseWorker.java
│       ├── CollectCompsWorker.java
│       └── ReportWorker.java
└── src/test/java/propertyvaluation/workers/
    ├── AnalyzeWorkerTest.java        # 2 tests
    ├── AppraiseWorkerTest.java        # 2 tests
    ├── CollectCompsWorkerTest.java        # 2 tests
    └── ReportWorkerTest.java        # 2 tests

```
