# Inline Tasks in Java with Conductor

Demonstrates INLINE tasks. JavaScript that runs on the Conductor server with no workers. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## The Problem

You need to perform lightweight data transformations between workflow steps. computing a sum and average from a list of numbers, converting text to uppercase and generating a URL slug, classifying a score into tiers (gold/silver/bronze), and assembling a final response object. These operations are simple enough that deploying a dedicated worker service for each one is overkill. You just need a few lines of logic to run without the overhead of a separate Java process, Docker container, or network round-trip.

Without INLINE tasks, every transformation requires a deployed worker: a Java class, a task definition registration, a polling connection, and operational overhead. For a workflow with 10 small transformations, that means 10 worker classes that each contain 5 lines of logic wrapped in boilerplate. The cost of deploying, monitoring, and maintaining those workers far exceeds the complexity of the logic they contain.

## The Solution

**You just write JavaScript expressions in the workflow definition. Conductor executes them server-side via GraalJS. No workers, no polling, no deployment.**

This example uses zero workers. Every task is an INLINE task. JavaScript that executes directly on the Conductor server via GraalJS. The `math_aggregation` step computes sum, average, min, max, and count from an input number array. The `string_manipulation` step converts text to uppercase, counts words, generates a URL slug, and reverses the string. The `conditional_logic` step classifies the computed average into gold (90+), silver (80+), or bronze tiers. Finally, `build_response` assembles all results into a single structured response object. No worker deployment, no polling, no network hops. just JavaScript expressions evaluated server-side.

### What You Write: Workers

This example uses Conductor system tasks (INLINE). no custom workers needed. All four tasks are JavaScript expressions that execute directly on the Conductor server via GraalJS.

### The Workflow

```
math_aggregation [INLINE]
    │
    ▼
string_manipulation [INLINE]
    │
    ▼
conditional_logic [INLINE]
    │
    ▼
build_response [INLINE]

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
java -jar target/inline-tasks-1.0.0.jar

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
java -jar target/inline-tasks-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow inline_tasks_demo \
  --version 1 \
  --input '{"numbers": "sample-numbers", "text": "Process this order for customer C-100", "config": "sample-config"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w inline_tasks_demo -s COMPLETED -c 5

```

## How to Extend

Since this example uses only INLINE tasks (no workers), extending it means modifying the JavaScript expressions directly in `workflow.json`.

- **math_aggregation**: add percentile calculations, standard deviation, or weighted averages by editing the GraalJS expression
- **string_manipulation**: add regex-based extraction, template rendering, or locale-aware formatting
- **conditional_logic**: adjust tier thresholds, add more classification buckets, or incorporate multiple input signals into the decision
- **build_response**: reshape the output to match your downstream API contract or add computed fields

For operations that exceed what INLINE JavaScript can handle (database queries, HTTP calls, ML inference), convert that specific step to a SIMPLE task backed by a worker. the rest of the workflow stays unchanged.

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
inline-tasks/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/inlinetasks/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── InlineTasksExample.java          # Main entry point (supports --workers mode)
│   └── workers/
└── src/test/java/inlinetasks/workers/
    └── WorkflowJsonTest.java        # 17 tests

```
