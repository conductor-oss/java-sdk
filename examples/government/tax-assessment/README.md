# Property Tax Assessment in Java with Conductor :  Valuation, Calculation, and Owner Notification

A Java Conductor workflow example for municipal property tax assessment. collecting property data, appraising value, computing tax liability, notifying property owners, and opening an appeal window. Uses [Conductor](https://github.

## The Problem

You need to assess property taxes for a municipality. Each assessment involves pulling property records (square footage, lot size, bedroom count), appraising the property's market value, applying the local mill rate to compute the tax bill, mailing the owner a notice, and opening a statutory appeal window. These steps must run in strict sequence. you cannot calculate tax without a valuation, and you cannot notify an owner without a final amount.

Without orchestration, you'd wire all of this into a single monolithic class. querying the property database, calling the appraisal service, computing the tax, sending the notification, and recording the appeal deadline. If the appraisal service times out, you'd need retry logic. If the notification fails after the tax is already calculated, you'd need to track partial progress. Every step needs error handling, and auditors need a complete record of every assessment for compliance.

## The Solution

**You just write the property data collection, valuation, tax calculation, owner notification, and appeal processing logic. Conductor handles valuation retries, notice generation, and assessment audit trails.**

Each stage of the tax assessment is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of executing them in the right order, retrying on failure, tracking every assessment from data collection through appeal window, and resuming if the process crashes mid-assessment. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Property data collection, valuation, assessment calculation, and notice generation workers each handle one phase of the tax assessment cycle.

| Worker | Task | What It Does |
|---|---|---|
| **CollectDataWorker** | `txa_collect_data` | Pulls property records (sqft, bedrooms, lot size) for a given property ID and tax year |
| **AssessPropertyWorker** | `txa_assess_property` | Appraises fair market value and determines the applicable tax rate based on property data |
| **CalculateWorker** | `txa_calculate` | Multiplies the assessed value by the mill rate to produce the final tax bill |
| **NotifyWorker** | `txa_notify` | Sends the tax bill notice to the property owner |
| **AppealWorker** | `txa_appeal` | Opens the statutory appeal window and records the deadline for the property owner to contest |

Workers implement government operations. application processing, compliance checks, notifications,  with realistic outputs. Replace with real agency system integrations and the workflow stays the same.

### The Workflow

```
txa_collect_data
    │
    ▼
txa_assess_property
    │
    ▼
txa_calculate
    │
    ▼
txa_notify
    │
    ▼
txa_appeal

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
java -jar target/tax-assessment-1.0.0.jar

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
java -jar target/tax-assessment-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow txa_tax_assessment \
  --version 1 \
  --input '{"propertyId": "TEST-001", "taxYear": "sample-taxYear", "ownerId": "TEST-001"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w txa_tax_assessment -s COMPLETED -c 5

```

## How to Extend

Wire each worker to your real assessment systems. your property database for parcel data, your appraisal engine for valuation, your tax billing platform for owner notifications, and the workflow runs identically in production.

- **CollectDataWorker** → query your county assessor database or GIS system to pull real parcel records (lot dimensions, improvements, zoning)
- **AssessPropertyWorker** → call an appraisal model or MLS comps API to generate fair market valuations based on comparable sales
- **NotifyWorker** → send real tax bill notices via USPS Informed Delivery, SendGrid, or your municipality's notification system
- **AppealWorker** → create appeal cases in your case management system with statutory deadlines and hearing scheduling
- Add a **ExemptionCheckWorker** to apply homestead, senior, or veteran exemptions before calculating the final bill

Change valuation models or notice formats and the assessment pipeline adjusts without modification.

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
tax-assessment-tax-assessment/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/taxassessment/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── TaxAssessmentExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AppealWorker.java
│       ├── AssessPropertyWorker.java
│       ├── CalculateWorker.java
│       ├── CollectDataWorker.java
│       └── NotifyWorker.java
└── src/test/java/taxassessment/workers/
    ├── AppealWorkerTest.java
    ├── CalculateWorkerTest.java
    └── CollectDataWorkerTest.java

```
