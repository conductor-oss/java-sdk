# Implementing Circuit Breaker Pattern in Java with Conductor: Prevent Cascading Failures with State-Based Routing

A Java Conductor workflow example implementing the circuit breaker pattern. Checking the circuit state based on recent failure counts, routing to the real service call when the circuit is CLOSED, or returning cached/fallback data when the circuit is OPEN to prevent cascading failures.

## The Problem

You have a service that depends on an unreliable downstream dependency, a payment API, a third-party geocoding service, an internal inventory microservice. When that dependency starts failing, every request continues to hit it, making things worse (thundering herd), and each request waits for a timeout before failing (degrading your own response times). Meanwhile, the downstream service is overwhelmed and cannot recover.

### The Circuit Breaker State Machine

The circuit breaker pattern tracks failure counts and transitions between three states:

```
           success
     +------------------+
     |                  |
     v    failureCount  |    failureCount
  CLOSED  ------------> OPEN  <-- stays OPEN until cooldown
     ^   >= threshold   |
     |                  | cooldown expires
     |                  v
     +--- HALF_OPEN ----+
          (test one       failure -> back to OPEN
           request)       success -> back to CLOSED
```

- **CLOSED**: Normal operation. Requests go through to the real service. Failures are counted.
- **OPEN**: Too many failures. Requests are immediately routed to fallback (cached data, default values) without touching the failing service. This gives the downstream service time to recover.
- **HALF_OPEN**: After a cooldown period, one test request is sent to the real service. If it succeeds, the circuit closes. If it fails, the circuit opens again.

Without orchestration, implementing circuit breakers means embedding state management, threshold logic, and fallback routing into every service that calls a dependency. Each implementation is slightly different, the failure thresholds are hardcoded, and nobody can see which circuits are open across the system.

## The Solution

**You just write the service call and fallback logic. Conductor handles SWITCH-based state routing between OPEN and CLOSED paths, retries on the service call, and visibility into every circuit evaluation showing which state was active and whether the live or fallback path was taken.**

Each circuit breaker concern is a simple, independent worker. One evaluates the circuit state from failure count and threshold, one makes the actual service call, one returns fallback data. Conductor's SWITCH task handles the routing: when the circuit is OPEN, it skips the service call entirely and routes to the fallback path. Every circuit evaluation is tracked with inputs, outputs, and timing, giving you full visibility into which circuits tripped and when. You get all of that for free, without writing a single line of orchestration code.

### What You Write: Workers

CheckCircuitWorker evaluates the circuit state from failure counts and thresholds, CallServiceWorker makes the live service call when the circuit is CLOSED, and FallbackWorker returns cached data when the circuit is OPEN to protect the failing dependency.

| Worker | Task | What It Does | Real / Simulated |
|---|---|---|---|
| **CheckCircuitWorker** | `cb_check_circuit` | Evaluates the circuit breaker state. If `circuitState` is "OPEN" or "HALF_OPEN", returns that state directly (manual override). Otherwise, compares `failureCount` against `threshold`. Returns "OPEN" if failures >= threshold, "CLOSED" otherwise. Defaults: failureCount=0, threshold=3. | Simulated |
| **CallServiceWorker** | `cb_call_service` | Makes the real service call. Called when the circuit is CLOSED or HALF_OPEN. Returns `{result: "Service payment-api responded successfully", source: "live"}`. | Simulated |
| **FallbackWorker** | `cb_fallback` | Returns cached/fallback data. Called when the circuit is OPEN. Returns `{result: "Fallback data for payment-api", source: "cache"}`. | Simulated |

Workers simulate success and failure scenarios so you can observe the resilience pattern end-to-end. Swap in real service calls and the retry, compensation, and recovery behavior works identically.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically. Configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status.; no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |
| **Conditional routing** | SWITCH tasks route execution to different paths based on worker output |

### The Workflow

```
cb_check_circuit
    |
    v
SWITCH (circuit_switch_ref) on state:
    |-- "OPEN":    cb_fallback  --> returns cached data (source: "cache")
    |-- default:   cb_call_service --> returns live data (source: "live")
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
java -jar target/circuit-breaker-1.0.0.jar
```

### Option 3: Use the run script

```bash
./run.sh

# Or on a custom port:
CONDUCTOR_PORT=9090 ./run.sh

# Or pointing at an existing Conductor:
CONDUCTOR_BASE_URL=http://localhost:9090/api ./run.sh
```

