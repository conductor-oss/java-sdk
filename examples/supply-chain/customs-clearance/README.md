# Customs Clearance in Java with Conductor :  Declaration, Document Validation, Duty Calculation, Clearance, and Cargo Release

A Java Conductor workflow example for international customs clearance. filing customs declarations for imported goods (e.g., electronic components from Shanghai to Los Angeles), validating HS codes and commercial documents, calculating import duties and tariffs based on goods value and classification, obtaining customs clearance approval, and releasing cargo for domestic delivery. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.
## The Problem

You need to clear imported shipments through customs. A container of electronic components (HS code 8542, $45K value) and packaging materials (HS code 4819, $5K value) arriving from Shanghai must be declared to US Customs, documents validated (commercial invoice, packing list, bill of lading), duties calculated based on tariff schedules and trade agreements, clearance obtained from CBP, and cargo released from the port. Each step depends on the previous one. you cannot calculate duty without validated HS codes, and cargo cannot be released without clearance.

Without orchestration, customs brokers manage this process through email chains with freight forwarders and government portals. If the duty calculation fails because of an invalid HS code, the shipment sits at the port accruing demurrage charges ($500+/day) while someone manually fixes the declaration and resubmits. There is no unified timeline of when each step completed, making it impossible to identify bottlenecks or prove compliance to auditors.

## The Solution

**You just write the customs workers. Declaration filing, document validation, duty calculation, clearance submission, and cargo release. Conductor handles strict step ordering, automatic retries on API timeouts, and timestamped records for trade compliance audits.**

Each step of the customs clearance process is a simple, independent worker. a plain Java class that does one thing. Conductor sequences them so declarations are filed before validation, validated documents feed duty calculation, duties must be confirmed before clearance is requested, and cargo is only released after clearance is granted. If the customs authority's API times out during the clearance request, Conductor retries automatically without re-filing the declaration. Every filing, validation result, duty amount, and clearance decision is recorded with timestamps for trade compliance audits.

### What You Write: Workers

Five workers move shipments through customs: DeclareWorker files the declaration, ValidateWorker checks HS codes and documents, CalculateDutyWorker computes tariffs, ClearWorker submits for clearance, and ReleaseWorker frees cargo for delivery.

| Worker | Task | What It Does |
|---|---|---|
| **CalculateDutyWorker** | `cst_calculate_duty` | Calculates import duties and tariffs based on HS codes, goods value, and trade agreements. |
| **ClearWorker** | `cst_clear` | Submits the clearance request to customs authorities with declaration and duty payment. |
| **DeclareWorker** | `cst_declare` | Files the customs declaration with shipment details, origin, and goods classification. |
| **ReleaseWorker** | `cst_release` | Releases cargo from the port for domestic delivery after clearance is granted. |
| **ValidateWorker** | `cst_validate` | Validates HS codes and commercial documents (invoice, packing list, bill of lading). |

Workers implement supply chain operations. inventory checks, shipment tracking, supplier coordination,  with realistic outputs. Replace with real ERP and logistics integrations and the workflow stays the same.

### The Workflow

```
cst_declare
    │
    ▼
cst_validate
    │
    ▼
cst_calculate_duty
    │
    ▼
cst_clear
    │
    ▼
cst_release

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
java -jar target/customs-clearance-1.0.0.jar

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
java -jar target/customs-clearance-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow cst_customs_clearance \
  --version 1 \
  --input '{"shipmentId": "TEST-001", "origin": "sample-origin", "destination": "production", "goods": "sample-goods"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w cst_customs_clearance -s COMPLETED -c 5

```

## How to Extend

Connect DeclareWorker to your customs broker portal, ValidateWorker to your trade compliance system, and CalculateDutyWorker to your tariff schedule database. The workflow definition stays exactly the same.

- **DeclareWorker** (`cst_declare`): file customs declarations via CBP ACE (Automated Commercial Environment) API, EU ICS2, or your customs broker's EDI gateway
- **ValidateWorker** (`cst_validate`): verify HS codes against the Harmonized Tariff Schedule, validate commercial invoice amounts against the purchase order, and check for restricted/sanctioned goods
- **CalculateDutyWorker** (`cst_calculate_duty`): compute import duties using tariff databases (USITC, WCO), applying preferential rates from trade agreements (USMCA, EU FTAs) where eligible
- **ClearWorker** (`cst_clear`): submit the entry for CBP review, handle holds or examinations, and poll for clearance status from the government portal
- **ReleaseWorker** (`cst_release`): notify the freight forwarder and warehouse that cargo is cleared for pickup, update the TMS with release confirmation

Replace any simulation with a real customs API while keeping the same return fields, and the pipeline continues to work as designed.

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
customs-clearance/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/customsclearance/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── CustomsClearanceExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CalculateDutyWorker.java
│       ├── ClearWorker.java
│       ├── DeclareWorker.java
│       ├── ReleaseWorker.java
│       └── ValidateWorker.java
└── src/test/java/customsclearance/workers/
    ├── CalculateDutyWorkerTest.java        # 2 tests
    ├── ClearWorkerTest.java        # 2 tests
    ├── DeclareWorkerTest.java        # 2 tests
    ├── ReleaseWorkerTest.java        # 2 tests
    └── ValidateWorkerTest.java        # 2 tests

```
