# Smoke Testing in Java with Conductor :  Endpoint Checks, Data Verification, Integration Tests, and Status Reporting

Orchestrates post-deployment smoke testing using [Conductor](https://github.com/conductor-oss/conductor). This workflow checks that critical API endpoints return 200 OK, verifies that the database has expected data (migrations ran, seed data present), tests integrations with external services (payment gateway reachable, email service connected), and reports the overall smoke test status with per-check results.

## Did the Deploy Actually Work?

You just deployed to staging. The CI pipeline says "success," but does the /health endpoint return 200? Can the API actually connect to the database? Is the payment gateway integration responding? Smoke tests answer these questions in 60 seconds. Before anyone files a bug report. Each check depends on the previous layer: there's no point testing database queries if the endpoints are down, and no point testing payment integration if the database has no data.

Without orchestration, you'd run a bash script with curl commands and grep for "200 OK." If the database check fails, you still run integration tests against a broken database and get confusing failures. There's no structured report of which checks passed and which failed, no retry when an endpoint takes a few seconds to warm up after deploy, and no audit trail of smoke test results across deployments.

## The Solution

**You write the health checks and integration tests. Conductor handles layered test sequencing, warm-up retries, and structured pass/fail reporting.**

Each layer of the smoke test is a simple, independent worker. The endpoint checker hits critical URLs (health, readiness, key API routes) and verifies HTTP status codes and response shapes. The data verifier connects to the database to confirm migrations ran, seed data exists, and key tables are queryable. The integration tester exercises connections to external services. Payment gateway, email provider, search index, cache layer. The status reporter aggregates all check results into a pass/fail verdict with details per check. Conductor executes them in strict sequence, retries endpoint checks when services need warm-up time after deploy, and provides a clear record of every smoke test run. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Three workers run layered smoke tests. Checking endpoint health, verifying database state, and reporting overall deployment status.

| Worker | Task | What It Does |
|---|---|---|
| **CheckEndpointsWorker** | `st_check_endpoints` | Hits all critical API endpoints and verifies they return healthy HTTP responses |
| **ReportStatusWorker** | `st_report_status` | Aggregates all check results into a final pass/fail verdict for the deployment |
| **VerifyDataWorker** | `st_verify_data` | Validates database connectivity and confirms sample queries return expected results |

Workers implement infrastructure operations with realistic output so you can see the automation flow without affecting real systems. Replace with real infrastructure API calls. the workflow and rollback logic stay the same.

### The Workflow

```
st_check_endpoints
    │
    ▼
st_verify_data
    │
    ▼
st_test_integrations
    │
    ▼
st_report_status

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
java -jar target/smoke-testing-1.0.0.jar

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
java -jar target/smoke-testing-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow smoke_testing_workflow \
  --version 1 \
  --input '{"service": "order-service", "environment": "staging"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w smoke_testing_workflow -s COMPLETED -c 5

```

## How to Extend

Each worker validates one layer of the deployment. replace the simulated calls with real HTTP clients, JDBC checks, and Stripe or SendGrid connectivity tests, and the smoke testing workflow runs unchanged.

- **CheckEndpointsWorker** → hit real endpoints: HTTP GET/POST with expected status codes and response schema validation using OkHttp or Java HttpClient, with configurable retry for cold-start latency
- **VerifyDataWorker** → query real databases: JDBC checks for migration version tables, row count assertions on critical tables, and sample queries that exercise key indexes
- **TestIntegrationsWorker** → test real external services: Stripe API ping for payment connectivity, SendGrid domain verification, Elasticsearch cluster health, Redis PING/PONG, and RabbitMQ/Kafka broker connectivity
- **ReportStatusWorker** → report to real channels: post smoke test results to Slack with per-check pass/fail, update GitHub deployment status, or write results to a deployment tracking database for historical comparison

Swap in real HTTP checks and database queries; the smoke test workflow uses the same pass/fail contract.

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
smoke-testing-smoke-testing/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/smoketesting/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── MainExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CheckEndpointsWorker.java
│       ├── ReportStatusWorker.java
│       └── VerifyDataWorker.java
└── src/test/java/smoketesting/
    └── MainExampleTest.java        # 2 tests. workflow resource loading, worker instantiation

```
