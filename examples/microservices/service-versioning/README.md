# Service Versioning in Java with Conductor

API version management with version routing. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers .## The Problem

When an API evolves, clients on different versions must be served correctly. This workflow resolves the requested API version (mapping aliases like 'latest' to 'v2'), routes to the correct versioned handler (v1 or v2), and logs the version usage for deprecation tracking.

Without orchestration, version routing is embedded in API gateway config or application code with if/else branches. Tracking which clients are still using deprecated versions requires parsing access logs, and adding a v3 means modifying the routing logic.

## The Solution

**You just write the version-resolver, versioned API handlers, and usage-logging workers. Conductor handles version-based routing via SWITCH, per-version retries, and usage analytics for deprecation decisions.**

Each worker represents a service boundary. Conductor manages cross-service orchestration, compensating transactions, timeout enforcement, and distributed tracing .  your workers just make the service calls.

### What You Write: Workers

Four workers manage API versioning: ResolveVersionWorker maps aliases like 'latest' to a concrete version, CallV1Worker and CallV2Worker serve their respective APIs, and LogVersionUsageWorker tracks usage for deprecation planning.

| Worker | Task | What It Does |
|---|---|---|
| **CallV1Worker** | `sv_call_v1` | Handles requests using the v1 (legacy) API handler. |
| **CallV2Worker** | `sv_call_v2` | Handles requests using the v2 (current) API handler. |
| **LogVersionUsageWorker** | `sv_log_version_usage` | Logs which API version was requested and resolved, for deprecation tracking. |
| **ResolveVersionWorker** | `sv_resolve_version` | Resolves the requested API version to a concrete version (e.g., 'latest' -> 'v2') and flags deprecated versions. |

Workers simulate service calls with realistic request/response shapes so you can see the coordination pattern without running the full service mesh. Replace with real HTTP clients .  the workflow coordination stays the same.

### The Workflow

```
sv_resolve_version
    │
    ▼
SWITCH (route_ref)
    ├── v2: sv_call_v2
    └── default: sv_call_v1
    │
    ▼
sv_log_version_usage
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
java -jar target/service-versioning-1.0.0.jar
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
java -jar target/service-versioning-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow service_versioning_workflow \
  --version 1 \
  --input '{"apiVersion": "test-value", "request": "test-value"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w service_versioning_workflow -s COMPLETED -c 5
```

## How to Extend

Connect each worker to your real version-resolution logic, versioned service endpoints, and analytics pipeline, the resolve-route-log version management workflow stays exactly the same.

- **CallV1Worker** (`sv_call_v1`): route to the real v1 service endpoint or legacy code path
- **CallV2Worker** (`sv_call_v2`): route to the real v2 service endpoint or current code path
- **LogVersionUsageWorker** (`sv_log_version_usage`): publish version-usage events to your analytics pipeline for deprecation dashboards

Adding a v3 handler only requires a new worker and a SWITCH branch. Existing version handlers remain untouched.

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
service-versioning/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/serviceversioning/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ServiceVersioningExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CallV1Worker.java
│       ├── CallV2Worker.java
│       ├── LogVersionUsageWorker.java
│       └── ResolveVersionWorker.java
└── src/test/java/serviceversioning/workers/
    ├── CallV1WorkerTest.java        # 2 tests
    ├── CallV2WorkerTest.java        # 2 tests
    ├── LogVersionUsageWorkerTest.java        # 2 tests
    └── ResolveVersionWorkerTest.java        # 2 tests
```
