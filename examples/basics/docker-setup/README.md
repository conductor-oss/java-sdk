# Docker Setup Verification in Java with Conductor: One-Task Smoke Test

A minimal Java Conductor workflow with a single task that verifies your Docker-based Conductor setup is working correctly. If the workflow completes successfully, your Docker environment (Conductor server, worker connectivity, task polling) is properly configured. If it fails, the error tells you exactly what's wrong. Uses [Conductor](https://github.com/conductor-oss/conductor) running in Docker.

## Verifying Your Setup Before Building

Before building complex workflows, you need to confirm the basics work: Conductor is running in Docker, the health endpoint responds, workers can connect and poll for tasks, and workflow execution completes end-to-end. This single-task workflow is the quickest way to verify all of that.

A successful run means: Docker is running Conductor, the API is reachable, the worker registered and polled successfully, the task executed, and the result was recorded. If any of those steps fail, you get a specific error to debug.

## The Solution

**Run this first, build everything else second.**

One worker, one task, one workflow. If it completes, your setup is correct. If it fails, the error pinpoints whether the issue is Docker, Conductor, connectivity, or the worker.

### What You Write: Workers

A single smoke-test worker verifies your Docker and Conductor setup is working correctly before you build anything more complex.

| Worker | Task | What It Does |
|---|---|---|
| `DockerTestWorker` | `docker_test_task` | Accepts a `message` string input (defaults to "Docker setup test" if blank), echoes it back with an ISO-8601 timestamp to confirm Docker-based Conductor connectivity |

Workers in this example use in-memory simulation so you can run the full workflow without external dependencies. To move to production, swap the simulated logic for your real service calls, the worker contract stays the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically. Configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status.; no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
docker_test_task
```

## Example Output

```
=== Docker Setup Verificatio ===

Step 1: Registering task definitions...
  Registered: docker_test_task

Step 2: Registering workflow 'docker_setup_test'...
  Workflow registered.

Step 3: Starting workers...
  1 workers polling.

Step 4: Starting workflow...
  Workflow ID: 56f449fa-3b28-4065-8d10-2777d9c2b2c3

  [docker_test_task worker] Verifying Docker setup


  Status: COMPLETED
  Output: {message=Docker setup test, timestamp=2026-03-16T14:30:00Z}

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
java -jar target/docker-setup-1.0.0.jar
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
java -jar target/docker-setup-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow docker_setup_test \
  --version 1 \
  --input '{"message": "Sample message"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w docker_setup_test -s COMPLETED -c 5
```

## How to Extend

Once this smoke test passes, you can start building real workflows, the Docker environment and worker connectivity are verified and ready for production workers.



The smoke test worker verifies connectivity without coupling to any specific workflow definition.

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
docker-setup/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/dockersetup/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── DockerSetupExample.java          # Main entry point (supports --workers mode)
│   └── workers/
└── src/test/java/dockersetup/workers/
    └── DockerTestWorkerTest.java        # 5 tests
```
