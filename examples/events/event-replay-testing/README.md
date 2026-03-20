# Event Replay Testing in Java Using Conductor

Event Replay Testing .  loads recorded events, sets up a sandbox environment, replays each event in a DO_WHILE loop comparing results, then generates a test report. Uses [Conductor](https://github.## The Problem

You need to test your event processing logic by replaying recorded production events in a sandbox environment. The workflow loads a set of recorded events, sets up an isolated test environment, replays each event through the processing pipeline while comparing actual results to expected results, and generates a test report summarizing pass/fail outcomes. Without replay testing, you only discover processing bugs when they corrupt production data.

Without orchestration, you'd build a custom test harness that loads event fixtures, manages sandbox setup/teardown, replays events in a loop, diffs results, and generates reports .  manually handling test environment cleanup, ensuring sandbox isolation, and managing the test event corpus.

## The Solution

**You just write the event-load, sandbox-setup, replay, and result-comparison workers. Conductor handles DO_WHILE test iteration, sandbox lifecycle management, and a durable record of every replay test run.**

Each replay-testing concern is a simple, independent worker .  a plain Java class that does one thing. Conductor takes care of loading events, setting up the sandbox, replaying each event in a DO_WHILE loop with result comparison, and generating the test report ,  retrying flaky tests and tracking every replay run. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Four workers drive replay testing: LoadEventsWorker reads recorded production events, SetupSandboxWorker provisions an isolated test environment, ReplayEventWorker processes each event in a DO_WHILE loop, and CompareResultWorker diffs actual vs expected output.

| Worker | Task | What It Does |
|---|---|---|
| **CompareResultWorker** | `rt_compare_result` | Compares the actual replay result against the expected result. |
| **LoadEventsWorker** | `rt_load_events` | Loads recorded events for replay testing. |
| **ReplayEventWorker** | `rt_replay_event` | Replays a single recorded event in the sandbox environment. |
| **SetupSandboxWorker** | `rt_setup_sandbox` | Sets up an isolated sandbox environment for replay testing. |

Workers simulate event processing with realistic payloads so you can trace the full event flow without external message brokers. Replace the simulation with real event sources .  the workflow and routing logic stay the same.

### The Workflow

```
rt_load_events
    │
    ▼
rt_setup_sandbox
    │
    ▼
DO_WHILE
    └── rt_replay_event
    └── rt_compare_result
    │
    ▼
rt_test_report
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
java -jar target/event-replay-testing-1.0.0.jar
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
java -jar target/event-replay-testing-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow event_replay_testing \
  --version 1 \
  --input '{"testSuiteId": "TEST-001", "eventSource": "test-value"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w event_replay_testing -s COMPLETED -c 5
```

## How to Extend

Connect each worker to your real event store, sandbox environment provisioner, and test comparison logic, the load-sandbox-replay-compare testing workflow stays exactly the same.

- **Event loader**: load recorded events from your event store (S3, Kafka topic replay, database snapshots) with configurable filters
- **Sandbox setup**: provision isolated test environments using Docker containers, AWS CloudFormation, or Terraform
- **Replay/compare worker**: replay events through your actual processing pipeline and diff results against golden snapshots
- **Report generator**: generate HTML/JUnit test reports with pass/fail counts, diff details, and regression analysis

Changing the sandbox setup or comparison logic inside any worker preserves the load-setup-replay-compare test pipeline.

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
event-replay-testing/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/eventreplaytesting/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── EventReplayTestingExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CompareResultWorker.java
│       ├── LoadEventsWorker.java
│       ├── ReplayEventWorker.java
│       └── SetupSandboxWorker.java
└── src/test/java/eventreplaytesting/workers/
    ├── CompareResultWorkerTest.java        # 10 tests
    ├── LoadEventsWorkerTest.java        # 10 tests
    ├── ReplayEventWorkerTest.java        # 9 tests
    ├── SetupSandboxWorkerTest.java        # 8 tests
    └── TestReportWorkerTest.java        # 9 tests
```
