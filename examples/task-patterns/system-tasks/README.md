# System Tasks in Java with Conductor

Demonstrates INLINE and JSON_JQ_TRANSFORM system tasks. no workers needed. Uses [Conductor](https://github.

## The Problem

You need to build an employee compensation summary: look up a user's profile (name, department, base salary, performance rating), calculate their bonus based on performance tiers (15% for ratings 4.5+, 10% for 4.0+, 5% for 3.0+), and format the results into a structured summary with compensation breakdown and performance classification. None of these steps require external API calls. it is all data lookup, calculation, and reshaping. Deploying three separate worker services for this logic is unnecessary overhead.

Without system tasks, you'd write three worker classes, each containing a few lines of logic wrapped in boilerplate (implement Worker interface, register task definition, handle polling). The user lookup is a map access, the bonus calculation is basic arithmetic with if/else, and the output formatting is JSON restructuring. Three deployed services for what is essentially three pure functions adds operational cost with no benefit.

## The Solution

**You just write INLINE JavaScript and JQ expressions in the workflow definition. Conductor runs them server-side. No workers needed for pure data lookup, calculation, and reshaping.**

This example uses zero workers.  everything runs as Conductor system tasks on the server. The `lookup_user` step (INLINE/GraalJS) takes a userId and looks up the employee profile from an in-memory map, returning name, department, base salary, and performance rating. The `calculate_bonus` step (INLINE/GraalJS) applies tiered bonus rules: Gold tier (15% bonus) for ratings 4.5+, Silver (10%) for 4.0+, Bronze (5%) for 3.0+, computing the dollar amount and total compensation. The `format_output` step (JSON_JQ_TRANSFORM) reshapes the user and bonus data into a structured summary with nested compensation and performance sections using a JQ expression. No worker deployment, no polling, no Docker containers,  just expressions evaluated server-side.

### What You Write: Workers

This example uses zero custom workers, all three tasks (user lookup, bonus calculation, output formatting) run as server-side INLINE and JQ system tasks with no external deployment.

This example uses Conductor system tasks. no custom workers needed. All logic runs server-side via INLINE (GraalJS) and JSON_JQ_TRANSFORM tasks.

### The Workflow

```
lookup_user [INLINE]
    │
    ▼
calculate_bonus [INLINE]
    │
    ▼
format_output [JSON_JQ_TRANSFORM]

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
java -jar target/system-tasks-1.0.0.jar

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
java -jar target/system-tasks-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow system_tasks_demo \
  --version 1 \
  --input '{"userId": "TEST-001"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w system_tasks_demo -s COMPLETED -c 5

```

## How to Extend

Since this example uses only system tasks (no workers), extending it means modifying the expressions in `workflow.json`.

- **lookup_user** (INLINE): replace the hardcoded user map with a real data source by converting this step to a SIMPLE worker that queries your HR database or identity provider (Workday, BambooHR, Okta)
- **calculate_bonus** (INLINE): adjust the tier thresholds and bonus percentages for your compensation policy, add additional factors (tenure, department multiplier, manager discretion)
- **format_output** (JSON_JQ_TRANSFORM): reshape the output to match your payroll system's expected format, add fields for tax withholding, benefits deductions, or equity vesting

For steps that need external data, convert the INLINE task to a SIMPLE task backed by a worker. the rest of the pipeline stays unchanged.

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
system-tasks/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/systemtasks/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── SystemTasksExample.java          # Main entry point (supports --workers mode)
│   └── workers/
└── src/test/java/systemtasks/workers/
    └── WorkflowDefinitionTest.java        # 19 tests

```
