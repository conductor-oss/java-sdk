# Patent Filing in Java with Conductor

A Java Conductor workflow example demonstrating Patent Filing. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers .## The Problem

An inventor submits a new invention disclosure. You need to draft a patent application with claims (typically 12+), conduct a prior art search to check for novelty, have a patent attorney review the draft for quality and completeness, file the application with the USPTO, and track its status through examination. Missing a filing deadline or overlooking prior art can forfeit patent rights entirely.

Without orchestration, you'd wire all of this together in a single monolithic class .  managing execution order manually, writing try/catch blocks around every step, building retry loops with backoff, and adding logging to understand what happened when things go wrong. That code becomes brittle, hard to test, and impossible to observe at scale.

## The Solution

**You just write the prior art search, application drafting, filing submission, and status tracking logic. Conductor handles search retries, filing sequencing, and patent application audit trails.**

Each worker handles one legal operation. Conductor manages the review pipeline, approval chains, deadline enforcement, and audit trail.

### What You Write: Workers

Prior art search, claim drafting, filing preparation, and submission workers each manage one stage of the patent application process.

| Worker | Task | What It Does |
|---|---|---|
| **DraftWorker** | `ptf_draft` | Takes the invention title and drafts a patent application, generating a draft ID (DRF-{timestamp}) and defining 12 claims |
| **ReviewWorker** | `ptf_review` | Reviews the draft for claim quality and completeness, returning an approval status and reviewer comments (e.g., "Claims well-drafted") |
| **PriorArtWorker** | `ptf_prior_art` | Searches prior art databases for the invention title, reports 3 matches found, and assesses conflict risk as "low" with a clearance flag |
| **FileWorker** | `ptf_file` | Files the patent application with the USPTO, generating an application number (US-2024-XXXXXX) and recording the filing date |
| **TrackWorker** | `ptf_track` | Creates a tracking record (TRK-{timestamp}) for the filed application and reports its status as "pending-examination" |

Workers simulate legal operations .  document review, compliance checks, approval routing ,  with realistic outputs. Replace with real document management and e-signature integrations and the workflow stays the same.

### The Workflow

```
ptf_draft
    │
    ▼
ptf_review
    │
    ▼
ptf_prior_art
    │
    ▼
ptf_file
    │
    ▼
ptf_track
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
java -jar target/patent-filing-1.0.0.jar
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
java -jar target/patent-filing-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow ptf_patent_filing \
  --version 1 \
  --input '{"inventionTitle": "test-value", "inventors": "test-value", "description": "test-value"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w ptf_patent_filing -s COMPLETED -c 5
```

## How to Extend

Swap each worker for your real IP tools .  your patent search database for prior art, your drafting platform for application preparation, the USPTO or EPO filing API for submission, and the workflow runs identically in production.

- **DraftWorker** (`ptf_draft`): integrate with a patent drafting tool like PatSnap or ClaimMaster to auto-generate claim sets from invention disclosures
- **ReviewWorker** (`ptf_review`): connect to an IP management platform like Anaqua or CPA Global for attorney review workflows and approval tracking
- **PriorArtWorker** (`ptf_prior_art`): query prior art databases via Google Patents API, USPTO PAIR, or Espacenet to search for novelty-blocking references
- **FileWorker** (`ptf_file`): submit applications electronically via USPTO EFS-Web/Patent Center API or WIPO ePCT for international filings
- **TrackWorker** (`ptf_track`): monitor prosecution status through USPTO PAIR or an IP docketing system like FoundationIP or AppColl

Swap prior art databases or filing systems and the patent pipeline continues without changes.

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
patent-filing/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/patentfiling/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── PatentFilingExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── DraftWorker.java
│       ├── FileWorker.java
│       ├── PriorArtWorker.java
│       ├── ReviewWorker.java
│       └── TrackWorker.java
└── src/test/java/patentfiling/workers/
    ├── DraftWorkerTest.java        # 2 tests
    ├── FileWorkerTest.java        # 2 tests
    ├── PriorArtWorkerTest.java        # 2 tests
    ├── ReviewWorkerTest.java        # 2 tests
    └── TrackWorkerTest.java        # 2 tests
```