## Example Output

```
=== Circuit Breaker Pattern: Stop Calling Failed Services ===

Step 1: Registering task definitions...
  Registered: cb_check_circuit, cb_call_service, cb_fallback

Step 2: Registering workflow 'circuit_breaker_demo'...
  Workflow registered.

Step 3: Starting workers...
  3 workers polling.

Step 4: Scenario A. Circuit CLOSED (normal, calls go through)...

  Workflow ID: f3a1b2c4-...
  [cb_check_circuit] circuitState=null failureCount=0 threshold=3 => CLOSED
  [cb_call_service] Calling service: payment-api
  Status: COMPLETED
  Output: {circuitState=CLOSED, result=Service payment-api responded successfully, source=live}

Step 5: Scenario B. Circuit OPEN (too many failures, use fallback)...

  Workflow ID: d7e8f9a0-...
  [cb_check_circuit] circuitState=null failureCount=5 threshold=3 => OPEN
  [cb_fallback] Returning fallback data for service: payment-api
  Status: COMPLETED
  Output: {circuitState=OPEN, result=Fallback data for payment-api, source=cache}

Step 6: Scenario C. Circuit forced OPEN (manual override)...

  Workflow ID: b5c6d7e8-...
  [cb_check_circuit] circuitState=OPEN failureCount=0 threshold=3 => OPEN
  [cb_fallback] Returning fallback data for service: payment-api
  Status: COMPLETED
  Output: {circuitState=OPEN, result=Fallback data for payment-api, source=cache}

Result: PASSED
```

## Configuration

| Environment Variable | Default | Description |
|---|---|---|
| `CONDUCTOR_BASE_URL` | `http://localhost:8080/api` | Conductor server URL |
| `CONDUCTOR_PORT` | `8080` | Host port for Conductor (Docker Compose only) |

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/circuit-breaker-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
# Circuit CLOSED: 0 failures, threshold 3, service call goes through
conductor workflow start \
  --workflow circuit_breaker_demo \
  --version 1 \
  --input '{"failureCount": 0, "threshold": 3, "serviceName": "payment-api"}'

# Circuit OPEN: 5 failures exceed threshold of 3, fallback returned
conductor workflow start \
  --workflow circuit_breaker_demo \
  --version 1 \
  --input '{"failureCount": 5, "threshold": 3, "serviceName": "payment-api"}'

# Circuit forced OPEN: manual kill switch, regardless of failure count
conductor workflow start \
  --workflow circuit_breaker_demo \
  --version 1 \
  --input '{"circuitState": "OPEN", "serviceName": "payment-api"}'

# Circuit HALF_OPEN: testing if service has recovered
conductor workflow start \
  --workflow circuit_breaker_demo \
  --version 1 \
  --input '{"circuitState": "HALF_OPEN", "serviceName": "inventory-api"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w circuit_breaker_demo -s COMPLETED -c 5
```

## How to Extend

Each worker handles one circuit breaker concern. Connect the service call worker to your real downstream API (payment gateway, geocoding service), the fallback worker to serve cached data, and the state-check-route-respond workflow stays the same.

- **CheckCircuitWorker** (`cb_check_circuit`): back with Redis or DynamoDB to persist failure counts across invocations and support HALF_OPEN state with time-based cooldowns
- **CallServiceWorker** (`cb_call_service`): replace with your real HTTP/gRPC service call, record success/failure to update the circuit state for the next invocation
- **FallbackWorker** (`cb_fallback`): return cached data from Redis/Memcached, serve stale values from a read replica, or return a graceful degraded response
- **Add HALF_OPEN routing**: extend the SWITCH task with a HALF_OPEN case that sends a single test request and transitions back to CLOSED on success or OPEN on failure

Wire the service call worker to your real downstream API and the fallback to your cache layer, and the state-based circuit breaker routing works without any orchestration changes.

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
circuit-breaker/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/circuitbreaker/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── CircuitBreakerExample.java   # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CallServiceWorker.java
│       ├── CheckCircuitWorker.java
│       └── FallbackWorker.java
└── src/test/java/circuitbreaker/workers/
    ├── CallServiceWorkerTest.java   # 4 tests
    ├── CheckCircuitWorkerTest.java  # 12 tests
    └── FallbackWorkerTest.java      # 4 tests
```
