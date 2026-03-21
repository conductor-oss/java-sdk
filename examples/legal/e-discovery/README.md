# E Discovery in Java with Conductor

A Java Conductor workflow example demonstrating E Discovery. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## The Problem

Litigation is underway and opposing counsel has served discovery requests. You need to identify relevant data sources (email, Slack), collect 45,000+ items totaling 120 GB, de-duplicate and process them down to 28,000 unique documents, have attorneys review for responsiveness and privilege (8,500 responsive, 320 privileged), and produce the final set to opposing counsel. Each stage depends on the prior one, and a missed step can lead to spoliation sanctions or waiver of privilege.

Without orchestration, you'd wire all of this together in a single monolithic class. managing execution order manually, writing try/catch blocks around every step, building retry loops with backoff, and adding logging to understand what happened when things go wrong. That code becomes brittle, hard to test, and impossible to observe at scale.

## The Solution

**You just write the data collection, document processing, relevance classification, and production set generation logic. Conductor handles collection retries, review sequencing, and defensible discovery audit trails.**

Each worker handles one legal operation. Conductor manages the review pipeline, approval chains, deadline enforcement, and audit trail.

### What You Write: Workers

Data collection, processing, review, and production workers handle electronic discovery through distinct, defensible stages.

| Worker | Task | What It Does |
|---|---|---|
| **IdentifyWorker** | `edc_identify` | Maps relevant data sources (email, Slack) and outputs initial collection metadata. 45,000 total items across 120 GB, with estimated 28,000 unique and 8,500 responsive documents |
| **CollectWorker** | `edc_collect` | Collects electronically stored information from identified sources, gathering 45,000 items (120 GB) and de-duplicating to 28,000 unique documents |
| **ProcessWorker** | `edc_process` | De-duplicates, indexes, and normalizes collected data, reducing to 28,000 unique processable documents for review |
| **ReviewWorker** | `edc_review` | Runs attorney review on processed documents, classifying 8,500 as responsive and flagging 320 as privileged |
| **ProduceWorker** | `edc_produce` | Packages the final production set (8,180 non-privileged responsive documents) in the agreed-upon format for delivery to opposing counsel |

Workers implement legal operations. document review, compliance checks, approval routing,  with realistic outputs. Replace with real document management and e-signature integrations and the workflow stays the same.

### The Workflow

```
edc_identify
    │
    ▼
edc_collect
    │
    ▼
edc_process
    │
    ▼
edc_review
    │
    ▼
edc_produce

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
java -jar target/e-discovery-1.0.0.jar

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
java -jar target/e-discovery-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow edc_e_discovery \
  --version 1 \
  --input '{"matterId": "TEST-001", "custodians": "sample-custodians", "dateRange": "2026-01-01T00:00:00Z"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w edc_e_discovery -s COMPLETED -c 5

```

## How to Extend

Swap each worker for your real e-discovery stack. Relativity for document processing, your AI platform for relevance classification, your review tool for attorney coding, and the workflow runs identically in production.

- **IdentifyWorker** (`edc_identify`): integrate with Microsoft Purview, Google Vault, or Slack eDiscovery APIs to programmatically map custodian data sources and estimate collection volumes
- **CollectWorker** (`edc_collect`): use Logikcull, Relativity Collect, or Nuix to ingest data from custodian accounts, preserving metadata and chain of custody
- **ProcessWorker** (`edc_process`): connect to a processing engine like Relativity Processing, IPRO, or Nuix Workstation for de-duplication, near-duplicate detection, and text extraction
- **ReviewWorker** (`edc_review`): integrate with Relativity or Everlaw for technology-assisted review (TAR), continuous active learning, and privilege detection
- **ProduceWorker** (`edc_produce`): use Relativity Production or CloudNine to generate Bates-stamped productions in agreed-upon formats (TIFF, native, or PDF)

Switch processing tools or review platforms and the discovery pipeline handles them transparently.

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
e-discovery/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/ediscovery/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── EDiscoveryExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CollectWorker.java
│       ├── IdentifyWorker.java
│       ├── ProcessWorker.java
│       ├── ProduceWorker.java
│       └── ReviewWorker.java
└── src/test/java/ediscovery/workers/
    ├── CollectWorkerTest.java        # 2 tests
    ├── IdentifyWorkerTest.java        # 2 tests
    ├── ProcessWorkerTest.java        # 2 tests
    ├── ProduceWorkerTest.java        # 2 tests
    └── ReviewWorkerTest.java        # 2 tests

```
