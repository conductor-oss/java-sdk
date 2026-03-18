# Conductor UI Explorer in Java with Conductor -- A 3-Step Workflow for Learning the Dashboard

A Java Conductor workflow designed specifically for exploring the Conductor UI at `http://localhost:5000`. This three-step workflow gives you a running execution to inspect in the dashboard -- you can see task inputs and outputs flowing between steps, watch execution progress in real time, and explore the workflow diagram, timeline, and task detail views. Uses [Conductor](https://github.com/conductor-oss/conductor) to provide a live workflow execution you can explore visually.

## Seeing Orchestration in Action

Reading about workflow orchestration is abstract. Seeing it in a UI makes it concrete. This workflow runs three sequential tasks that pass data between them, giving you a real execution to explore in Conductor's built-in dashboard. You can click on each task to see its inputs, outputs, and timing. You can see how data flows from one step to the next via JSONPath expressions. And you can use the workflow diagram view to visualize the execution path.

This is a learning tool -- the tasks themselves are simple, but the UI exploration skills you build here apply to any Conductor workflow.

## The Solution

**You just write the step workers that produce inspectable outputs. Conductor handles sequencing, data flow, and the dashboard visualization.**

Three simple workers produce outputs that flow between tasks. The value is in exploring the Conductor UI -- task detail panels, workflow diagrams, execution timelines, and search/filtering -- using a real, running workflow as your sandbox.

### What You Write: Workers

Three simple workers -- StepOneWorker, StepTwoWorker, and StepThreeWorker -- give you a multi-step workflow to explore in the Conductor UI dashboard.

| Worker | Task | What It Does | Real / Simulated |
|---|---|---|---|
| **StepOneWorker** | `ui_step_one` | Step One -- Processes user action input. | Simulated |
| **StepThreeWorker** | `ui_step_three` | Step Three -- Summarizes results from steps one and two. | Simulated |
| **StepTwoWorker** | `ui_step_two` | Step Two -- Enriches data from step one with metadata. | Simulated |

Workers in this example use in-memory simulation so you can run the full workflow without external dependencies. To move to production, swap the simulated logic for your real service calls -- the worker contract stays the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically -- configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status -- no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
ui_step_one
    │
    ▼
ui_step_two
    │
    ▼
ui_step_three
```

## Example Output

```
=== Conductor UI Guide: Monitoring and Debugging Workflows ===

Step 1: Registering task definitions...
  Registered: ui_step_one, ui_step_three, ui_step_two

Step 2: Registering workflow 'conductor_ui'...
  Workflow registered.

Step 3: Starting workers...
  3 workers polling.

Step 4: Starting workflow...
  Workflow ID: 5da5938f-a662-da15-e050-383b8a5f3198

  [ui_step_one] Processed signup for user user-42
  [ui_step_two] Enriched: Processed signup for user user-42
  [ui_step_three] Pipeline complete. Step 1: n/a | Step 2: n/a



  Status: COMPLETED
  Output: {summary=Pipeline complete. Step 1: n/a | Step 2: n/a, path=ui_step_one -> ui_step_two -> ui_step_three}

Result: PASSED
```
## Running It

### Prerequisites

- **Java 21+** -- verify with `java -version`
- **Maven 3.8+** -- verify with `mvn -version`
- **Docker** -- to run Conductor

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
java -jar target/conductor-ui-1.0.0.jar
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
java -jar target/conductor-ui-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow ui_demo_workflow \
  --version 1 \
  --input '{"userId": "user-42", "action": "signup"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w ui_demo_workflow -s COMPLETED -c 5
```

## How to Extend

Replace the demo workers with your real application logic, then use the same Conductor UI to inspect task inputs, outputs, timing, and execution diagrams in production.

- **StepOneWorker** (`ui_step_one`) -- integrate with your production Conductor deployment with proper authentication and monitoring
- **StepThreeWorker** (`ui_step_three`) -- integrate with your production Conductor deployment with proper authentication and monitoring
- **StepTwoWorker** (`ui_step_two`) -- integrate with your production Conductor deployment with proper authentication and monitoring

Each step's logic is self-contained, so you can extend any step without affecting the others.

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
conductor-ui/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/conductorui/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ConductorUiExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── StepOneWorker.java
│       ├── StepThreeWorker.java
│       └── StepTwoWorker.java
└── src/test/java/conductorui/workers/
    ├── StepOneWorkerTest.java        # 5 tests
    └── StepTwoWorkerTest.java        # 6 tests
```
