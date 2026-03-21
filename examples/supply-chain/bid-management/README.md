# Bid Management in Java with Conductor :  RFP Creation, Vendor Distribution, Bid Collection, Evaluation, and Award

A Java Conductor workflow example for competitive bid management. creating bid packages for projects (e.g., a warehouse expansion with a $100K budget), distributing RFPs to qualified vendors, collecting submitted bids by deadline, evaluating proposals against cost, timeline, and capability criteria, and awarding the contract to the winning bidder. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.
## The Problem

You need to run a competitive bidding process across multiple vendors. The procurement team creates a bid package with project specs and budget, distributes it to a shortlist of vendors (Alpha Corp, Beta Ltd, Gamma Inc), collects their proposals by a deadline, evaluates each bid on cost, schedule, and qualifications, and awards the contract. If a vendor's submission fails to upload, you need to retry without losing other submissions. If the evaluation step crashes, you need to resume without re-soliciting bids.

Without orchestration, you'd manage this in email threads and spreadsheets. manually tracking which vendors received the RFP, chasing late submissions, and comparing proposals in a shared doc. There is no audit trail of when bids were received, evaluation criteria applied inconsistently across reviewers, and the award decision has no traceable link to the scored evaluations.

## The Solution

**You just write the bid lifecycle workers. RFP creation, vendor distribution, proposal collection, evaluation scoring, and contract award. Conductor handles sequencing, retries, and full audit trails for procurement compliance.**

Each phase of the bidding lifecycle is a simple, independent worker. a plain Java class that does one thing. Conductor sequences them so the RFP is fully created before distribution, bids are only collected after all vendors have been notified, evaluation only runs once all submissions are in, and the award references the evaluation scores. If the distribution worker fails for one vendor, Conductor retries without re-sending to vendors already notified. Every step is recorded with timestamps and outputs for procurement audit compliance.

### What You Write: Workers

Five workers divide the bid lifecycle: CreateWorker builds the RFP package, DistributeWorker sends it to vendors, CollectWorker gathers proposals, EvaluateWorker scores them, and AwardWorker issues the contract.

| Worker | Task | What It Does |
|---|---|---|
| **AwardWorker** | `bid_award` | Awards the contract to the winning bidder based on evaluation results. |
| **CollectWorker** | `bid_collect` | Collects submitted bid responses from vendors by the deadline. |
| **CreateWorker** | `bid_create` | Creates a bid package with project specifications and budget. |
| **DistributeWorker** | `bid_distribute` | Distributes the RFP to the shortlisted vendors. |
| **EvaluateWorker** | `bid_evaluate` | Scores each bid against cost, timeline, and capability criteria. |

Workers implement supply chain operations. inventory checks, shipment tracking, supplier coordination,  with realistic outputs. Replace with real ERP and logistics integrations and the workflow stays the same.

### The Workflow

```
bid_create
    │
    ▼
bid_distribute
    │
    ▼
bid_collect
    │
    ▼
bid_evaluate
    │
    ▼
bid_award

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
java -jar target/bid-management-1.0.0.jar

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
java -jar target/bid-management-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow bid_management \
  --version 1 \
  --input '{"projectName": "test", "budget": "sample-budget", "vendors": "sample-vendors"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w bid_management -s COMPLETED -c 5

```

## How to Extend

Each worker wraps one procurement step. Connect CreateWorker to SAP Ariba for bid packages, DistributeWorker to your supplier portal for RFP delivery, and EvaluateWorker to your scoring model. The workflow definition stays exactly the same.

- **CreateWorker** (`bid_create`): generate the bid package in your procurement system (SAP Ariba, Coupa, or Jaggaer) with project specs, budget ceiling, and evaluation criteria
- **DistributeWorker** (`bid_distribute`): send RFPs to qualified vendors via your supplier portal or email API, recording delivery timestamps for each recipient
- **CollectWorker** (`bid_collect`): poll the supplier portal for submitted proposals, validate document completeness, and lock submissions after the deadline
- **EvaluateWorker** (`bid_evaluate`): score each bid against weighted criteria (cost 40%, timeline 30%, capability 30%) using your evaluation matrix or ML-based scoring model
- **AwardWorker** (`bid_award`): issue the award notification to the winning vendor, send regret notices to others, and create the purchase order in your ERP system

Each worker preserves its output contract, so swapping in real procurement APIs requires zero workflow changes.

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
bid-management/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/bidmanagement/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── BidManagementExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AwardWorker.java
│       ├── CollectWorker.java
│       ├── CreateWorker.java
│       ├── DistributeWorker.java
│       └── EvaluateWorker.java
└── src/test/java/bidmanagement/workers/
    ├── AwardWorkerTest.java        # 2 tests
    ├── CollectWorkerTest.java        # 2 tests
    ├── CreateWorkerTest.java        # 2 tests
    ├── DistributeWorkerTest.java        # 2 tests
    └── EvaluateWorkerTest.java        # 2 tests

```
