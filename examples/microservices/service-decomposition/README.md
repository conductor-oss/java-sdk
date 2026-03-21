# Service Decomposition in Java with Conductor

Strangler fig pattern: routes requests to monolith or microservice based on feature flags, with optional shadow comparison. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## The Problem

Migrating from a monolith to microservices cannot happen all at once. The strangler fig pattern uses feature flags to gradually route requests from the monolith to new microservices. This workflow checks a feature flag to decide the routing target (monolith, microservice, or shadow mode), and in shadow mode runs both in parallel to compare results before committing to the new service.

Without orchestration, the routing proxy is a bespoke middleware that hard-codes if/else branches for each migrated feature. Shadow-mode comparison requires custom diff logic, and there is no visibility into what percentage of traffic is hitting the monolith vs the microservice.

## The Solution

**You just write the feature-flag check, monolith-call, microservice-call, and result-comparison workers. Conductor handles flag-based conditional routing, parallel shadow execution, and full visibility into migration traffic splits.**

Each worker represents a service boundary. Conductor manages cross-service orchestration, compensating transactions, timeout enforcement, and distributed tracing .  your workers just make the service calls.

### What You Write: Workers

Four workers implement the strangler fig: CheckFeatureFlagWorker determines the routing target, CallMonolithWorker handles legacy requests, CallMicroserviceWorker handles migrated ones, and CompareResultsWorker diffs outputs in shadow mode.

| Worker | Task | What It Does |
|---|---|---|
| **CallMicroserviceWorker** | `sd_call_microservice` | Calls the new microservice to process the request. |
| **CallMonolithWorker** | `sd_call_monolith` | Calls the legacy monolith to process the request. |
| **CheckFeatureFlagWorker** | `sd_check_feature_flag` | Checks feature flag to determine routing target. |
| **CompareResultsWorker** | `sd_compare_results` | Compares results from monolith and microservice in shadow mode. |

Workers simulate service calls with realistic request/response shapes so you can see the coordination pattern without running the full service mesh. Replace with real HTTP clients .  the workflow coordination stays the same.

### The Workflow

```
sd_check_feature_flag
    │
    ▼
SWITCH (route_ref)
    ├── microservice: sd_call_microservice
    ├── shadow: parallel_compare -> join_compare -> sd_compare_results
    └── default: sd_call_monolith

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
java -jar target/service-decomposition-1.0.0.jar

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
java -jar target/service-decomposition-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow service_decomposition_workflow \
  --version 1 \
  --input '{"feature": "sample-feature", "request": "sample-request"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w service_decomposition_workflow -s COMPLETED -c 5

```

## How to Extend

Point each worker at your real monolith endpoint, new microservice, and feature flag provider (LaunchDarkly, Unleash), the strangler-fig routing workflow stays exactly the same.

- **CallMicroserviceWorker** (`sd_call_microservice`): make a real HTTP/gRPC call to the new microservice
- **CallMonolithWorker** (`sd_call_monolith`): make a real HTTP call to the legacy monolith endpoint
- **CheckFeatureFlagWorker** (`sd_check_feature_flag`): evaluate flags in LaunchDarkly, Unleash, or a database-backed flag table to control migration percentage

Advancing a feature from monolith to microservice routing only requires toggling the flag, the workflow definition stays the same.

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
service-decomposition/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/servicedecomposition/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ServiceDecompositionExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CallMicroserviceWorker.java
│       ├── CallMonolithWorker.java
│       ├── CheckFeatureFlagWorker.java
│       └── CompareResultsWorker.java

```
