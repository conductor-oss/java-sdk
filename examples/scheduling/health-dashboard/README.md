# Health Dashboard in Java Using Conductor :  Parallel Service Health Checks and Dashboard Rendering

A Java Conductor workflow example for building a health dashboard .  checking the health of API servers, databases, and caches in parallel via FORK/JOIN, then rendering a unified dashboard from the collected status data.

## The Problem

You need a health dashboard that shows the status of every component. API servers, databases, caches .  at a glance. Each health check is independent and can run in parallel for speed (don't wait for the database check to finish before checking the cache). The results must be aggregated into a single dashboard view showing green/yellow/red status for each component.

Without orchestration, health dashboards either poll each component from the browser (slow, client-heavy) or run a monolithic script that checks everything serially (slow, single point of failure). If one health check hangs, the entire dashboard is stale.

## The Solution

**You just write the health check endpoints and dashboard rendering. Conductor handles parallel health checks so one slow component does not block the others, retries on transient check failures, and per-component timing for every dashboard refresh.**

Conductor's FORK/JOIN runs API, database, and cache health checks in parallel. A render worker aggregates all results into a unified dashboard. If one check is slow, the others still complete quickly. Every dashboard refresh is tracked with per-component timing and status. You get all of that for free, without writing a single line of orchestration code.

### What You Write: Workers

Three health checkers run in parallel via FORK/JOIN. CheckApiWorker probes API servers, CheckDbWorker tests database connectivity, CheckCacheWorker verifies Redis/Memcached, then a RenderDashboardWorker aggregates results into a green/yellow/red status view.

| Worker | Task | What It Does |
|---|---|---|
| **CheckApiWorker** | `hd_check_api` | Checks API server health for a given environment, returning status and response time in milliseconds |
| **CheckCacheWorker** | `hd_check_cache` | Checks cache (Redis/Memcached) health for a given environment, returning status and response time |
| **CheckDbWorker** | `hd_check_db` | Checks database health for a given environment, returning status and response time |
| **RenderDashboardWorker** | `hd_render_dashboard` | Aggregates all component statuses into an overall health rating (GREEN/DEGRADED) and generates a dashboard URL |

Workers simulate scheduled operations with realistic outputs so you can see the scheduling pattern without external systems. Replace with real job logic .  the schedule triggers, retry behavior, and monitoring stay the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |
| **Parallel execution** | FORK_JOIN runs multiple tasks simultaneously and waits for all to complete |

### The Workflow

```
FORK_JOIN
    ├── hd_check_api
    ├── hd_check_db
    └── hd_check_cache
    │
    ▼
JOIN (wait for all branches)
hd_render_dashboard
```

## Example Output

```
=== Example 417: Health Dashboard ===

Step 1: Registering task definitions...
  Registered: hd_check_api, hd_check_db, hd_check_cache, hd_render_dashboard

Step 2: Registering workflow 'health_dashboard_417'...
  Workflow registered.

Step 3: Starting workers...
  4 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [api] Checking api health in
  [cache] Checking cache health in
  [database] Checking database health in
  [render] Rendering dashboard for

  Status: COMPLETED
  Output: {responseTimeMs=..., overallHealth=..., dashboardUrl=..., components=...}

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
java -jar target/health-dashboard-1.0.0.jar
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
java -jar target/health-dashboard-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow health_dashboard_417 \
  --version 1 \
  --input '{"environment": "sample-environment"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w health_dashboard_417 -s COMPLETED -c 5
```

## How to Extend

Each worker checks one component .  connect the API health checker to your service endpoints, the database checker to run real connection tests, and the parallel-check-then-render workflow stays the same.

- **CheckApiWorker** (`hd_check_api`): make real HTTP health check requests to your API endpoints and measure response times
- **CheckCacheWorker** (`hd_check_cache`): ping Redis/Memcached, check memory usage, measure hit/miss ratios, verify cluster health
- **CheckDbWorker** (`hd_check_db`): execute a lightweight query against your database (SELECT 1, connection pool stats) and check replication lag

Point health checkers at your real endpoints, and the parallel check-and-render dashboard pipeline works without any workflow modifications.

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
health-dashboard/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/healthdashboard/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── HealthDashboardExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CheckApiWorker.java
│       ├── CheckCacheWorker.java
│       ├── CheckDbWorker.java
│       └── RenderDashboardWorker.java
└── src/test/java/healthdashboard/workers/
    └── RenderDashboardWorkerTest.java        # 3 tests
```
