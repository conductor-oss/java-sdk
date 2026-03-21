# Performance Testing in Java with Conductor :  Environment Prep, Load Testing, Results Analysis, and Report Generation

Orchestrates automated performance testing using [Conductor](https://github.com/conductor-oss/conductor). This workflow prepares a test environment, runs a load test against the target service at a specified requests-per-second rate for a configured duration, analyzes the results (P50/P95/P99 latency, error rates, throughput), and generates a performance report with pass/fail against SLO targets.

## Can Your Service Handle the Load?

Before launching a marketing campaign that will 5x your traffic, you need to know if your API can handle 10,000 requests per second without latency spiking above 200ms. Performance testing requires a clean test environment (isolated from production traffic), a load generator that ramps to the target RPS over the configured duration, analysis of the results (did P99 latency stay under 500ms? did error rate stay below 1%?), and a report that tells the team whether it's safe to launch. Each step depends on the previous one: you can't analyze results before the test runs, and the test can't run until the environment is ready.

Without orchestration, you'd SSH into a load testing box, run a k6 or JMeter script, eyeball the Grafana dashboard, and send a Slack message saying "looks good." There's no structured report, no comparison against SLO targets, no record of which test ran at what RPS for how long, and no audit trail for the next person who asks "did we performance test this release?"

## The Solution

**You write the load generation and analysis logic. Conductor handles environment-to-report sequencing, SLO comparison, and test result tracking.**

Each stage of the performance testing pipeline is a simple, independent worker. The environment preparer provisions or configures the test environment. Spinning up isolated infrastructure, warming caches, and seeding test data. The load tester generates traffic at the target RPS for the specified duration, recording response times and error counts. The results analyzer computes percentile latencies (P50, P95, P99), throughput, error rates, and compares them against SLO thresholds. The report generator produces a structured performance report with pass/fail verdicts and trend comparisons against previous runs. Conductor executes them in strict sequence, ensures the load test only runs after the environment is ready, retries if environment provisioning fails, and tracks all test parameters and results. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Three workers execute the performance test. Preparing the environment, running the load test at target RPS, and analyzing results against SLO thresholds.

| Worker | Task | What It Does |
|---|---|---|
| **AnalyzeResultsWorker** | `pt_analyze_results` | Compares load test results against SLO thresholds (P99 latency, error rates) to determine pass/fail |
| **GenerateReportWorker** | `pt_generate_report` | Produces a performance report with percentile latencies, throughput, and trend analysis |
| **PrepareEnvWorker** | `pt_prepare_env` | Provisions and configures an isolated load test environment (warm caches, seed data) |

Workers simulate infrastructure operations with realistic output so you can see the automation flow without affecting real systems. Replace with real infrastructure API calls .  the workflow and rollback logic stay the same.

### The Workflow

```
pt_prepare_env
    │
    ▼
pt_run_load_test
    │
    ▼
pt_analyze_results
    │
    ▼
pt_generate_report

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
java -jar target/performance-testing-1.0.0.jar

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
java -jar target/performance-testing-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow performance_testing_workflow \
  --version 1 \
  --input '{"service": "order-service", "targetRps": "production", "duration": "sample-duration"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w performance_testing_workflow -s COMPLETED -c 5

```

## How to Extend

Each worker handles one testing stage .  replace the simulated calls with k6, Gatling, or JMeter for real load generation and latency analysis, and the testing workflow runs unchanged.

- **PrepareEnvWorker** → provision real test environments: Terraform/Pulumi for infrastructure, Kubernetes namespaces for isolation, or AWS CloudFormation for ephemeral stacks with pre-warmed caches and seeded databases
- **RunLoadTestWorker** → execute real load tests: k6 for script-based HTTP testing, Gatling for Scala-based scenarios, JMeter for complex test plans, or Locust for Python-based distributed load generation
- **AnalyzeResultsWorker** → compute real metrics: parse k6/Gatling output for percentile latencies, compare against SLO thresholds defined in a config file, detect performance regressions by comparing against previous baseline runs
- **GenerateReportWorker** → produce real reports: HTML reports with charts (latency distribution, throughput over time), post results to Grafana annotations, or publish to a performance tracking dashboard for historical trend analysis

Replace with k6 or Gatling for real load testing; the performance pipeline maintains the same data format.

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
performance-testing-performance-testing/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/performancetesting/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── MainExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AnalyzeResultsWorker.java
│       ├── GenerateReportWorker.java
│       └── PrepareEnvWorker.java
└── src/test/java/performancetesting/
    └── MainExampleTest.java        # 2 tests .  workflow resource loading, worker instantiation

```
