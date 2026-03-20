# Multi Tenancy in Java with Conductor

Tenant-isolated workflows with per-tenant routing. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers .  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## The Problem

In a multi-tenant system, each request must be routed to the correct tenant context, processed according to the tenant's tier (which determines rate limits, features, and resource allocation), and logged for per-tenant billing. The tenant's tier affects which resources and SLAs apply.

Without orchestration, tenant isolation logic is scattered across middleware and service code. Changing a tenant's tier or adding a new billing rule requires modifying multiple services, and there is no centralized audit trail of per-tenant usage.

## The Solution

**You just write the tenant-resolution, request-processing, and usage-logging workers. Conductor handles per-tenant routing, usage tracking across every request, and durable execution state per tenant.**

Each worker represents a service boundary. Conductor manages cross-service orchestration, compensating transactions, timeout enforcement, and distributed tracing .  your workers just make the service calls.

### What You Write: Workers

Three workers isolate tenant concerns: ResolveTenantWorker maps a tenant ID to its configuration and tier, ProcessRequestWorker applies tier-appropriate resources, and LogUsageWorker records consumption for billing.

| Worker | Task | What It Does |
|---|---|---|
| **LogUsageWorker** | `mt_log_usage` | Logs the tenant's usage (action and cost) for billing and audit purposes. |
| **ProcessRequestWorker** | `mt_process_request` | Processes the tenant's request using tier-appropriate resources and returns the result with cost. |
| **ResolveTenantWorker** | `mt_resolve_tenant` | Resolves a tenant ID to its configuration: tier (enterprise/standard), region, and isolation level. |

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
mt_resolve_tenant
    │
    ▼
mt_process_request
    │
    ▼
mt_log_usage
```

## Example Output

```
=== Example 319: Multi-Tenancy ===

Step 1: Registering task definitions...
  Registered: mt_resolve_tenant, mt_process_request, mt_log_usage

Step 2: Registering workflow 'multi_tenancy_workflow'...
  Workflow registered.

Step 3: Starting workers...
  3 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [usage] Logged:
  [process] Tenant
  [tenant] Resolved

  Status: COMPLETED
  Output: {logged=..., result=..., cost=..., tier=...}

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
java -jar target/multi-tenancy-1.0.0.jar
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
java -jar target/multi-tenancy-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow multi_tenancy_workflow \
  --version 1 \
  --input '{"tenantId": "tenant-acme", "tenant-acme": "action", "action": "generate-report", "generate-report": "data", "data": {"key": "value"}}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w multi_tenancy_workflow -s COMPLETED -c 5
```

## How to Extend

Connect each worker to your real tenant-management database, tier-appropriate infrastructure, and billing system (Stripe Metering), the resolve-process-log workflow stays exactly the same.

- **LogUsageWorker** (`mt_log_usage`): write usage records to your billing system (Stripe Metering, custom usage table, data warehouse)
- **ProcessRequestWorker** (`mt_process_request`): route to tier-appropriate infrastructure (dedicated vs shared clusters, premium vs standard queues)
- **ResolveTenantWorker** (`mt_resolve_tenant`): look up the tenant in your tenant-management database or identity provider

Connecting ResolveTenantWorker to a real tenant database or LogUsageWorker to Stripe Metering preserves the resolve-process-log pipeline.

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
multi-tenancy/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/multitenancy/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── MultiTenancyExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── LogUsageWorker.java
│       ├── ProcessRequestWorker.java
│       └── ResolveTenantWorker.java
└── src/test/java/multitenancy/workers/
    ├── LogUsageWorkerTest.java        # 2 tests
    ├── ProcessRequestWorkerTest.java        # 2 tests
    └── ResolveTenantWorkerTest.java        # 2 tests
```
