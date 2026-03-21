# Contract Analysis in Java with Conductor

A Java Conductor workflow example demonstrating Contract Analysis. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## The Problem

A new vendor contract lands on the legal team's desk. You need to parse the document, extract key clauses (termination terms, liability caps, non-compete provisions), assess risk levels across each clause, and produce a consolidated summary for the business team to review before signing. Doing this manually across dozens of contracts per quarter leads to missed risk clauses and inconsistent analysis.

Without orchestration, you'd wire all of this together in a single monolithic class. managing execution order manually, writing try/catch blocks around every step, building retry loops with backoff, and adding logging to understand what happened when things go wrong. That code becomes brittle, hard to test, and impossible to observe at scale.

## The Solution

**You just write the contract parsing, clause extraction, risk assessment, and summary generation logic. Conductor handles extraction retries, risk scoring sequencing, and contract review audit trails.**

Each worker handles one legal operation. Conductor manages the review pipeline, approval chains, deadline enforcement, and audit trail.

### What You Write: Workers

Clause extraction, risk identification, obligation mapping, and summary generation workers each analyze one dimension of contract language.

| Worker | Task | What It Does |
|---|---|---|
| **ParseWorker** | `cna_parse` | Parses the contract document, extracting page count (42) and section count (18) for downstream analysis |
| **ExtractWorker** | `cna_extract` | Extracts key clauses from the parsed contract. termination terms (90-day notice), liability caps, and clause inventory |
| **AnalyzeWorker** | `cna_analyze` | Evaluates risk across extracted clauses, flags high-risk items (e.g., non-compete clauses), and assigns an overall risk level (low/medium/high) |
| **SummarizeWorker** | `cna_summarize` | Generates a summary with a unique ID (SUM-694), consolidating parsed data, clause details, and identified risks into a single output |

Workers implement legal operations. document review, compliance checks, approval routing,  with realistic outputs. Replace with real document management and e-signature integrations and the workflow stays the same.

### The Workflow

```
cna_parse
    │
    ▼
cna_extract
    │
    ▼
cna_analyze
    │
    ▼
cna_summarize

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
java -jar target/contract-analysis-1.0.0.jar

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
java -jar target/contract-analysis-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow cna_contract_analysis \
  --version 1 \
  --input '{"contractId": "TEST-001", "contractType": "standard"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w cna_contract_analysis -s COMPLETED -c 5

```

## How to Extend

Connect each worker to your real contract tools. your CLM platform for document ingestion, an NLP service for clause extraction, your risk scoring engine for analysis, and the workflow runs identically in production.

- **ParseWorker** (`cna_parse`): use a document parsing service like Adobe PDF Extract API, Amazon Textract, or DocuSign Insight to extract structured text from contract PDFs
- **ExtractWorker** (`cna_extract`): integrate with a contract intelligence platform like Icertis, Ironclad, or Kira Systems to identify and extract specific clause types (termination, indemnification, liability)
- **AnalyzeWorker** (`cna_analyze`): connect to a legal AI risk engine or custom NLP model to evaluate clause-level risk, flagging deviations from standard playbook terms
- **SummarizeWorker** (`cna_summarize`): write the consolidated analysis to a CLM (Contract Lifecycle Management) system like Agiloft or ContractPodAi, and notify stakeholders via Slack or email

Swap your NLP model or clause library and the analysis pipeline continues operating identically.

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
contract-analysis/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/contractanalysis/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ContractAnalysisExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AnalyzeWorker.java
│       ├── ExtractWorker.java
│       ├── ParseWorker.java
│       └── SummarizeWorker.java
└── src/test/java/contractanalysis/workers/
    ├── AnalyzeWorkerTest.java        # 2 tests
    ├── ExtractWorkerTest.java        # 2 tests
    ├── ParseWorkerTest.java        # 2 tests
    └── SummarizeWorkerTest.java        # 2 tests

```
