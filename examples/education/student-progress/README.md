# Student Progress in Java with Conductor :  Grade Collection, GPA Analysis, Progress Reports, and Notifications

A Java Conductor workflow example for tracking student academic progress .  collecting all course grades for a semester, analyzing performance to compute GPA and academic standing, generating a progress report, and notifying the student of their results. Uses [Conductor](https://github.## The Problem

You need to evaluate a student's academic progress at the end of each semester. This means pulling grades from all enrolled courses, computing the semester GPA and cumulative standing (good standing, probation, dean's list), generating a formal progress report for the student's academic record, and notifying the student of their standing. Sending a progress report with incorrect GPA calculations undermines institutional credibility; failing to flag probation status delays critical academic interventions.

Without orchestration, you'd build a single end-of-semester batch job that queries the gradebook, runs GPA calculations, generates PDF reports, and sends email notifications .  manually handling missing grades from courses with incomplete submissions, retrying when the report generation service crashes, and logging everything to investigate discrepancies when a student disputes their standing.

## The Solution

**You just write the grade collection, GPA analysis, progress report generation, and student notification logic. Conductor handles data collection retries, alert routing, and progress tracking across terms.**

Each progress-tracking concern is a simple, independent worker .  a plain Java class that does one thing. Conductor takes care of executing them in order (collect grades, analyze, generate report, notify), retrying if the gradebook service is temporarily unavailable, maintaining an audit trail of every progress evaluation, and resuming from the last successful step if the process crashes. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Data collection, performance analysis, alert generation, and report distribution workers monitor student outcomes through independent checkpoints.

| Worker | Task | What It Does |
|---|---|---|
| **CollectGradesWorker** | `spr_collect_grades` | Pulls all course grades for the student for the specified semester |
| **AnalyzeWorker** | `spr_analyze` | Computes semester GPA and determines academic standing (good standing, probation, dean's list) |
| **GenerateReportWorker** | `spr_generate_report` | Creates a formal progress report with GPA, standing, and course-by-course breakdown |
| **NotifyWorker** | `spr_notify` | Notifies the student of their GPA and academic standing |

Workers simulate educational operations .  enrollment, grading, notifications ,  with realistic outputs. Replace with real LMS and SIS integrations and the workflow stays the same.

### The Workflow

```
spr_collect_grades
    │
    ▼
spr_analyze
    │
    ▼
spr_generate_report
    │
    ▼
spr_notify
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
java -jar target/student-progress-1.0.0.jar
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
java -jar target/student-progress-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow spr_student_progress \
  --version 1 \
  --input '{"studentId": "TEST-001", "semester": "test-value"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w spr_student_progress -s COMPLETED -c 5
```

## How to Extend

Wire each worker to your real academic systems .  your LMS gradebook (Canvas, Blackboard) for grade collection, your SIS for GPA computation and standing rules, a PDF renderer for progress reports, and the workflow runs identically in production.

- **CollectGradesWorker** (`spr_collect_grades`): query your LMS gradebook API (Canvas, Blackboard) or SIS (Banner, PeopleSoft) for all finalized course grades for the semester
- **AnalyzeWorker** (`spr_analyze`): compute GPA using your institution's grading scale (4.0, weighted, etc.), apply academic standing rules (probation thresholds, dean's list cutoffs, satisfactory academic progress for financial aid)
- **GenerateReportWorker** (`spr_generate_report`): render a PDF progress report using a template engine (iText, Apache PDFBox) and store it in the student's permanent record
- **NotifyWorker** (`spr_notify`): send progress notifications via email, SMS, or push notification; for probation cases, also notify the academic advisor

Change your analytics engine or alert thresholds and the progress monitoring pipeline continues unmodified.

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
student-progress/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/studentprogress/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── StudentProgressExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AnalyzeWorker.java
│       ├── CollectGradesWorker.java
│       ├── GenerateReportWorker.java
│       └── NotifyWorker.java
└── src/test/java/studentprogress/workers/
    ├── AnalyzeWorkerTest.java        # 2 tests
    ├── CollectGradesWorkerTest.java        # 2 tests
    ├── GenerateReportWorkerTest.java        # 2 tests
    └── NotifyWorkerTest.java        # 2 tests
```
