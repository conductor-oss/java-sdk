# Plagiarism Detection in Java with Conductor :  Submission, Scanning, Comparison, Verdict Routing, and Reporting

A Java Conductor workflow example for academic plagiarism detection .  ingesting a student submission, scanning the document for textual fingerprints, comparing against a corpus of known sources to compute a similarity score, routing to clean or flagged handling based on the verdict, and generating an integrity report. Uses [Conductor](https://github.## The Problem

You need to check student submissions for plagiarism before grades are assigned. The document must be ingested, scanned to extract textual fingerprints and n-grams, compared against a database of published papers, web content, and prior student submissions, and then a similarity score determines the verdict. If the submission is clean, it proceeds to grading; if flagged, it must be routed to the academic integrity office for review. Either way, an originality report is generated for the instructor's records.

Without orchestration, you'd chain document ingestion, text analysis, corpus comparison, and conditional notification in a single service .  manually handling timeouts when the similarity engine takes too long on large documents, writing if/else routing for clean vs: flagged results, and logging every check to defend decisions during student appeals.

## The Solution

**You just write the document scanning, corpus comparison, clean/flagged verdict routing, and originality reporting logic. Conductor handles source comparison retries, similarity scoring, and detection audit trails.**

Each plagiarism-check concern is a simple, independent worker .  a plain Java class that does one thing. Conductor takes care of executing them in order (submit, scan, compare), then routing via a SWITCH task to the correct outcome (clean or flagged), and finally generating the report ,  retrying if the similarity engine times out, maintaining an audit trail for every submission, and resuming from the last step if the process crashes. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Document ingestion, source comparison, similarity scoring, and report generation workers each tackle one stage of academic integrity checking.

| Worker | Task | What It Does |
|---|---|---|
| **SubmitWorker** | `plg_submit` | Ingests the student's document submission and records it for processing |
| **ScanWorker** | `plg_scan` | Extracts textual fingerprints, n-grams, and structural features from the document |
| **CompareWorker** | `plg_compare` | Compares scan results against a corpus of sources and computes a similarity score and verdict |
| **HandleCleanWorker** | `plg_handle_clean` | Marks the submission as original and clears it for grading |
| **HandleFlaggedWorker** | `plg_handle_flagged` | Flags the submission for academic integrity review with the similarity score |
| **ReportWorker** | `plg_report` | Generates an originality report for the instructor's records |

Workers simulate educational operations .  enrollment, grading, notifications ,  with realistic outputs. Replace with real LMS and SIS integrations and the workflow stays the same.

### The Workflow

```
plg_submit
    │
    ▼
plg_scan
    │
    ▼
plg_compare
    │
    ▼
SWITCH (plg_switch_ref)
    ├── clean: plg_handle_clean
    └── default: plg_handle_flagged
    │
    ▼
plg_report
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
java -jar target/plagiarism-detection-1.0.0.jar
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
java -jar target/plagiarism-detection-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow plg_plagiarism_detection \
  --version 1 \
  --input '{"studentId": "TEST-001", "assignmentId": "TEST-001", "documentText": "test-value"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w plg_plagiarism_detection -s COMPLETED -c 5
```

## How to Extend

Swap each worker for your real plagiarism tools. Turnitin or Copyscape for similarity scanning, your institutional document corpus for comparison, your academic integrity system for flagged-case routing, and the workflow runs identically in production.

- **ScanWorker** (`plg_scan`): integrate with a text analysis service (Turnitin API, Copyscape API) or run local NLP fingerprinting with Apache OpenNLP or spaCy
- **CompareWorker** (`plg_compare`): query your institutional document corpus and external databases (CrossRef, PubMed, web crawl indexes) for similarity matching
- **HandleCleanWorker** (`plg_handle_clean`): update the assignment status in your LMS to "cleared" and release it for grading
- **HandleFlaggedWorker** (`plg_handle_flagged`): create a case in your academic integrity system and notify the instructor and dean of students via email
- **ReportWorker** (`plg_report`): generate a PDF originality report with highlighted matching passages and source attribution, store it alongside the submission

Update your source database or similarity algorithm and the detection pipeline operates identically.

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
plagiarism-detection/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/plagiarismdetection/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── PlagiarismDetectionExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CompareWorker.java
│       ├── HandleCleanWorker.java
│       ├── HandleFlaggedWorker.java
│       ├── ReportWorker.java
│       ├── ScanWorker.java
│       └── SubmitWorker.java
└── src/test/java/plagiarismdetection/workers/
    ├── CompareWorkerTest.java        # 2 tests
    ├── HandleCleanWorkerTest.java        # 2 tests
    ├── HandleFlaggedWorkerTest.java        # 2 tests
    ├── ReportWorkerTest.java        # 2 tests
    ├── ScanWorkerTest.java        # 3 tests
    └── SubmitWorkerTest.java        # 2 tests
```
