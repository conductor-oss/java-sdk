# Legal Billing in Java with Conductor

A Java Conductor workflow example demonstrating Legal Billing. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## The Problem

The billing period has closed. You need to collect time entries from attorneys across matters (e.g., 4.5 hours of contract review by J. Smith, 2.0 hours of research by A. Jones), review them for billing guideline compliance, generate an invoice with the correct total ($3,250.00), send it to the client, and track payment until collected. Manual billing processes lead to write-offs from missed time entries, rejected invoices from guideline violations, and delayed collections.

Without orchestration, you'd wire all of this together in a single monolithic class. managing execution order manually, writing try/catch blocks around every step, building retry loops with backoff, and adding logging to understand what happened when things go wrong. That code becomes brittle, hard to test, and impossible to observe at scale.

## The Solution

**You just write the time entry collection, rate application, invoice generation, and payment processing logic. Conductor handles time entry retries, invoice generation, and billing audit trails.**

Each worker handles one legal operation. Conductor manages the review pipeline, approval chains, deadline enforcement, and audit trail.

### What You Write: Workers

Time entry collection, rate application, invoice generation, and payment tracking workers each manage one phase of legal fee accounting.

| Worker | Task | What It Does |
|---|---|---|
| **TrackTimeWorker** | `lgb_track_time` | Collects time entries for the matter from all attorneys (e.g., J. Smith: 4.5 hrs contract review, A. Jones: 2.0 hrs research), totaling 6.5 hours |
| **ReviewWorker** | `lgb_review` | Reviews time entries against billing guidelines, approving 6.5 hours with zero adjustments and flagging any non-compliant entries |
| **GenerateWorker** | `lgb_generate` | Generates an invoice (INV-{timestamp}) for the client with a total amount of $3,250.00 USD based on approved time entries |
| **SendWorker** | `lgb_send` | Delivers the invoice to the client via their preferred channel and records the sent timestamp |
| **CollectWorker** | `lgb_collect` | Tracks payment status for the invoice, recording the payment date and marking the invoice as "paid" upon receipt |

Workers implement legal operations. document review, compliance checks, approval routing,  with realistic outputs. Replace with real document management and e-signature integrations and the workflow stays the same.

### The Workflow

```
lgb_track_time
    │
    ▼
lgb_review
    │
    ▼
lgb_generate
    │
    ▼
lgb_send
    │
    ▼
lgb_collect

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
java -jar target/legal-billing-1.0.0.jar

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
java -jar target/legal-billing-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow lgb_legal_billing \
  --version 1 \
  --input '{"clientId": "TEST-001", "matterId": "TEST-001", "billingPeriod": "sample-billingPeriod"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w lgb_legal_billing -s COMPLETED -c 5

```

## How to Extend

Connect each worker to your real billing systems. your time tracking platform for hour collection, your rate engine for fee calculation, your accounting system for invoice delivery, and the workflow runs identically in production.

- **TrackTimeWorker** (`lgb_track_time`): integrate with a legal time tracking system like Clio, Aderant, or Elite 3E to pull attorney time entries for the billing period
- **ReviewWorker** (`lgb_review`): connect to LEDES/UTBMS billing guideline engines or outside counsel guideline (OCG) validation tools like Brightflag or CounselLink to auto-check compliance
- **GenerateWorker** (`lgb_generate`): use your billing system's API (Clio, Aderant, or Elite 3E) to generate LEDES-format invoices with proper task/activity codes
- **SendWorker** (`lgb_send`): deliver invoices via e-billing platforms like Legal Tracker, Tymetrix, or BrightFlag, or email them as PDF/LEDES attachments
- **CollectWorker** (`lgb_collect`): integrate with your accounting system (QuickBooks, NetSuite) or e-billing platform to track payment status, aging, and send automated collection reminders

Update rate cards or billing policies and the pipeline calculates fees without structural modifications.

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
legal-billing/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/legalbilling/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── LegalBillingExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CollectWorker.java
│       ├── GenerateWorker.java
│       ├── ReviewWorker.java
│       ├── SendWorker.java
│       └── TrackTimeWorker.java
└── src/test/java/legalbilling/workers/
    ├── CollectWorkerTest.java        # 2 tests
    ├── GenerateWorkerTest.java        # 2 tests
    ├── ReviewWorkerTest.java        # 2 tests
    ├── SendWorkerTest.java        # 2 tests
    └── TrackTimeWorkerTest.java        # 2 tests

```
