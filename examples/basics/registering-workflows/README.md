# Workflow Registration in Java with Conductor: How to Register Definitions via the SDK

A Java example that demonstrates how to register workflow and task definitions with Conductor using the Java SDK, the essential first step before running any workflow. The example registers a simple echo workflow, runs it, and shows the output. This teaches the registration API: how workflow JSON gets loaded, how task definitions get created, and how the SDK submits them to the Conductor server. Uses [Conductor](https://github.com/conductor-oss/conductor) to accept and manage the registered definitions.

## Understanding Workflow Registration

Before a workflow can run, its definition must be registered with the Conductor server. This is a one-time operation (per version) that tells Conductor the workflow's structure, which tasks it contains, how they connect, and what inputs they expect. Task definitions must also be registered so Conductor knows about retry policies, timeouts, and rate limits.

This example walks through the registration process end-to-end: load a workflow definition from JSON, register it via the SDK, start an execution, and verify the result. The echo task simply passes a message through, so you can focus on the registration mechanics.

## The Solution

**Register once, run many times.**

The example shows the complete registration lifecycle. Creating task definitions, registering the workflow, starting an execution, and reading the result. Once you understand this pattern, you can register any workflow definition.

### What You Write: Workers

Workers here illustrate how task definitions and workflow registrations connect through the SDK, showing the registration lifecycle.

| Worker | Task | What It Does |
|---|---|---|
| **EchoWorker** | `echo_task` | Simple worker that echoes an input message back as output. |

Workers in this example use in-memory simulation so you can run the full workflow without external dependencies. To move to production, swap the simulated logic for your real service calls, the worker contract stays the same.

### The Workflow

```
echo_task

```

## Example Output

```
=== Workflow Registration: SDK and JSON Approaches ===

Step 1: Registering task definitions...
  Registered: echo_task

Step 2: Registering workflow 'registering_workflows'...
  Workflow registered.

Step 3: Starting workers...
  1 workers polling.

Step 4: Starting workflow...
  Workflow ID: 07e9970f-4311-6a5d-a137-6fb991beb8fb

  [echo_task worker] Hello from v1!


  Status: COMPLETED
  Output: {result=(no message)}

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
docker run -d -p 8080:8080 -p 1234:5000 orkesio/orkes-conductor-standalone:1.2.3

# Wait for Conductor to be ready
until curl -sf http://localhost:8080/health > /dev/null; do sleep 2; done

# Build and run
mvn package -DskipTests
java -jar target/registering-workflows-1.0.0.jar

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
java -jar target/registering-workflows-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow registration_demo \
  --version 1 \
  --input '{"message": "Hello from Conductor!"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w registration_demo -s COMPLETED -c 5

```

## How to Extend

Use this same registration pattern for your production workflows. Load the workflow JSON, register task definitions with retry and timeout policies, and submit via the SDK.

- **EchoWorker** (`echo_task`): integrate with your production Conductor deployment with proper authentication and monitoring

Workflow definitions and task registrations use the same SDK calls regardless of how complex your workers become.

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
registering-workflows/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/registeringworkflows/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── RegisteringWorkflowsExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       └── EchoWorker.java
└── src/test/java/registeringworkflows/workers/
    └── EchoWorkerTest.java        # 4 tests

```
