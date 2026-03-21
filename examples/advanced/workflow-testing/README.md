# Workflow Testing in Java Using Conductor: Fixture Setup, Assertions, Teardown, and Report Generation

A Java Conductor workflow example for automated workflow test orchestration: defining test fixtures, executing the workflow under test against golden inputs, asserting that actual outputs match expected outputs field-by-field, tearing down test resources, and generating a pass/fail report. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers, you write the business logic, Conductor handles retries, failure routing, durability, and observability.

## The Problem

You have workflows in production and you need to verify they behave correctly after every change. Unit-testing individual workers is easy, they're just Java classes. But testing the full workflow, does the SWITCH route correctly? Does the FORK_JOIN merge outputs properly? Does the workflow handle a failed task and retry?, requires running the entire orchestration end-to-end.

You need to set up test data, trigger the workflow, wait for completion, check that every task produced the expected output, clean up, and report whether the test passed. Doing this manually after every change is unsustainable. Automating it means defining test suites with golden inputs and expected outputs, running them against the real Conductor engine, and producing pass/fail reports with detailed assertion results.

This example is for teams building CI/CD pipelines around Conductor workflows, validating integration environments before deployment, or running golden input/output validation suites against staging.

## The Solution

**You write the fixtures and assertions. Conductor handles the test orchestration, teardown sequencing, and report generation.**

`SetupWorker` prepares the test environment: creating mock databases, mock API endpoints, and test data fixtures. `ExecuteWorker` triggers the workflow under test with the golden input and captures its output. `AssertWorker` compares each field of the actual output against the expected output, producing a per-assertion pass/fail result with expected vs, actual values. `TeardownWorker` releases mock databases, API endpoints, and test data. `ReportWorker` computes the final test report from the assertion results, total assertions, passed count, overall pass/fail verdict, and teardown status. Conductor records every test run for regression tracking.

### What You Write: Workers

Five workers orchestrate the test lifecycle. Fixture setup, workflow execution against golden inputs, field-by-field assertion, resource teardown, and pass/fail report generation.

| Worker | Task | What It Does |
|---|---|---|
| **SetupWorker** | `wft_setup` | Creates test fixtures: mock database (localhost:5432/test_db), mock API endpoint (localhost:9090), and two test data records (alpha, beta). |
| **ExecuteWorker** | `wft_execute` | Runs the workflow under test with the fixtures and captures its actual output (status, processed count, per-record results). |
| **AssertWorker** | `wft_assert` | Compares actual vs. expected output field-by-field: checks `status` equality and `processed` count match. Returns a list of named assertions with expected/actual/passed for each. |
| **TeardownWorker** | `wft_teardown` | Releases all fixture resources: mockDb, mockApi, testData. Reports cleanup status. |
| **ReportWorker** | `wft_report` | Computes the test report from the assertion list: counts total and passed assertions, determines overall PASSED/FAILED verdict, includes teardown status. |

Workers implement the pattern behavior with realistic inputs and outputs so you can observe the advanced workflow mechanics. Replace with real implementations, the pattern and Conductor orchestration stay the same.

### The Workflow

The five tasks run sequentially. Each task's output feeds into the next task's input via Conductor's expression language.; no custom wiring code.

```
wft_setup          (create fixtures: mockDb, mockApi, testData)
    |
    v
wft_execute        (run workflow under test with fixtures, capture actualOutput)
    |
    v
wft_assert         (compare actualOutput vs expectedOutput, produce per-assertion results)
    |
    v
wft_teardown       (release fixtures, report cleanedUp status)
    |
    v
wft_report         (compute report: total/passed assertions, PASSED/FAILED verdict)

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
java -jar target/workflow-testing-1.0.0.jar

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
java -jar target/workflow-testing-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow wft_workflow_testing \
  --version 1 \
  --input '{"testSuite": "order-processing-tests", "workflowUnderTest": "order_processing_workflow", "expectedOutput": {"status": "SUCCESS", "processed": 2}}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w wft_workflow_testing -s COMPLETED -c 5

```

## How to Extend

Each worker handles one test lifecycle phase. Replace the demo fixture setup and assertions with real test framework integrations and the setup-execute-assert-report pipeline runs unchanged.

- **SetupWorker** (`wft_setup`): create real test fixtures: insert test data into a staging database via JDBC, deploy mock services with WireMock or MockServer, or register test workflow definitions via Conductor's metadata API
- **ExecuteWorker** (`wft_execute`): trigger the real workflow under test: call `WorkflowClient.startWorkflow()` with the golden input, poll for completion, and capture the actual output from the completed workflow execution
- **AssertWorker** (`wft_assert`): implement rich assertions: JSON deep-compare using Jackson's `JsonNode.equals()`, regex matching for dynamic fields (timestamps, UUIDs), or integrate AssertJ/Hamcrest for fluent assertion DSLs with detailed mismatch messages
- **TeardownWorker** (`wft_teardown`): clean up real resources: drop staging database tables, stop WireMock servers, deregister temporary workflow definitions
- **ReportWorker** (`wft_report`): generate CI/CD-compatible reports: JUnit XML format for Jenkins/GitHub Actions integration, HTML reports via Allure for visual dashboards, or post pass/fail summaries to Slack/Teams with links to failed Conductor executions

The fixture and assertion contract stays fixed. Swap the demo test runner for a real Conductor execution and the assert-teardown-report pipeline runs unchanged.

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
workflow-testing/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/workflowtesting/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── WorkflowTestingExample.java  # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AssertWorker.java        # Field-by-field output comparison
│       ├── ExecuteWorker.java       # Runs workflow under test
│       ├── ReportWorker.java        # Computes pass/fail report from assertions
│       ├── SetupWorker.java         # Creates mock fixtures
│       └── TeardownWorker.java      # Releases test resources
└── src/test/java/workflowtesting/workers/
    ├── AssertWorkerTest.java        # 10 tests. Matching, mismatch, null handling
    ├── ExecuteWorkerTest.java       # 9 tests. Output shape, null handling
    ├── ReportWorkerTest.java        # 13 tests. Assertion counting, verdict, teardown
    ├── SetupWorkerTest.java         # 8 tests. Fixture structure, null handling
    └── TeardownWorkerTest.java      # 8 tests. Cleanup status, resource list

```
