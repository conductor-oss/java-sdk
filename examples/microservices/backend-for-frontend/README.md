# Backend For Frontend in Java with Conductor

Backend for Frontend pattern with platform-specific responses. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## The Problem

Different client platforms (web, mobile, TV) need different response shapes from the same backend data. A web dashboard can display a full user profile with all fields, while a mobile app needs a compact summary to conserve bandwidth. The BFF pattern solves this by fetching shared data once and then branching into platform-specific transformers.

Without orchestration, each platform ends up with its own ad-hoc endpoint that duplicates the data-fetching logic, or a single endpoint bloats with if/else branches for every client type. Adding a new platform (e.g., smart TV) means touching existing code paths and risking regressions.

## The Solution

**You just write the data-fetch and platform-transform workers. Conductor handles conditional platform routing, retry on data-fetch failures, and per-request tracing.**

Each worker represents a service boundary. Conductor manages cross-service orchestration, compensating transactions, timeout enforcement, and distributed tracing. your workers just make the service calls.

### What You Write: Workers

FetchDataWorker loads shared user data once, then TransformWebWorker and TransformMobileWorker each reshape it for their target platform. Web gets the full dataset, mobile gets a compressed payload.

| Worker | Task | What It Does |
|---|---|---|
| **FetchDataWorker** | `bff_fetch_data` | Loads the user's profile, order count, and notification count from backend services. |
| **TransformMobileWorker** | `bff_transform_mobile` | Compresses the fetched data into a compact payload optimized for small screens and low bandwidth. |
| **TransformWebWorker** | `bff_transform_web` | Returns the full dataset with all fields for rich desktop rendering. |

Workers implement service calls with realistic request/response shapes so you can see the coordination pattern without running the full service mesh. Replace with real HTTP clients. the workflow coordination stays the same.

### The Workflow

```
bff_fetch_data
    │
    ▼
SWITCH (switch_ref)
    ├── web: bff_transform_web
    ├── mobile: bff_transform_mobile
    └── default: bff_transform_web

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
java -jar target/backend-for-frontend-1.0.0.jar

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
java -jar target/backend-for-frontend-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow bff_workflow \
  --version 1 \
  --input '{"userId": "TEST-001", "platform": "sample-platform"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w bff_workflow -s COMPLETED -c 5

```

## How to Extend

Connect the fetch worker to your real user-profile and orders APIs, and wire each transform worker to your platform-specific formatting logic, the BFF workflow stays exactly the same.

- **FetchDataWorker** (`bff_fetch_data`): call your user-profile, orders, and notifications microservices via HTTP or gRPC
- **TransformMobileWorker** (`bff_transform_mobile`): apply real mobile-specific field filtering, image resizing, and payload compression
- **TransformWebWorker** (`bff_transform_web`): enrich the response with desktop-specific widgets, analytics tags, or server-rendered HTML fragments

Each platform transformer can be rewritten independently without touching the shared data-fetch step or the workflow definition.

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
backend-for-frontend/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/backendforfrontend/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── BackendForFrontendExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── FetchDataWorker.java
│       ├── TransformMobileWorker.java
│       └── TransformWebWorker.java
└── src/test/java/backendforfrontend/workers/
    ├── FetchDataWorkerTest.java        # 2 tests
    ├── TransformMobileWorkerTest.java        # 2 tests
    └── TransformWebWorkerTest.java        # 2 tests

```
