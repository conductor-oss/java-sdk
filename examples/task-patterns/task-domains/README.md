# Task Domains in Java with Conductor

Task Domains demo .  route tasks to specific worker groups using domains. Uses [Conductor](https://github.

## The Problem

You need to route tasks to specific worker groups, for example, sending GPU-intensive work to GPU-equipped workers or routing region-specific tasks to workers in that region. Task domains let you tag workers with domain labels so only workers in the matching domain pick up the task, without changing the workflow definition.

Without task domains, you'd need separate task types for each worker group (e.g., `process_gpu`, `process_cpu`), duplicating workflow definitions. Task domains decouple routing from task identity, the same `td_process` task can go to different worker pools based on the domain configuration at runtime.

## The Solution

**You just write the processing worker and register it with domain labels. Conductor handles routing tasks to the correct worker pool based on runtime domain configuration.**

This example runs two instances of the same `td_process` worker, each registered with a different domain label (e.g., "gpu" and "cpu"). The TdProcessWorker tags its output with the worker group name so you can verify which domain handled the task. The example code starts the workflow with a `taskToDomain` mapping that routes `td_process` to a specific domain at runtime, the same workflow definition, the same task name, but different worker pools handling it depending on the domain configuration passed at execution time. No workflow JSON changes are needed to shift work between groups.

### What You Write: Workers

A single worker demonstrates domain-based routing: TdProcessWorker tags its output with the worker group name so you can verify which domain pool (GPU, CPU, region) handled the task, while the workflow definition stays identical across all domain configurations.

| Worker | Task | What It Does |
|---|---|---|
| **TdProcessWorker** | `td_process` | Processes data and tags output with the worker group name. When used with task domains, this worker can be routed to ... |

Workers simulate their processing steps so you can see the pattern in action without external services. Replace the simulation with real processing logic .  the task pattern and Conductor orchestration remain unchanged.

### The Workflow

```
td_process

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
java -jar target/task-domains-1.0.0.jar

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
java -jar target/task-domains-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow task_domain_demo \
  --version 1 \
  --input '{"data": {"key": "value"}}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w task_domain_demo -s COMPLETED -c 5

```

## How to Extend

Deploy your real worker across GPU and CPU pools tagged with domain labels, and the domain-based task routing works unchanged without modifying the workflow definition.

- **TdProcessWorker** (`td_process`): replace the simulated processing with real domain-specific work: GPU-domain workers could run CUDA-based inference, region-domain workers could query local databases with low latency, or tier-domain workers could apply different SLA-based processing speeds

Deploying real domain-specific processing (GPU inference, region-local queries, tier-based SLA enforcement) requires no workflow changes, since the domain routing is configured entirely through task-to-domain mappings at execution time.

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
task-domains/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/taskdomains/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── TaskDomainsExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       └── TdProcessWorker.java
└── src/test/java/taskdomains/workers/
    └── TdProcessWorkerTest.java        # 8 tests

```
