# Document Review in Java with Conductor

A Java Conductor workflow example demonstrating Document Review. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## The Problem

A litigation matter requires reviewing 1,500 documents for production. You need to ingest the document batch, classify documents by relevance (420 relevant out of 1,500), have attorneys review for responsiveness (310 responsive), apply privilege designations to protected communications (25 privileged), and produce the final non-privileged responsive set (285 documents) to opposing counsel. Manual handling at this scale is error-prone and risks inadvertent privilege waiver.

Without orchestration, you'd wire all of this together in a single monolithic class .  managing execution order manually, writing try/catch blocks around every step, building retry loops with backoff, and adding logging to understand what happened when things go wrong. That code becomes brittle, hard to test, and impossible to observe at scale.

## The Solution

**You just write the document ingestion, classification, review assignment, and approval logic. Conductor handles classification retries, privilege routing, and document review audit trails.**

Each worker handles one legal operation. Conductor manages the review pipeline, approval chains, deadline enforcement, and audit trail.

### What You Write: Workers

Document ingestion, relevance classification, privilege screening, and annotation workers each handle one stage of the legal review pipeline.

| Worker | Task | What It Does |
|---|---|---|
| **IngestWorker** | `drv_ingest` | Loads 1,500 documents into the review platform, extracting text and metadata for downstream classification |
| **ClassifyWorker** | `drv_classify` | Classifies ingested documents by relevance, identifying 420 relevant documents and flagging 25 potentially privileged items |
| **ReviewWorker** | `drv_review` | Runs attorney review on classified documents, determining 310 as responsive and confirming the 25 privileged documents |
| **PrivilegeWorker** | `drv_privilege` | Applies privilege designations and withholds protected documents, calculating 285 producible (non-privileged responsive) documents |
| **ProduceWorker** | `drv_produce` | Packages the final 285 producible documents into the agreed-upon production format for delivery to opposing counsel |

Workers simulate legal operations .  document review, compliance checks, approval routing ,  with realistic outputs. Replace with real document management and e-signature integrations and the workflow stays the same.

### The Workflow

```
drv_ingest
    │
    ▼
drv_classify
    │
    ▼
drv_review
    │
    ▼
drv_privilege
    │
    ▼
drv_produce

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
java -jar target/document-review-1.0.0.jar

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
java -jar target/document-review-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow drv_document_review \
  --version 1 \
  --input '{"matterId": "TEST-001", "documentBatch": "sample-documentBatch"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w drv_document_review -s COMPLETED -c 5

```

## How to Extend

Point each worker at your real review tools .  your document management system for intake, your AI classifier for relevance scoring, your review platform for attorney assignment, and the workflow runs identically in production.

- **IngestWorker** (`drv_ingest`): connect to a document management system like NetDocuments, iManage, or SharePoint to pull documents, or use IPRO for bulk load and text extraction
- **ClassifyWorker** (`drv_classify`): integrate with Relativity Analytics or Everlaw for technology-assisted review (TAR) to auto-classify documents by relevance and issue coding
- **ReviewWorker** (`drv_review`): use Relativity or Everlaw review workflows with continuous active learning to accelerate attorney review and responsiveness determinations
- **PrivilegeWorker** (`drv_privilege`): integrate with privilege detection models (Relativity Privilege Screen or Brainspace) to flag attorney-client communications and generate privilege logs
- **ProduceWorker** (`drv_produce`): use Relativity Production or IPRO to generate Bates-stamped, redacted productions in TIFF, PDF, or native format per the ESI protocol

Change your relevance classifier or privilege rules and the review pipeline structure stays the same.

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
document-review/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/documentreview/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── DocumentReviewExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ClassifyWorker.java
│       ├── IngestWorker.java
│       ├── PrivilegeWorker.java
│       ├── ProduceWorker.java
│       └── ReviewWorker.java
└── src/test/java/documentreview/workers/
    ├── ClassifyWorkerTest.java        # 2 tests
    ├── IngestWorkerTest.java        # 2 tests
    ├── PrivilegeWorkerTest.java        # 2 tests
    ├── ProduceWorkerTest.java        # 2 tests
    └── ReviewWorkerTest.java        # 2 tests

```
