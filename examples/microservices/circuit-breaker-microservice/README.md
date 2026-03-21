# Circuit Breaker Microservice in Java with Conductor

Circuit breaker pattern for resilient service calls. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## The Problem

When a downstream service is failing, continuing to send requests wastes resources and can cascade failures to the caller. The circuit breaker pattern tracks failure counts and, when a threshold is exceeded, short-circuits requests to a fallback response until the downstream recovers. This workflow checks circuit state, routes to either a live call or a fallback, and records the result to update the circuit's failure counter.

Without orchestration, circuit breaker logic is embedded in each calling service using libraries like Resilience4j or Hystrix. There is no centralized view of which circuits are open, no audit trail of when circuits tripped, and changing thresholds requires redeploying each caller.

## The Solution

**You just write the circuit-check, service-call, fallback, and result-recording workers. Conductor handles conditional routing between call and fallback paths, failure tracking, and automatic retries.**

Each worker represents a service boundary. Conductor manages cross-service orchestration, compensating transactions, timeout enforcement, and distributed tracing .  your workers just make the service calls.

### What You Write: Workers

Four workers implement the circuit breaker: CheckCircuitWorker reads the circuit state, CallServiceWorker makes the downstream call when the circuit is closed, FallbackWorker returns a degraded response when it is open, and RecordResultWorker updates the failure counter.

| Worker | Task | What It Does |
|---|---|---|
| **CallServiceWorker** | `cb_call_service` | Makes the actual call to the downstream service when the circuit is closed. |
| **CheckCircuitWorker** | `cb_check_circuit` | Checks the circuit state (closed/open/half-open) for the target service and returns failure count and threshold. |
| **FallbackWorker** | `cb_fallback` | Returns a cached or degraded response when the circuit is open. |
| **RecordResultWorker** | `cb_record_result` | Records the call outcome (success/failure) to update the circuit's failure counter. |

Workers simulate service calls with realistic request/response shapes so you can see the coordination pattern without running the full service mesh. Replace with real HTTP clients .  the workflow coordination stays the same.

### The Workflow

```
cb_check_circuit
    │
    ▼
SWITCH (switch_ref)
    ├── open: cb_fallback
    └── default: cb_call_service -> cb_record_result

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
java -jar target/circuit-breaker-microservice-1.0.0.jar

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
java -jar target/circuit-breaker-microservice-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow circuit_breaker_workflow \
  --version 1 \
  --input '{"serviceName": "test", "request": "sample-request"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w circuit_breaker_workflow -s COMPLETED -c 5

```

## How to Extend

Point each worker at your real Redis circuit-state store, downstream HTTP/gRPC service, and cache layer, the check-call-or-fallback workflow stays exactly the same.

- **CallServiceWorker** (`cb_call_service`): make the real HTTP/gRPC call to your downstream service
- **CheckCircuitWorker** (`cb_check_circuit`): read circuit state from Redis or a shared state store to coordinate across multiple caller instances
- **FallbackWorker** (`cb_fallback`): return data from a cache (Redis, Memcached) or a static fallback payload

Moving the circuit state from an in-memory store to Redis has no effect on the check-call-or-fallback workflow.

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
circuit-breaker-microservice/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/circuitbreakermicroservice/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── CircuitBreakerMicroserviceExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CallServiceWorker.java
│       ├── CheckCircuitWorker.java
│       ├── FallbackWorker.java
│       └── RecordResultWorker.java
└── src/test/java/circuitbreakermicroservice/workers/
    ├── CallServiceWorkerTest.java        # 2 tests
    ├── CheckCircuitWorkerTest.java        # 2 tests
    ├── FallbackWorkerTest.java        # 2 tests
    └── RecordResultWorkerTest.java        # 2 tests

```
