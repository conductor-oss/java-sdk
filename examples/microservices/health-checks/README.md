# Health Checks in Java with Conductor

Check health of multiple services in parallel. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers .## The Problem

Monitoring the health of multiple services (API gateway, database, cache) requires hitting each service's health endpoint, collecting status and latency data, and producing a consolidated health report. These checks should run in parallel to minimize total check time, and the report must account for partial failures.

Without orchestration, health checks are run sequentially in a cron job, making the check cycle slow and providing no structured report. If one health endpoint times out, it blocks all subsequent checks, and there is no historical record of health status over time.

## The Solution

**You just write the service-check and report-generation workers. Conductor handles parallel execution of all checks, per-service timeout isolation, and historical tracking of every health run.**

Each worker represents a service boundary. Conductor manages cross-service orchestration, compensating transactions, timeout enforcement, and distributed tracing .  your workers just make the service calls.

### What You Write: Workers

Two worker types power the health pipeline: CheckServiceWorker probes individual service endpoints in parallel, then GenerateReportWorker consolidates all results into a single health report.

| Worker | Task | What It Does |
|---|---|---|
| **CheckServiceWorker** | `hc_check_service` | Checks health of an individual service. |
| **GenerateReportWorker** | `hc_generate_report` | Generates a health report from individual service checks. |

Workers simulate service calls with realistic request/response shapes so you can see the coordination pattern without running the full service mesh. Replace with real HTTP clients .  the workflow coordination stays the same.

### The Workflow

```
FORK_JOIN
    ├── hc_check_service
    ├── hc_check_service
    └── hc_check_service
    │
    ▼
JOIN (wait for all branches)
hc_generate_report
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
java -jar target/health-checks-1.0.0.jar
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
java -jar target/health-checks-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow health_checks_295 \
  --version 1 \
  --input '{"input": "test"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w health_checks_295 -s COMPLETED -c 5
```

## How to Extend

Point each check worker at your real service health endpoints (Spring Boot Actuator, custom /health routes) and publish the report to Grafana or PagerDuty, the parallel-check-and-report workflow stays exactly the same.

- **CheckServiceWorker** (`hc_check_service`): make real HTTP calls to each service's health endpoint (e.g., Spring Boot Actuator /health)
- **GenerateReportWorker** (`hc_generate_report`): publish the health report to a monitoring dashboard (Grafana, PagerDuty, Slack webhook)

Pointing CheckServiceWorker at real Actuator endpoints and GenerateReportWorker at Grafana changes nothing in the workflow.

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
health-checks/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/healthchecks/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── HealthChecksExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CheckServiceWorker.java
│       └── GenerateReportWorker.java
└── src/test/java/healthchecks/workers/
    ├── CheckServiceWorkerTest.java        # 5 tests
    └── GenerateReportWorkerTest.java        # 4 tests
```
