# Automated Testing Pipeline in Java with Conductor :  Setup Environment, Parallel Test Suites, Aggregate Results

Orchestrates a test suite: setup environment, run unit/integration/e2e tests in parallel, aggregate results. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers .  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## Parallel Testing Cuts Pipeline Time by 3x

A test suite takes 30 minutes to run sequentially: 10 minutes for unit tests, 15 minutes for integration tests, 5 minutes for performance tests. Running all three in parallel brings it down to 15 minutes (the slowest suite). But parallel execution requires a shared environment setup, independent test runners, and a final aggregation step that merges results from all three suites.

If integration tests fail, unit test results are still valid .  you don't need to re-run them. If the environment setup fails, no tests should run. After all suites complete, the aggregation step needs to produce a single pass/fail verdict with per-suite breakdowns, coverage metrics, and failure details.

## The Solution

**You write the test runners and environment setup. Conductor handles parallel execution, result aggregation, and per-suite failure isolation.**

`SetupEnvWorker` provisions the test environment .  spinning up test databases, mock services, and test fixtures for the specified branch. `FORK_JOIN` dispatches parallel test suites: unit tests, integration tests, and performance tests run simultaneously in isolated runners. After `JOIN` collects all results, `AggregateResultsWorker` merges the per-suite reports into a unified test report with pass/fail counts, coverage metrics, failure details, and execution times. Conductor runs all suites in parallel and records each suite's results independently.

### What You Write: Workers

Five workers run the test pipeline. Setting up the environment, executing unit/integration/e2e suites in parallel, and aggregating results into a unified report.

| Worker | Task | What It Does |
|---|---|---|
| **AggregateResults** | `at_aggregate_results` | Aggregates test results from all suites. |
| **RunE2e** | `at_run_e2e` | Runs end-to-end tests. |
| **RunIntegration** | `at_run_integration` | Runs integration tests. |
| **RunUnit** | `at_run_unit` | Runs unit tests. |
| **SetupEnv** | `at_setup_env` | Sets up the test environment. |

Workers simulate infrastructure operations with realistic output so you can see the automation flow without affecting real systems. Replace with real infrastructure API calls .  the workflow and rollback logic stay the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |
| **Parallel execution** | FORK_JOIN runs multiple tasks simultaneously and waits for all to complete |

### The Workflow

```
at_setup_env
    │
    ▼
FORK_JOIN
    ├── at_run_unit
    ├── at_run_integration
    └── at_run_e2e
    │
    ▼
JOIN (wait for all branches)
at_aggregate_results
```

## Example Output

```
=== Automated Testing Demo ===

Step 1: Registering task definitions...
  Registered: at_aggregate_results, at_run_e2e, at_run_integration, at_run_unit, at_setup_env

Step 2: Registering workflow 'automated_testing_workflow'...
  Workflow registered.

Step 3: Starting workers...
  5 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [at_aggregate_results] All tests passed
  [at_run_e2e] 12 tests passed
  [at_run_integration] 18 tests passed
  [at_run_unit] 247 tests passed
  [at_setup_env] Test environment ready

  Status: COMPLETED
  Output: {totalPassed=..., totalFailed=..., coverage=..., passed=...}

Result: PASSED
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
docker run -d -p 8080:8080 -p 1234:5000 orkesio/orkes-conductor-standalone:latest

# Wait for Conductor to be ready
until curl -sf http://localhost:8080/health > /dev/null; do sleep 2; done

# Build and run
mvn package -DskipTests
java -jar target/automated-testing-1.0.0.jar
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
java -jar target/automated-testing-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow automated_testing_workflow \
  --version 1 \
  --input '{"suite": "integration", "branch": "main"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w automated_testing_workflow -s COMPLETED -c 5
```

## How to Extend

Each worker runs one test suite or setup task .  replace the simulated calls with Maven Surefire, Testcontainers, or k6 for real test execution and coverage reporting, and the testing workflow runs unchanged.

- **Test suite workers**: execute real test suites via Maven Surefire (unit), Testcontainers (integration), or k6/Gatling (performance) with actual pass/fail results and coverage reports
- **SetupEnvWorker** (`at_setup_env`): provision ephemeral test environments using Docker Compose, Kubernetes namespaces, or Terraform workspaces with automatic cleanup after test completion
- **AggregateResultsWorker** (`at_aggregate_results`): generate HTML test reports, push results to SonarQube for quality gate enforcement, and post summaries to GitHub PR checks

Swap in real test frameworks like JUnit and Selenium; the parallel test pipeline maintains the same aggregation interface.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):

## Project Structure

```
automated-testing/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/automatedtesting/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── AutomatedTestingExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AggregateResults.java
│       ├── RunE2e.java
│       ├── RunIntegration.java
│       ├── RunUnit.java
│       └── SetupEnv.java
└── src/test/java/automatedtesting/workers/
    ├── AggregateResultsTest.java        # 7 tests
    ├── RunUnitTest.java        # 7 tests
    └── SetupEnvTest.java        # 7 tests
```
