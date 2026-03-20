# Donor Management in Java with Conductor

A Java Conductor workflow example demonstrating Donor Management. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers .## The Problem

A new donor makes their first gift to your nonprofit. The development team needs to acquire the donor by recording their contact information, begin stewardship with engagement touchpoints, send a tax-deductible donation acknowledgment letter, create a retention plan based on their giving level, and evaluate whether they qualify for upgrade to major-donor status. Each step depends on the previous one's output.

Without orchestration, you'd wire all of this together in a single monolithic class .  managing execution order manually, writing try/catch blocks around every step, building retry loops with backoff, and adding logging to understand what happened when things go wrong. That code becomes brittle, hard to test, and impossible to observe at scale.

## The Solution

**You just write the donor intake, gift recording, acknowledgment, and stewardship logic. Conductor handles gift processing retries, acknowledgment sequencing, and donor engagement audit trails.**

Each worker handles one nonprofit operation. Conductor manages the donation pipeline, campaign sequencing, receipt generation, and reporting.

### What You Write: Workers

Donor intake, gift processing, acknowledgment, and stewardship workers each manage one phase of the donor relationship lifecycle.

| Worker | Task | What It Does |
|---|---|---|
| **AcknowledgeWorker** | `dnr_acknowledge` | Sends a personalized donation acknowledgment letter to the donor for tax purposes |
| **AcquireWorker** | `dnr_acquire` | Records the new donor's name and email, assigning a unique donor ID |
| **RetainWorker** | `dnr_retain` | Creates a retention plan for the donor based on their giving level, with a recommended next action (e.g., annual report) |
| **StewardWorker** | `dnr_steward` | Manages donor engagement touchpoints, tracking contact history and sentiment for the donor's first gift |
| **UpgradeWorker** | `dnr_upgrade` | Evaluates whether the donor qualifies for upgrade from their current level to major-donor status |

Workers simulate nonprofit operations .  donor processing, campaign management, reporting ,  with realistic outputs. Replace with real CRM and payment integrations and the workflow stays the same.

### The Workflow

```
dnr_acquire
    │
    ▼
dnr_steward
    │
    ▼
dnr_acknowledge
    │
    ▼
dnr_retain
    │
    ▼
dnr_upgrade
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
java -jar target/donor-management-1.0.0.jar
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
java -jar target/donor-management-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow donor_management_755 \
  --version 1 \
  --input '{"donorName": "test", "donorEmail": "user@example.com", "firstGift": "test-value"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w donor_management_755 -s COMPLETED -c 5
```

## How to Extend

Point each worker at your real donor systems. Salesforce NPSP for donor records, Stripe for payment processing, Mailchimp for acknowledgment emails, and the workflow runs identically in production.

- **AcquireWorker** (`dnr_acquire`): create the donor contact in Salesforce NPSP, DonorPerfect, or Bloomerang via their API, returning the CRM donor ID
- **StewardWorker** (`dnr_steward`): log stewardship touchpoints in your CRM and schedule follow-up tasks using Salesforce Tasks or DonorPerfect activities
- **AcknowledgeWorker** (`dnr_acknowledge`): generate a tax-deductible acknowledgment letter using a template engine and send it via SendGrid or your CRM's email integration
- **RetainWorker** (`dnr_retain`): query giving history from your CRM to classify the donor tier and create a retention workflow in Bloomerang or DonorPerfect
- **UpgradeWorker** (`dnr_upgrade`): evaluate giving patterns against your major-donor criteria in the CRM and update the donor's level and cultivation stage

Change your CRM or acknowledgment templates and the donor pipeline keeps its structure.

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
donor-management/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/donormanagement/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── DonorManagementExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AcknowledgeWorker.java
│       ├── AcquireWorker.java
│       ├── RetainWorker.java
│       ├── StewardWorker.java
│       └── UpgradeWorker.java
└── src/test/java/donormanagement/workers/
    ├── AcquireWorkerTest.java        # 1 tests
    └── UpgradeWorkerTest.java        # 1 tests
```
