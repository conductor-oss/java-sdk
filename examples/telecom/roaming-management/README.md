# Roaming Management in Java Using Conductor

A Java Conductor workflow example that orchestrates telecom roaming management. detecting when a subscriber connects to a visited network, validating the inter-carrier roaming agreement between the home and visited networks, rating the roaming usage according to the agreement's tariff schedule, billing the subscriber for roaming charges, and settling the inter-carrier payment between the home and visited operators. Uses [Conductor](https://github.

## Why Roaming Management Needs Orchestration

Managing roaming events requires a pipeline that spans two independent carrier networks. You detect roaming when a subscriber registers on a visited network. their device attaches to a foreign PLMN and the visited network sends a location update to the home network. You validate that a roaming agreement exists between the home and visited networks,  checking that the agreement is active, covers the subscriber's service types, and has not exceeded volume caps. You rate the roaming usage by applying the tariffs defined in the inter-carrier agreement,  which differ from the subscriber's domestic plan. You bill the subscriber by adding roaming charges to their account. Finally, you settle the inter-carrier amount between the home and visited operators.

If billing succeeds but settlement fails, the home operator has charged the subscriber but hasn't paid the visited operator. creating a financial discrepancy that compounds across millions of roaming events. If agreement validation discovers no active agreement, the subscriber should be barred from the visited network before usage accumulates unbillable charges. Without orchestration, you'd build a batch process that collects TAP files weekly, manually reconciles rates, and generates settlement invoices in spreadsheets,  making it impossible to handle near-real-time roaming events, detect agreement violations before they accumulate, or audit which tariff was applied to which roaming session.

## The Solution

**You just write the roaming detection, agreement validation, usage rating, subscriber billing, and inter-carrier settlement logic. Conductor handles session validation retries, charge calculations, and roaming settlement audit trails.**

Each worker handles one telecom operation. Conductor manages the provisioning pipeline, activation sequencing, billing triggers, and service state tracking.

### What You Write: Workers

Partner agreement lookup, session validation, charge calculation, and settlement workers each handle one aspect of cross-network roaming.

| Worker | Task | What It Does |
|---|---|---|
| **BillWorker** | `rmg_bill` | Bills the subscriber by adding roaming charges to their account based on rated usage. |
| **DetectRoamingWorker** | `rmg_detect_roaming` | Detects a roaming event when a subscriber connects to a visited network and captures the usage data. |
| **RateWorker** | `rmg_rate` | Rates roaming usage by applying the inter-carrier agreement's tariff schedule to the captured usage. |
| **SettleWorker** | `rmg_settle` | Settles the inter-carrier payment between the home and visited operators for the roaming usage. |
| **ValidateAgreementWorker** | `rmg_validate_agreement` | Validates that an active roaming agreement exists between the home and visited networks. |

Workers implement telecom operations. provisioning, activation, billing,  with realistic outputs. Replace with real OSS/BSS integrations and the workflow stays the same.

### The Workflow

```
rmg_detect_roaming
    │
    ▼
rmg_validate_agreement
    │
    ▼
rmg_rate
    │
    ▼
rmg_bill
    │
    ▼
rmg_settle

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
java -jar target/roaming-management-1.0.0.jar

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
java -jar target/roaming-management-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow rmg_roaming_management \
  --version 1 \
  --input '{"subscriberId": "TEST-001", "homeNetwork": "sample-homeNetwork", "visitedNetwork": "sample-visitedNetwork"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w rmg_roaming_management -s COMPLETED -c 5

```

## How to Extend

Swap each worker for your real roaming infrastructure. your HLR for roaming detection, your IREG platform for agreement validation, your TAP processing system for inter-carrier settlement, and the workflow runs identically in production.

- **DetectRoamingWorker** (`rmg_detect_roaming`): consume roaming events from your NRTRDE (Near Real Time Roaming Data Exchange) feed or parse TAP (Transferred Account Procedure) files from the visited network's data clearing house (BICS, Syniverse)
- **ValidateAgreementWorker** (`rmg_validate_agreement`): query your roaming agreement database or partner management platform (Mobileum, TOMIA) to confirm the agreement is active and covers the service types used
- **RateWorker** (`rmg_rate`): apply IOT (Inter-Operator Tariff) rates from the bilateral agreement using your roaming rating engine, calculating both subscriber charges and the inter-carrier settlement amount
- **BillWorker** (`rmg_bill`): post roaming charges to the subscriber's account in your billing system (Amdocs, CSG, Oracle BRM) as a separate line item with visited-network detail
- **SettleWorker** (`rmg_settle`): generate the inter-carrier settlement record via your clearing house (Syniverse DCH, BICS) or submit directly to the visited operator's settlement platform per the GSMA AA.13/AA.14 standards

Update partner agreements or settlement rules and the roaming pipeline handles them with no code changes.

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
roaming-management-roaming-management/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/roamingmanagement/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── RoamingManagementExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── BillWorker.java
│       ├── DetectRoamingWorker.java
│       ├── RateWorker.java
│       ├── SettleWorker.java
│       └── ValidateAgreementWorker.java
└── src/test/java/roamingmanagement/workers/
    ├── DetectRoamingWorkerTest.java        # 1 tests
    └── SettleWorkerTest.java        # 1 tests

```
