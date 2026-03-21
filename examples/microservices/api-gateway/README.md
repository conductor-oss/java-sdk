# API Gateway in Java with Conductor

A mobile client hits your API. The request needs to be authenticated, routed to the right backend, and the response transformed before it goes back. You built this as a single Spring controller: authenticate inline, make the backend call, transform the response, return. Then the auth service starts taking 2 seconds instead of 200ms. Your entire gateway blocks. You add a retry loop for the backend call, but now a transient auth failure retries the backend too. A customer reports getting someone else's response, the response-transform step was reading from a shared variable that another thread overwrote. Every fix makes the controller harder to reason about, and when something fails, you have no idea which of the four steps caused it. This workflow breaks the gateway into independent, observable steps, authenticate, route, transform, respond, each with its own retries and timeouts. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers, you write the business logic, Conductor handles retries, failure routing, durability, and observability.

## The Problem

An API gateway sits between external clients and your backend services. Every request must be authenticated, routed to the correct service, have its response transformed into a client-friendly format, and be sent back with appropriate headers. These steps are inherently sequential, routing depends on the authenticated client identity, and the response transform depends on the routing result.

Without orchestration, you wire authenticate-route-transform-respond into a single class with nested try/catch blocks, bespoke retry loops for each outbound call, and no visibility into where a request failed. Adding a new step (rate limiting, logging, caching) means rewriting the entire pipeline.

## The Solution

**You just write the authentication, routing, and response-transform workers. Conductor handles sequential execution, per-step retries, and full request tracing across the gateway pipeline.**

Each worker represents a service boundary. Conductor manages cross-service orchestration, compensating transactions, timeout enforcement, and distributed tracing. Your workers just make the service calls.

### What You Write: Workers

The gateway pipeline breaks into four focused workers: authenticating API keys, routing to backends, transforming responses, and sending the final reply, each handling one hop in the request lifecycle.

| Worker | Task | What It Does |
|---|---|---|
| **AgAuthenticateWorker** | `ag_authenticate` | Validates an API key and returns client info. |
| **RouteRequestWorker** | `ag_route_request` | Routes the API request to the appropriate backend service. |
| **SendResponseWorker** | `ag_send_response` | Sends the final API response to the client. |
| **TransformResponseWorker** | `ag_transform_response` | Transforms the raw backend response into a client-friendly format. |

Workers implement service calls with realistic request/response shapes so you can see the coordination pattern without running the full service mesh. Replace with real HTTP clients, the workflow coordination stays the same.

### The Workflow

```
ag_authenticate
    │
    ▼
ag_route_request
    │
    ▼
ag_transform_response
    │
    ▼
ag_send_response

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
java -jar target/api-gateway-1.0.0.jar

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
java -jar target/api-gateway-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow api_gateway_292 \
  --version 1 \
  --input '{"apiKey": "ak-premium-xyz789", "endpoint": "/api/v1/users", "method": "GET", "payload": {}}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w api_gateway_292 -s COMPLETED -c 5

```

## How to Extend

Replace each simulated worker with a call to your identity provider, service mesh, or response-formatting layer, the authenticate-route-transform-respond workflow stays exactly the same.

- **AgAuthenticateWorker** (`ag_authenticate`): validate API keys or JWTs against your identity provider or API-key store
- **RouteRequestWorker** (`ag_route_request`): make a real HTTP call to the target backend service via service discovery or a load balancer
- **SendResponseWorker** (`ag_send_response`): write the response to the actual HTTP connection (e.g., via Spring WebFlux or Netty)

As long as each worker accepts the same inputs and returns the same fields, the gateway workflow needs zero changes.

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
api-gateway/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/apigateway/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ApiGatewayExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AgAuthenticateWorker.java
│       ├── RouteRequestWorker.java
│       ├── SendResponseWorker.java
│       └── TransformResponseWorker.java
└── src/test/java/apigateway/workers/
    ├── AgAuthenticateWorkerTest.java        # 8 tests
    ├── RouteRequestWorkerTest.java        # 8 tests
    ├── SendResponseWorkerTest.java        # 8 tests
    └── TransformResponseWorkerTest.java        # 8 tests

```
