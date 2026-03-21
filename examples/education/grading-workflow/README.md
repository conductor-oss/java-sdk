# Grading Workflow in Java with Conductor :  Submission, Scoring, Review, Recording, and Student Notification

A Java Conductor workflow example for assignment grading. receiving a student submission, scoring it against the rubric, reviewing the grade for accuracy, recording the final score in the gradebook, and notifying the student of their result. Uses [Conductor](https://github.

## The Problem

You need to grade student assignments end-to-end. A student submits their work, the grading system scores it against the assignment rubric, an instructor or peer reviews the score for fairness and accuracy, the final grade is recorded in the course gradebook, and the student is notified of their result. Recording a grade before review is complete risks posting inaccurate scores; failing to notify leaves students in the dark about their performance.

Without orchestration, you'd build a single grading script that ingests submissions, runs scoring logic, updates the gradebook, and sends notification emails. manually ensuring grades are never recorded without review, retrying when the gradebook database is locked, and logging every step to investigate grade disputes when a student claims they were scored incorrectly.

## The Solution

**You just write the submission intake, rubric scoring, grade review, gradebook recording, and student notification logic. Conductor handles scoring retries, grade routing, and complete assessment audit trails.**

Each grading concern is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of executing them in order (submit, grade, review, record, notify), retrying if the gradebook service is temporarily unavailable, maintaining a complete audit trail of every grade from submission to notification, and resuming from the last successful step if the process crashes. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Submission collection, grading, review, and grade posting workers each handle one phase of the assessment evaluation cycle.

| Worker | Task | What It Does |
|---|---|---|
| **SubmitWorker** | `grd_submit` | Receives and logs the student's assignment submission |
| **GradeWorker** | `grd_grade` | Scores the submission against the assignment rubric |
| **ReviewWorker** | `grd_review` | Reviews the assigned score for accuracy and fairness, producing a final score |
| **RecordWorker** | `grd_record` | Records the final score in the course gradebook |
| **NotifyWorker** | `grd_notify` | Notifies the student of their final grade |

Workers implement educational operations. enrollment, grading, notifications,  with realistic outputs. Replace with real LMS and SIS integrations and the workflow stays the same.

### The Workflow

```
grd_submit
    │
    ▼
grd_grade
    │
    ▼
grd_review
    │
    ▼
grd_record
    │
    ▼
grd_notify

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
java -jar target/grading-workflow-1.0.0.jar

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
java -jar target/grading-workflow-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow grd_grading \
  --version 1 \
  --input '{"studentId": "TEST-001", "courseId": "TEST-001", "assignmentId": "TEST-001"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w grd_grading -s COMPLETED -c 5

```

## How to Extend

Wire each worker to your real grading tools. your LMS (Canvas, Blackboard) for submission intake and gradebook writes, automated scoring engines for objective questions, SendGrid or Twilio for student notifications, and the workflow runs identically in production.

- **SubmitWorker** (`grd_submit`): accept file uploads from your LMS (Canvas, Blackboard, Moodle) and store them in S3/GCS for processing
- **GradeWorker** (`grd_grade`): run automated grading (unit test execution for code assignments, NLP-based essay scoring, or answer-key matching for multiple choice)
- **ReviewWorker** (`grd_review`): route to a human instructor via a WAIT task for manual review, or apply statistical anomaly detection to flag outlier grades
- **RecordWorker** (`grd_record`): write the final score to your LMS gradebook API or student information system
- **NotifyWorker** (`grd_notify`): send grade notification via LMS push notification, email (SendGrid/SES), or SMS (Twilio) with feedback comments

Plug in automated scoring or peer review and the grading pipeline handles either approach without changes.

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
grading-workflow/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/gradingworkflow/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── GradingWorkflowExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── GradeWorker.java
│       ├── NotifyWorker.java
│       ├── RecordWorker.java
│       ├── ReviewWorker.java
│       └── SubmitWorker.java
└── src/test/java/gradingworkflow/workers/
    ├── GradeWorkerTest.java        # 2 tests
    ├── NotifyWorkerTest.java        # 2 tests
    ├── RecordWorkerTest.java        # 2 tests
    ├── ReviewWorkerTest.java        # 2 tests
    └── SubmitWorkerTest.java        # 2 tests

```
