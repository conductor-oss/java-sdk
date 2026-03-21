# API Gateway Routing in Java with Conductor

API gateway routing workflow that authenticates requests, checks rate limits, routes to backend services, and transforms responses. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## The Problem

An API gateway must authenticate every inbound request, enforce rate limits per client, route the call to the right backend service, and transform the response before returning it. Each of these concerns lives in a different service, and they must run in strict sequence. Routing depends on auth, and the response transform depends on the routing result.

Without orchestration, you end up hard-coding the call chain inside a single gateway class, manually threading client IDs between steps, and writing retry/timeout logic around each HTTP call. Any change to the pipeline (adding a logging step, rearranging rate-check order) forces a redeploy of the whole gateway.

## The Solution

**You just write the auth, rate-check, routing, and response-transform workers. Conductor handles ordered execution, automatic retries on transient failures, and per-request observability.**

Each worker represents a service boundary. Conductor manages cross-service orchestration, compensating transactions, timeout enforcement, and distributed tracing. your workers just make the service calls.

### What You Write: Workers

Four workers divide the routing pipeline: AuthenticateWorker validates tokens, RateCheckWorker enforces per-client limits, RouteRequestWorker forwards to backends, and TransformResponseWorker shapes the reply for the caller.

| Worker | Task | What It Does |
|---|---|---|
| **AuthenticateWorker** | `gw_authenticate` | Validates authentication tokens for incoming API requests. |
| **RateCheckWorker** | `gw_rate_check` | Checks rate limits for a client. |
| **RouteRequestWorker** | `gw_route_request` | Routes a request to the appropriate backend service. |
| **TransformResponseWorker** | `gw_transform_response` | Transforms a service response for the client version. |

Workers implement service calls with realistic request/response shapes so you can see the coordination pattern without running the full service mesh. Replace with real HTTP clients. the workflow coordination stays the same.

### The Workflow

```
gw_authenticate
    │
    ▼
gw_rate_check
    │
    ▼
gw_route_request
    │
    ▼
gw_transform_response

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
java -jar target/api-gateway-routing-1.0.0.jar

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
java -jar target/api-gateway-routing-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow api_gateway_routing \
  --version 1 \
  --input '{"path": "sample-path", "method": "sample-method", "headers": "sample-headers", "body": "sample-body"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w api_gateway_routing -s COMPLETED -c 5

```

## How to Extend

Point each worker at your real auth provider (Auth0, Cognito), Redis-backed rate limiter, and backend services, the routing pipeline workflow stays exactly the same.

- **AuthenticateWorker** (`gw_authenticate`): validate JWTs against your identity provider (Auth0, Keycloak, AWS Cognito)
- **RateCheckWorker** (`gw_rate_check`): query a Redis-backed sliding-window counter or a token-bucket service
- **RouteRequestWorker** (`gw_route_request`): make the real HTTP call to your backend microservice via service discovery

Swapping in real auth, rate-limiting, or routing backends requires no workflow modifications, the contract between steps is preserved.

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
api-gateway-routing/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/apigatewayrouting/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ApiGatewayRoutingExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AuthenticateWorker.java
│       ├── RateCheckWorker.java
│       ├── RouteRequestWorker.java
│       └── TransformResponseWorker.java
└── src/test/java/apigatewayrouting/workers/
    ├── AuthenticateWorkerTest.java        # 7 tests
    ├── RateCheckWorkerTest.java        # 8 tests
    ├── RouteRequestWorkerTest.java        # 8 tests
    └── TransformResponseWorkerTest.java        # 8 tests

```
