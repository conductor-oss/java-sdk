# Distributed Locking in Java with Conductor

Distributed locking for concurrency control. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## The Problem

When multiple service instances process the same resource concurrently, you need a distributed lock to prevent race conditions. The workflow acquires a lock with a TTL on a named resource, executes the critical-section operation while holding the lock, and then releases it. If the process crashes, the TTL ensures the lock is eventually released.

Without orchestration, distributed locking is implemented inline with try/finally blocks around Redis or ZooKeeper calls. If the process crashes between acquiring and releasing, the lock may be held until TTL expires with no visibility into what operation was in progress.

## The Solution

**You just write the lock-acquire, critical-section, and lock-release workers. Conductor handles ordered lock lifecycle, crash-safe state so locks are eventually freed, and full audit of lock acquisitions.**

Each worker represents a service boundary. Conductor manages cross-service orchestration, compensating transactions, timeout enforcement, and distributed tracing .  your workers just make the service calls.

### What You Write: Workers

Three workers enforce mutual exclusion: AcquireLockWorker obtains a distributed lock with a TTL, ExecuteCriticalWorker performs the protected operation, and ReleaseLockWorker frees the lock token.

| Worker | Task | What It Does |
|---|---|---|
| **AcquireLockWorker** | `dl_acquire_lock` | Acquires a distributed lock on the specified resource with a TTL and returns a lock token. |
| **ExecuteCriticalWorker** | `dl_execute_critical` | Executes the critical-section operation on the locked resource (e.g., update a shared counter). |
| **ReleaseLockWorker** | `dl_release_lock` | Releases the distributed lock using the lock token. |

Workers simulate service calls with realistic request/response shapes so you can see the coordination pattern without running the full service mesh. Replace with real HTTP clients .  the workflow coordination stays the same.

### The Workflow

```
dl_acquire_lock
    │
    ▼
dl_execute_critical
    │
    ▼
dl_release_lock

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
java -jar target/distributed-locking-1.0.0.jar

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
java -jar target/distributed-locking-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow distributed_locking_workflow \
  --version 1 \
  --input '{"resourceId": "TEST-001", "operation": "sample-operation", "ttlSeconds": "sample-ttlSeconds"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w distributed_locking_workflow -s COMPLETED -c 5

```

## How to Extend

Connect each worker to your real Redis Redlock, ZooKeeper, or etcd cluster and your critical-section business logic, the acquire-execute-release workflow stays exactly the same.

- **AcquireLockWorker** (`dl_acquire_lock`): acquire a real distributed lock via Redis (Redlock), ZooKeeper, or etcd
- **ExecuteCriticalWorker** (`dl_execute_critical`): perform the actual database update, file write, or state mutation that requires mutual exclusion
- **ReleaseLockWorker** (`dl_release_lock`): release the lock in Redis/ZooKeeper using the token for safe compare-and-delete

Migrating from an in-memory lock to Redis Redlock or ZooKeeper requires no changes to the acquire-execute-release flow.

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
distributed-locking/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/distributedlocking/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── DistributedLockingExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AcquireLockWorker.java
│       ├── ExecuteCriticalWorker.java
│       └── ReleaseLockWorker.java
└── src/test/java/distributedlocking/workers/
    ├── AcquireLockWorkerTest.java        # 2 tests
    ├── ExecuteCriticalWorkerTest.java        # 2 tests
    └── ReleaseLockWorkerTest.java        # 2 tests

```
