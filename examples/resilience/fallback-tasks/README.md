# Implementing Fallback Tasks in Java with Conductor :  Primary API, Secondary API, and Cache Lookup

A Java Conductor workflow example demonstrating tiered fallback .  trying a primary API first, falling back to a secondary API if the primary is unavailable, and ultimately serving from cache if both APIs are down.

## The Problem

You depend on an external API for critical data, but it's not always available. You need a tiered fallback strategy: try the primary API first, fall back to a secondary API (different provider, different region) if the primary is down, and serve cached/stale data as a last resort. The user should always get a response .  the quality may degrade, but the system never returns an error.

Without orchestration, fallback logic nests into deeply indented try/catch chains. The secondary API call is buried inside the primary's catch block, and the cache lookup is inside the secondary's catch block. Each layer adds complexity, and it's impossible to tell at a glance which data source actually served a given request.

## The Solution

**You just write the primary, secondary, and cache lookup logic. Conductor handles SWITCH-based fallback routing through the tiered chain, retries at each level, and a record of every request showing which data source ultimately served the response.**

The primary API worker makes the call. Based on its result, Conductor's SWITCH task routes to either the response path (primary succeeded), the secondary API (primary failed), or the cache lookup (both failed). Each fallback level is a simple, independent worker. Every request is tracked .  you can see which data source served each response and how far down the fallback chain it went. You get all of that for free, without writing a single line of orchestration code.

### What You Write: Workers

PrimaryApiWorker tries the preferred data source first, SecondaryApiWorker serves as the backup provider if the primary is unavailable, and CacheLookupWorker delivers stale-but-valid cached data as the last resort.

| Worker | Task | What It Does |
|---|---|---|
| **CacheLookupWorker** | `fb_cache_lookup` | Cache lookup worker (fb_cache_lookup). Last-resort fallback using cached data. Always succeeds. Returns: source = ".. |
| **PrimaryApiWorker** | `fb_primary_api` | Primary API worker (fb_primary_api). Calls the primary data source. |
| **SecondaryApiWorker** | `fb_secondary_api` | Secondary API worker (fb_secondary_api). Fallback API data source. Always succeeds. Returns: source = "secondary" d.. |

Workers simulate success and failure scenarios so you can observe the resilience pattern end-to-end. Swap in real service calls and the retry, compensation, and recovery behavior works identically.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |
| **Conditional routing** | SWITCH tasks route execution to different paths based on worker output |

### The Workflow

```
fb_primary_api
    │
    ▼
SWITCH (fallback_switch_ref)
    ├── unavailable: fb_secondary_api
    ├── error: fb_cache_lookup
```

## Example Output

```
=== Fallback Tasks Demo: Alternative Paths on Failure ===

Step 1: Registering task definitions...
  Registered: ...

Step 2: Registering workflow 'fallback_tasks_demo'...
  Workflow registered.

Step 3: Starting workers...
  3 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [cache_lookup] Processing
  [primary_api] Processing
  [secondary_api] Processing

  Status: COMPLETED
  Output: {source=..., data=..., stale=..., apiStatus=...}

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
java -jar target/fallback-tasks-1.0.0.jar
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
java -jar target/fallback-tasks-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow fallback_tasks_demo \
  --version 1 \
  --input '{"available": "sample-available"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w fallback_tasks_demo -s COMPLETED -c 5
```

## How to Extend

Each worker calls one data source .  connect the primary worker to your main API, the secondary worker to a backup provider or different region, the cache worker to Redis or Memcached, and the primary-secondary-cache fallback chain stays the same.

- **CacheLookupWorker** (`fb_cache_lookup`): serve from Redis/Memcached/local cache .  stale data is better than no data for most read operations
- **PrimaryApiWorker** (`fb_primary_api`): call your primary data provider (production database, main API endpoint, preferred vendor)
- **SecondaryApiWorker** (`fb_secondary_api`): call your backup data source (read replica, secondary vendor, different AWS region)

Point each tier at your real API providers and cache layer, and the primary-secondary-cache fallback chain operates in production with no orchestration changes.

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
fallback-tasks/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/fallbacktasks/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── FallbackTasksExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CacheLookupWorker.java
│       ├── PrimaryApiWorker.java
│       └── SecondaryApiWorker.java
└── src/test/java/fallbacktasks/workers/
    └── FallbackWorkersTest.java        # 12 tests
```
