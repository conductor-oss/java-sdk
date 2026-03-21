# Property Maintenance Request in Java with Conductor :  Submit, Classify, Assign, Complete, and Invoice

A Java Conductor workflow example for handling tenant maintenance requests end-to-end. submitting the request, classifying its category and priority, assigning a technician, tracking completion, and generating an invoice for labor and parts. Uses [Conductor](https://github.

## The Problem

You need to handle maintenance requests from tenants across your property portfolio. A tenant reports "kitchen faucet leaking". the request must be logged, classified by category (plumbing, electrical, HVAC, general) and priority (emergency, urgent, routine), assigned to an available technician with the right skills, tracked through completion with labor hours recorded, and invoiced for billing. Each step depends on the previous one: you can't assign a technician without knowing the category, and you can't invoice without knowing the labor hours.

Without orchestration, maintenance requests pile up in email inboxes. Property managers manually classify and assign them, lose track of which requests are pending, and forget to invoice completed work. Building this as a monolithic script means a failure in the assignment step silently prevents invoicing, and tenants never get updates on their request status.

## The Solution

**You just write the request intake, classification, technician assignment, completion tracking, and invoicing logic. Conductor handles dispatch retries, priority routing, and maintenance audit trails.**

Each maintenance step is a simple, independent worker. one logs the request, one classifies category and priority, one assigns the right technician, one records completion details, one generates the invoice. Conductor takes care of executing them in order, retrying if the technician scheduling system is temporarily down, and tracking every request from submission through invoicing so nothing falls through the cracks. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Request intake, priority assessment, vendor dispatch, and completion verification workers each manage one step of resolving property maintenance issues.

| Worker | Task | What It Does |
|---|---|---|
| **SubmitWorker** | `mtr_submit` | Logs the maintenance request from the tenant with a unique request ID and description |
| **ClassifyWorker** | `mtr_classify` | Determines the request category (plumbing, electrical, HVAC) and priority (emergency, urgent, routine) |
| **AssignWorker** | `mtr_assign` | Selects and assigns an available technician with the right skills based on category and priority |
| **CompleteWorker** | `mtr_complete` | Records work completion. labor hours, parts used, and resolution notes from the technician |
| **InvoiceWorker** | `mtr_invoice` | Generates an invoice for labor and materials, calculating total cost from completion data |

Workers implement property transaction steps. listing, inspection, escrow, closing,  with realistic outputs. Replace with real MLS and escrow service integrations and the workflow stays the same.

### The Workflow

```
mtr_submit
    │
    ▼
mtr_classify
    │
    ▼
mtr_assign
    │
    ▼
mtr_complete
    │
    ▼
mtr_invoice

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
java -jar target/maintenance-request-1.0.0.jar

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
java -jar target/maintenance-request-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow mtr_maintenance_request \
  --version 1 \
  --input '{"tenantId": "TEST-001", "propertyId": "TEST-001", "description": "sample-description"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w mtr_maintenance_request -s COMPLETED -c 5

```

## How to Extend

Connect each worker to your real maintenance systems. your property management portal for ticket intake, your workforce scheduler for technician dispatch, your accounting system for invoicing, and the workflow runs identically in production.

- **SubmitWorker** (`mtr_submit`): accept requests via a tenant portal API or integrate with your property management platform (AppFolio, Buildium)
- **ClassifyWorker** (`mtr_classify`): use NLP or an LLM to auto-classify free-text descriptions ("water dripping from ceiling" maps to plumbing/emergency)
- **AssignWorker** (`mtr_assign`): query your technician database for availability and skills, integrate with a scheduling system like ServiceTitan or Jobber
- **CompleteWorker** (`mtr_complete`): receive completion data from the technician's mobile app, attach before/after photos from cloud storage
- **InvoiceWorker** (`mtr_invoice`): generate invoices in QuickBooks or Xero, apply landlord vs: tenant billing rules, and send via email

Swap vendor dispatch systems or priority algorithms and the maintenance pipeline adjusts transparently.

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
maintenance-request/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/maintenancerequest/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── MaintenanceRequestExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AssignWorker.java
│       ├── ClassifyWorker.java
│       ├── CompleteWorker.java
│       ├── InvoiceWorker.java
│       └── SubmitWorker.java
└── src/test/java/maintenancerequest/workers/
    ├── AssignWorkerTest.java        # 2 tests
    ├── ClassifyWorkerTest.java        # 2 tests
    ├── CompleteWorkerTest.java        # 2 tests
    ├── InvoiceWorkerTest.java        # 2 tests
    └── SubmitWorkerTest.java        # 2 tests

```
