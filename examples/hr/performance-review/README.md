# Performance Review in Java with Conductor :  Self-Evaluation, Manager Evaluation, Calibration, and Finalization

A Java Conductor workflow example for performance reviews. collecting the employee's self-evaluation, gathering the manager's evaluation with competency ratings, running cross-team calibration to normalize ratings, and finalizing the review with a composite score and development plan. Uses [Conductor](https://github.

## The Problem

You need to run the annual performance review cycle. Each employee writes a self-evaluation covering goal progress, accomplishments, and development areas. Their manager completes an evaluation with competency ratings, goal achievement scores, and narrative feedback. The calibration step normalizes ratings across teams to ensure consistent standards. preventing rating inflation in lenient teams or deflation in strict ones. Finally, the review is finalized with a composite rating that feeds into compensation, promotion, and development decisions. Each step must complete before the next,  you cannot calibrate without both evaluations, and you cannot finalize without calibration.

Without orchestration, you'd manage this through email reminders, spreadsheet trackers, and manual follow-ups. HR sends reminder emails, managers submit evaluations at different times, calibration happens on whiteboards, and final ratings are entered one by one. If a manager misses their deadline, the entire team's calibration is delayed. HR has no real-time visibility into which of hundreds of reviews are stuck at which stage.

## The Solution

**You just write the self-evaluation, manager evaluation, calibration, and review finalization logic. Conductor handles review routing, calibration sequencing, and evaluation cycle audit trails.**

Each stage of the review cycle is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of collecting the self-eval before the manager eval, calibrating only after both evaluations are in, finalizing after calibration, and giving HR complete real-time visibility into every review's progress across the organization. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Goal retrieval, self-assessment collection, manager review, and calibration workers each manage one stage of the performance evaluation cycle.

| Worker | Task | What It Does |
|---|---|---|
| **SelfEvalWorker** | `pfr_self_eval` | Collects the employee's self-evaluation with goal progress, accomplishments, and development areas |
| **ManagerEvalWorker** | `pfr_manager_eval` | Gathers the manager's evaluation with competency ratings, goal achievement scores, and narrative feedback |
| **CalibrateWorker** | `pfr_calibrate` | Normalizes ratings across teams against organizational standards and distribution guidelines |
| **FinalizeWorker** | `pfr_finalize` | Finalizes the review with composite rating, development plan, and compensation recommendation |

Workers implement HR operations. onboarding tasks, approvals, provisioning,  with realistic outputs. Replace with real HRIS and identity provider integrations and the workflow stays the same.

### The Workflow

```
pfr_self_eval
    │
    ▼
pfr_manager_eval
    │
    ▼
pfr_calibrate
    │
    ▼
pfr_finalize

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
java -jar target/performance-review-1.0.0.jar

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
java -jar target/performance-review-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow pfr_performance_review \
  --version 1 \
  --input '{"employeeId": "TEST-001", "reviewPeriod": "sample-reviewPeriod", "managerId": "TEST-001"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w pfr_performance_review -s COMPLETED -c 5

```

## How to Extend

Connect each worker to your real review systems. your HRIS for evaluation forms, your calibration platform for cross-team normalization, your talent management system for development plans, and the workflow runs identically in production.

- **SelfEvalWorker** → integrate with your performance management platform (Lattice, Culture Amp, 15Five) to collect employee self-assessments
- **ManagerEvalWorker** → pull manager evaluations from your HRIS with competency frameworks and goal tracking data
- **CalibrateWorker** → connect to your calibration analytics to apply forced distribution curves and flag rating outliers across teams
- **FinalizeWorker** → write finalized reviews to your HRIS and trigger downstream compensation and promotion workflows
- Add a **PeerFeedbackWorker** between self-eval and manager eval to collect feature-environment-degree feedback from colleagues
- Add a **GoalSettingWorker** after finalization to create next-period goals based on development areas identified in the review

Connect to your goal-tracking tool or calibration platform and the review pipeline keeps its structure.

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
performance-review-performance-review/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/performancereview/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── PerformanceReviewExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CalibrateWorker.java
│       ├── FinalizeWorker.java
│       ├── ManagerEvalWorker.java
│       └── SelfEvalWorker.java
└── src/test/java/performancereview/workers/
    ├── CalibrateWorkerTest.java        # 2 tests
    ├── FinalizeWorkerTest.java        # 2 tests
    ├── ManagerEvalWorkerTest.java        # 2 tests
    └── SelfEvalWorkerTest.java        # 2 tests

```
