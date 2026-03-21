# Service Registry in Java with Conductor

Service registry workflow that registers a service, performs a health check, and discovers the service endpoint. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## The Problem

When a new service instance starts up, it must register itself with a service registry so other services can discover it. After registration, a health check confirms the instance is ready to receive traffic, and then the registry can provide its endpoint to callers.

Without orchestration, registration and health checks are handled by client libraries (Eureka client, Consul agent) with no centralized view of the registration lifecycle. If a health check fails after registration, the instance may still receive traffic from stale discovery cache.

## The Solution

**You just write the registration, health-check, and discovery workers. Conductor handles registration sequencing, health-check retries, and a durable record of every registration lifecycle.**

Each worker represents a service boundary. Conductor manages cross-service orchestration, compensating transactions, timeout enforcement, and distributed tracing. your workers just make the service calls.

### What You Write: Workers

Three workers manage the registration lifecycle: RegisterServiceWorker adds the instance to the registry, HealthCheckWorker confirms it is ready for traffic, and DiscoverServiceWorker retrieves its endpoint for callers.

| Worker | Task | What It Does |
|---|---|---|
| **DiscoverServiceWorker** | `sr_discover_service` | Discovers a service's endpoint from the registry. |
| **HealthCheckWorker** | `sr_health_check` | Performs a health check on a registered service. |
| **RegisterServiceWorker** | `sr_register_service` | Registers a service in the service registry. |

Workers implement service calls with realistic request/response shapes so you can see the coordination pattern without running the full service mesh. Replace with real HTTP clients. the workflow coordination stays the same.

### The Workflow

```
sr_register_service
    │
    ▼
sr_health_check
    │
    ▼
sr_discover_service

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
java -jar target/service-registry-1.0.0.jar

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
java -jar target/service-registry-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow service_registry_workflow \
  --version 1 \
  --input '{"serviceName": "test", "serviceUrl": "https://example.com", "version": "1.0"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w service_registry_workflow -s COMPLETED -c 5

```

## How to Extend

Point each worker at your real service registry (Consul, Eureka, Kubernetes) and health endpoint, the register-check-discover lifecycle workflow stays exactly the same.

- **DiscoverServiceWorker** (`sr_discover_service`): query Consul, Eureka, or Kubernetes Service endpoints for real instance discovery
- **HealthCheckWorker** (`sr_health_check`): make a real HTTP call to the service's /health endpoint (e.g., Spring Boot Actuator)
- **RegisterServiceWorker** (`sr_register_service`): register with Consul, Eureka, or Kubernetes Service via its API

Migrating from a demo registry to Consul or Eureka preserves the register-check-discover workflow.

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
service-registry/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/serviceregistry/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ServiceRegistryExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── DiscoverServiceWorker.java
│       ├── HealthCheckWorker.java
│       └── RegisterServiceWorker.java
└── src/test/java/serviceregistry/workers/
    ├── DiscoverServiceWorkerTest.java        # 8 tests
    ├── HealthCheckWorkerTest.java        # 7 tests
    └── RegisterServiceWorkerTest.java        # 8 tests

```
