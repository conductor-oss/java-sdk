# Service Discovery in Java with Conductor

Discover services, select instance, call with failover. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers .  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## The Problem

In a dynamic microservice environment, service instances come and go. Calling a service requires discovering available instances from a registry, selecting the best one (e.g., least connections, healthiest), making the call, and handling failover if the selected instance is down.

Without orchestration, service discovery is embedded in each client using libraries like Ribbon or Eureka client, with retry/failover logic duplicated across every calling service. There is no centralized view of which instance was selected, why, and what happened when it failed.

## The Solution

**You just write the discovery, instance-selection, service-call, and failover workers. Conductor handles discovery-to-call sequencing, per-call timeout enforcement, and a full record of instance selection decisions.**

Each worker represents a service boundary. Conductor manages cross-service orchestration, compensating transactions, timeout enforcement, and distributed tracing .  your workers just make the service calls.

### What You Write: Workers

Four workers handle dynamic service resolution: DiscoverServicesWorker queries the registry, SelectInstanceWorker picks the best candidate, CallServiceWorker makes the request, and HandleFailoverWorker retries on a different instance if needed.

| Worker | Task | What It Does |
|---|---|---|
| **CallServiceWorker** | `sd_call_service` | Calls the selected service instance. |
| **DiscoverServicesWorker** | `sd_discover_services` | Discovers service instances from a registry. |
| **HandleFailoverWorker** | `sd_handle_failover` | Handles failover if the service call failed. |
| **SelectInstanceWorker** | `sd_select_instance` | Selects the best service instance based on strategy (least-connections). |

Workers simulate service calls with realistic request/response shapes so you can see the coordination pattern without running the full service mesh. Replace with real HTTP clients .  the workflow coordination stays the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
sd_discover_services
    │
    ▼
sd_select_instance
    │
    ▼
sd_call_service
    │
    ▼
sd_handle_failover
```

## Example Output

```
=== Service Discovery Demo ===

Step 1: Registering task definitions...
  Registered: sd_discover_services, sd_select_instance, sd_call_service, sd_handle_failover

Step 2: Registering workflow 'service_discovery_293'...
  Workflow registered.

Step 3: Starting workers...
  4 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [sd_call_service] Calling
  [sd_discover_services] Looking up instances for \"" + serviceName + "\"...
  [sd_handle_failover] Call
  [sd_select_instance] Selected instance

  Status: COMPLETED
  Output: {response=..., latency=..., success=..., instances=...}

Result: PASSED
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
java -jar target/service-discovery-1.0.0.jar
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
java -jar target/service-discovery-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow service_discovery_293 \
  --version 1 \
  --input '{"serviceName": "order-service", "order-service": "request", "request": {"key": "value"}}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w service_discovery_293 -s COMPLETED -c 5
```

## How to Extend

Connect each worker to your real service registry (Consul, Eureka, Kubernetes), load-balancing strategy, and failover logic, the discover-select-call-failover workflow stays exactly the same.

- **CallServiceWorker** (`sd_call_service`): make real HTTP/gRPC calls to the selected instance
- **DiscoverServicesWorker** (`sd_discover_services`): query Consul, Eureka, or Kubernetes Service endpoints for real instance data
- **HandleFailoverWorker** (`sd_handle_failover`): retry the call on a different healthy instance from the discovery list, or return a circuit-breaker fallback

Migrating from Eureka to Consul or Kubernetes service endpoints requires no changes to the discover-select-call-failover flow.

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
service-discovery/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/servicediscovery/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ServiceDiscoveryExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CallServiceWorker.java
│       ├── DiscoverServicesWorker.java
│       ├── HandleFailoverWorker.java
│       └── SelectInstanceWorker.java
└── src/test/java/servicediscovery/workers/
    ├── CallServiceWorkerTest.java        # 8 tests
    ├── DiscoverServicesWorkerTest.java        # 8 tests
    ├── HandleFailoverWorkerTest.java        # 8 tests
    └── SelectInstanceWorkerTest.java        # 8 tests
```
