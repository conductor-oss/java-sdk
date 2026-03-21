# Scholarship Processing in Java with Conductor :  Application, Evaluation, Ranking, Award, and Notification

A Java Conductor workflow example for scholarship processing. accepting student applications, evaluating them based on GPA and financial need, ranking applicants competitively, awarding the scholarship to qualified recipients, and notifying students of the outcome. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## The Problem

You need to process scholarship applications from submission to award. A student applies for a specific scholarship, the financial aid office evaluates their eligibility based on GPA and demonstrated financial need, applicants are ranked against the competition, an award decision is made based on ranking and available funds, and the student is notified whether they received the scholarship and for how much. Awarding without proper evaluation risks compliance violations; ranking without consistent criteria leads to unfair outcomes.

Without orchestration, you'd build a single batch-processing script that pulls applications from a database, scores them inline, sorts by rank, updates award status, and sends notification emails. manually handling ties in ranking, retrying failed email deliveries, and logging every decision to satisfy audit requirements from donors and federal financial aid regulations.

## The Solution

**You just write the application intake, eligibility evaluation, competitive ranking, award decision, and student notification logic. Conductor handles financial analysis retries, award routing, and application audit trails.**

Each scholarship concern is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of executing them in order (apply, evaluate, rank, award, notify), retrying if the financial aid system is temporarily unavailable, maintaining a complete audit trail of every application's journey from submission to decision, and resuming from the last successful step if the process crashes. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Application review, financial analysis, award determination, and disbursement workers handle scholarship decisions as a transparent pipeline.

| Worker | Task | What It Does |
|---|---|---|
| **ApplyWorker** | `scp_apply` | Receives the student's scholarship application with GPA and financial information |
| **EvaluateWorker** | `scp_evaluate` | Scores the application based on GPA, financial need, and scholarship-specific criteria |
| **RankWorker** | `scp_rank` | Ranks the applicant against other candidates based on evaluation score |
| **AwardWorker** | `scp_award` | Decides whether to award the scholarship based on rank and available funds |
| **NotifyWorker** | `scp_notify` | Notifies the student of the award decision and amount |

Workers implement educational operations. enrollment, grading, notifications,  with realistic outputs. Replace with real LMS and SIS integrations and the workflow stays the same.

### The Workflow

```
scp_apply
    │
    ▼
scp_evaluate
    │
    ▼
scp_rank
    │
    ▼
scp_award
    │
    ▼
scp_notify

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
java -jar target/scholarship-processing-1.0.0.jar

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
java -jar target/scholarship-processing-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow scp_scholarship_processing \
  --version 1 \
  --input '{"studentId": "TEST-001", "scholarshipId": "TEST-001", "gpa": "sample-gpa", "financialNeed": "sample-financialNeed"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w scp_scholarship_processing -s COMPLETED -c 5

```

## How to Extend

Connect each worker to your real financial aid stack. your aid system (PowerFAIDS, Banner) for applications, FAFSA/ISIR data for need verification, your packaging system for fund allocation, and the workflow runs identically in production.

- **ApplyWorker** (`scp_apply`): persist the application to your financial aid system (PowerFAIDS, Banner Financial Aid) and validate required documents
- **EvaluateWorker** (`scp_evaluate`): pull verified GPA from your SIS, check FAFSA/ISIR data for financial need, and apply scholarship-specific eligibility rules
- **RankWorker** (`scp_rank`): rank against the full applicant pool using your scoring algorithm; handle tie-breaking rules defined by the scholarship committee
- **AwardWorker** (`scp_award`): allocate funds from the scholarship's budget in your financial aid packaging system; check for over-award compliance with federal Title IV rules
- **NotifyWorker** (`scp_notify`): send award/denial letters via email (SendGrid/SES) and update the student's financial aid portal with the decision

Modify award criteria or financial analysis rules and the processing pipeline stays the same.

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
scholarship-processing/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/scholarshipprocessing/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ScholarshipProcessingExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ApplyWorker.java
│       ├── AwardWorker.java
│       ├── EvaluateWorker.java
│       ├── NotifyWorker.java
│       └── RankWorker.java
└── src/test/java/scholarshipprocessing/workers/
    ├── ApplyWorkerTest.java        # 2 tests
    ├── AwardWorkerTest.java        # 3 tests
    ├── EvaluateWorkerTest.java        # 2 tests
    ├── NotifyWorkerTest.java        # 2 tests
    └── RankWorkerTest.java        # 3 tests

```
