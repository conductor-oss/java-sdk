# Exactly-Once Processing in Java Using Conductor: Lock, Dedup, Process, Commit, Unlock

The payment service processes the $49.99 debit, then crashes before acknowledging the message. The broker retries. Another instance picks it up and processes it again, customer double-charged. You add an idempotency check, but two instances grab the same message simultaneously and both pass the check before either records it. You add a distributed lock, but the lock holder crashes mid-processing and the TTL expires before cleanup. Every fix opens a new failure mode. Exactly-once delivery is impossible, but exactly-once processing is achievable with the right protocol. This example implements the full lock-check-process-commit-unlock sequence with Conductor, ensuring the business effect happens once even when messages arrive twice. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers, you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## Preventing Duplicate Processing in Distributed Systems

A payment message arrives twice. Once from the original send, once from a retry after a timeout. Without exactly-once semantics, the customer gets charged twice. A stock trade gets executed, the acknowledgment is lost, and the retry creates a duplicate position. In any system where the same message can arrive more than once, you need a way to guarantee that the business effect happens exactly once.

Exactly-once processing requires a careful protocol: acquire a distributed lock on the resource key (so no other consumer can process the same message concurrently), check the idempotency store to see if this message ID was already processed, execute the business logic only if it's new, atomically commit the result and record the message ID, then release the lock. If any step fails between lock and commit, the lock's TTL ensures eventual release, and the next retry will find the message uncommitted and reprocess it safely.

## The Solution

**You write the locking and deduplication logic. Conductor handles the protocol sequencing, retries, and state recovery.**

`ExoLockWorker` acquires a distributed lock on the resource key with a 30-second TTL and returns a lock token. `ExoCheckStateWorker` looks up the message ID in the idempotency store to determine if it's already been processed. `ExoProcessWorker` executes the business logic only if the state check shows the message is new. `ExoCommitWorker` atomically writes the result and marks the message ID as processed, using the lock token to verify it still holds the lock. `ExoUnlockWorker` releases the lock. Conductor ensures this five-step protocol executes in strict order, and if any step fails, the workflow can be retried from the point of failure without risking double-processing.

### What You Write: Workers

Five workers enforce the exactly-once protocol: distributed locking, state checking, business logic execution, atomic commit, and lock release, each owning one step of the deduplication boundary.

| Worker | Task | What It Does | Real / Simulated |
|---|---|---|---|
| **ExoCheckStateWorker** | `exo_check_state` | Checks whether the message was already processed by looking up its current state and sequence number | Simulated |
| **ExoCommitWorker** | `exo_commit` | Atomically commits the processing result and records the state transition (pending to completed) | Simulated |
| **ExoLockWorker** | `exo_lock` | Acquires a distributed lock with a TTL to prevent concurrent processing of the same message | Simulated |
| **ExoProcessWorker** | `exo_process` | Executes the idempotent business logic (e.g., applying a debit and computing new balance) | Simulated |
| **ExoUnlockWorker** | `exo_unlock` | Releases the distributed lock after processing and commit are complete | Simulated |

Workers simulate the pattern behavior with realistic inputs and outputs so you can observe the advanced workflow mechanics. Replace with real implementations, the pattern and Conductor orchestration stay the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically. Configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status.; no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
exo_lock
    │
    ▼
exo_check_state
    │
    ▼
exo_process
    │
    ▼
exo_commit
    │
    ▼
exo_unlock
```

## Example Output

```
=== Exactly-Once Processing Demo ===

Step 1: Registering task definitions...
  Registered: exo_lock, exo_check_state, exo_process, exo_commit, exo_unlock

Step 2: Registering workflow 'exo_exactly_once'...
  Workflow registered.

Step 3: Starting workers...
  5 workers polling.

Step 4: Starting workflow...
  Workflow ID: dd56d88b-e888-2717-c05c-fcec72a5ddcf

  [lock] Processing
  [check] Processing
  [process] Processing
  [commit] Processing
  [unlock] Processing



  Status: COMPLETED
  Output: {messageId=TXN-2024-5678, processed=true, committed=true, unlocked=true}

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
java -jar target/exactly-once-1.0.0.jar
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
java -jar target/exactly-once-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow exo_exactly_once \
  --version 1 \
  --input '{"messageId": "TXN-2024-5678", "payload": "debit_49.99", "resourceKey": "account:ACC-001"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w exo_exactly_once -s COMPLETED -c 5
```

## How to Extend

Each worker enforces one part of the lock-check-process-commit protocol. Replace the simulated Redis locks and idempotency lookups with real distributed lock and dedup store APIs and the exactly-once guarantee runs unchanged.

- **ExoLockWorker** (`exo_lock`): acquire a real distributed lock using Redis (Redisson `RLock`), ZooKeeper (`InterProcessMutex`), or DynamoDB conditional writes with a TTL
- **ExoCheckStateWorker** (`exo_check_state`): query a real idempotency store (DynamoDB `getItem` by message ID, PostgreSQL `SELECT` on a processed_messages table, or Redis `EXISTS`)
- **ExoCommitWorker** (`exo_commit`): atomically write the result and mark the message as processed in a single database transaction, or use DynamoDB `TransactWriteItems` to commit both in one call

The lock-check-commit protocol stays fixed. Swap the simulated Redis lock for a real distributed lock (Redlock, ZooKeeper) and the deduplication guarantee holds unchanged.

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
exactly-once/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/exactlyonce/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ExactlyOnceExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ExoCheckStateWorker.java
│       ├── ExoCommitWorker.java
│       ├── ExoLockWorker.java
│       ├── ExoProcessWorker.java
│       └── ExoUnlockWorker.java
└── src/test/java/exactlyonce/workers/
    ├── ExoCheckStateWorkerTest.java        # 4 tests
    ├── ExoCommitWorkerTest.java        # 4 tests
    ├── ExoLockWorkerTest.java        # 4 tests
    ├── ExoProcessWorkerTest.java        # 4 tests
    └── ExoUnlockWorkerTest.java        # 4 tests
```
