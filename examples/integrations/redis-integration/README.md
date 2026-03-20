# Redis Integration in Java Using Conductor

A Java Conductor workflow that exercises core Redis operations .  connecting to a Redis instance, performing GET/SET key-value operations, managing cache TTL and memory, and publishing a message to a Redis Pub/Sub channel. Given a host, key, value, and channel, the pipeline demonstrates connection management, caching, and messaging. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate the connect-cache-pubsub pipeline.

## Coordinating Redis Operations Across Connection, Cache, and Pub/Sub

Redis workflows often involve multiple operation types that share a connection: connecting to the server, writing and reading cache entries, managing TTL policies, and publishing messages to channels for real-time notifications. Each step depends on the connection established in the first step, and cache management needs to happen before publishing events that reference cached data.

Without orchestration, you would manage Redis connection IDs manually, chain Jedis or Lettuce calls, and handle connection lifecycle and error recovery yourself. Conductor sequences the operations and passes connection IDs and key references between workers automatically.

## The Solution

**You just write the Redis workers. Connection management, GET/SET operations, cache TTL configuration, and Pub/Sub messaging. Conductor handles connection lifecycle ordering, Redis operation retries, and connection ID routing between cache and messaging stages.**

Each worker integrates with one external system. Conductor manages the integration sequence, retry logic, timeout handling, and data transformation between systems.

### What You Write: Workers

Four workers exercise Redis operations: RedisConnectWorker establishes the connection, GetSetWorker performs key-value reads and writes, CacheMgmtWorker configures TTL and eviction policies, and PubSubWorker publishes messages to channels.

| Worker | Task | What It Does |
|---|---|---|
| **RedisConnectWorker** | `red_connect` | Connects to the Redis instance .  establishes a connection to the specified host and returns the connectionId for use by subsequent operations |
| **GetSetWorker** | `red_get_set` | Performs GET/SET operations .  writes the key-value pair and reads it back to confirm, returning the stored value |
| **CacheMgmtWorker** | `red_cache_mgmt` | Manages cache TTL and memory .  sets expiration times on keys, checks memory usage, and applies eviction policies |
| **PubSubWorker** | `red_pub_sub` | Publishes a message to a Redis channel .  sends the message to the specified Pub/Sub channel for real-time notification of subscribers |

Workers simulate external API calls with realistic response shapes so you can see the integration flow end-to-end. Replace with real API clients .  the workflow orchestration and error handling stay the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
red_connect
    │
    ▼
red_get_set
    │
    ▼
red_pub_sub
    │
    ▼
red_cache_mgmt
```

## Example Output

```
=== Example 445: Redis Integratio ===

Step 1: Registering task definitions...
  Registered: red_connect, red_get_set, red_pub_sub, red_cache_mgmt

Step 2: Registering workflow 'redis_integration_445'...
  Workflow registered.

Step 3: Starting workers...
  4 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [cache] Set TTL for
  [set] [SIMULATED]
  [publish] Channel \"" + channel + "\" ->
  [connect] Connected to

  Status: COMPLETED
  Output: {ttl=..., memoryUsage=..., encoding=..., setOk=...}

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
java -jar target/redis-integration-1.0.0.jar
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
| `REDIS_URL` | _(none)_ | Redis connection URL (e.g., `redis://localhost:6379`). Currently unused, all workers run in simulated mode with `[SIMULATED]` output prefix. Swap in Jedis or Lettuce for production. |

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/redis-integration-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow redis_integration_445 \
  --version 1 \
  --input '{"redisHost": "sample-redisHost", "redis://localhost:6481": "sample-redis://localhost:6481", "cacheKey": "sample-cacheKey", "user:session:abc123": "sample-user:session:abc123", "cacheValue": "sample-cacheValue", "session-data": "sample-session-data", "channel": "sample-channel"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w redis_integration_445 -s COMPLETED -c 5
```

## How to Extend

Swap in Jedis or Lettuce for connection management, Redis commands for caching and TTL, and Pub/Sub for real-time messaging against your real Redis instance. The workflow definition stays exactly the same.

- **RedisConnectWorker** (`red_connect`): use Jedis or Lettuce to establish a real Redis connection with authentication
- **GetSetWorker** (`red_get_set`): use real Redis GET/SET commands via Jedis or Lettuce
- **PubSubWorker** (`red_pub_sub`): use Redis PUBLISH command via Jedis or Lettuce for real message publishing

Swap each simulation for real Jedis or Lettuce commands while preserving output fields, and the connect-cache-pubsub pipeline needs no changes.

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
redis-integration/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/redisintegration/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── RedisIntegrationExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CacheMgmtWorker.java
│       ├── GetSetWorker.java
│       ├── PubSubWorker.java
│       └── RedisConnectWorker.java
└── src/test/java/redisintegration/workers/
    ├── CacheMgmtWorkerTest.java        # 2 tests
    ├── GetSetWorkerTest.java        # 2 tests
    ├── PubSubWorkerTest.java        # 2 tests
    └── RedisConnectWorkerTest.java        # 2 tests
```
