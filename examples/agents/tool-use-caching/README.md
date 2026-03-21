# Tool Use Caching in Java Using Conductor :  Check Cache, Execute-or-Return-Cached, Store Results

Tool Use Caching .  checks a cache before executing a tool, and caches the result afterward. Uses a SWITCH task to branch on cache hit vs miss. Uses [Conductor](https://github.

## Tool Calls Are Expensive :  Don't Repeat Them

Tool calls cost time and often money. A web search API charges per query. A database query consumes compute resources. A calculation takes CPU time. If the same tool is called with the same arguments within a short window (same weather query, same stock lookup, same calculation), returning the cached result saves time and cost.

The caching pattern checks the cache before executing: if the tool name and arguments match a recent result (within the TTL), return it immediately. Otherwise, execute the tool, cache the result with the configured TTL, and return it. The `SWITCH` task makes this routing explicit .  cache hits skip execution entirely, and every request records whether it was served from cache or computed fresh.

## The Solution

**You write the cache lookup, tool execution, and cache storage logic. Conductor handles the hit/miss routing, TTL management, and cache hit-rate tracking.**

`CheckCacheWorker` looks up the tool name and arguments in the cache and returns hit/miss status with any cached result. Conductor's `SWITCH` routes on cache status: hits go to `ReturnCachedWorker` which formats and returns the cached result. Misses go to `ExecuteToolWorker` which runs the tool, then `CacheResultWorker` which stores the result with the specified TTL. Conductor records cache hit rates per tool, enabling you to tune TTL values based on actual usage patterns.

### What You Write: Workers

Four workers implement caching. Checking the cache for a prior result, routing cache hits to direct return, and routing misses through execution and storage.

| Worker | Task | What It Does |
|---|---|---|
| **CacheResultWorker** | `uc_cache_result` | Stores the tool execution result in the cache. Input fields: cacheKey, result, ttlSeconds. Output fields: storedResul... |
| **CheckCacheWorker** | `uc_check_cache` | Checks the cache for a previously computed tool result. Computes a cacheKey from toolName + toolArgs and simulates a ... |
| **ExecuteToolWorker** | `uc_execute_tool` | Executes the requested tool and returns its result. Simulates a currency conversion: converts an amount from one curr... |
| **ReturnCachedWorker** | `uc_return_cached` | Returns a previously cached result directly. Input fields: cachedResult, cacheKey, cachedAt. Output fields: result, f... |

Workers simulate agent decisions and tool calls with realistic outputs so you can see the routing and handoff patterns without live LLM calls. Add your API keys to switch to live mode .  the agent workflow stays the same.

### The Workflow

```
uc_check_cache
    │
    ▼
SWITCH (cache_decision_ref)
    ├── hit: uc_return_cached
    └── default: uc_execute_tool -> uc_cache_result

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
java -jar target/tool-use-caching-1.0.0.jar

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
java -jar target/tool-use-caching-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow tool_use_caching \
  --version 1 \
  --input '{"toolName": "test", "toolArgs": "sample-toolArgs", "cacheTtlSeconds": "sample-cacheTtlSeconds"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w tool_use_caching -s COMPLETED -c 5

```

## How to Extend

Each worker handles one caching concern. Integrate Redis for cache lookups with TTL, connect real APIs (currency exchange, weather) for tool execution, and the check-route-execute-store caching workflow runs unchanged.

- **CheckCacheWorker** (`uc_check_cache`): use Redis with `GET` by a hash of tool name + args, or implement semantic caching using embedding similarity for near-duplicate queries
- **CacheResultWorker** (`uc_cache_result`): store in Redis with `SET` and `EX` for TTL, or use DynamoDB with TTL attributes for serverless caching with automatic expiration
- **ExecuteToolWorker** (`uc_execute_tool`): implement cache-aside pattern: the tool worker is unaware of caching and just executes, keeping tool logic separate from caching concerns

Plug in Redis for real caching; the cache-check-and-route workflow maintains the same hit/miss routing interface.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):

## Project Structure

```
tool-use-caching/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/toolusecaching/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ToolUseCachingExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CacheResultWorker.java
│       ├── CheckCacheWorker.java
│       ├── ExecuteToolWorker.java
│       └── ReturnCachedWorker.java
└── src/test/java/toolusecaching/workers/
    ├── CacheResultWorkerTest.java        # 9 tests
    ├── CheckCacheWorkerTest.java        # 9 tests
    ├── ExecuteToolWorkerTest.java        # 9 tests
    └── ReturnCachedWorkerTest.java        # 9 tests

```
