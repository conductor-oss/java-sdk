# Hello World in Java with Conductor -- The Simplest Possible Workflow

The absolute minimum Conductor example -- one workflow, one task, one worker. Takes a `name` as input, produces a greeting as output. This is your starting point for understanding how Conductor works: you define a workflow in JSON, write a worker in Java, and Conductor connects them. Uses [Conductor](https://github.com/conductor-oss/conductor) to run a single task.

## Your First Conductor Workflow

Before building complex pipelines, you need to understand the core loop: a workflow definition (JSON) declares tasks, a worker (Java class implementing `Worker`) polls for and executes tasks, and Conductor connects them. This example strips away everything except that core loop -- one task that takes a name and returns a greeting.

After running this, you'll understand: how to define a workflow in `workflow.json`, how to implement a worker with `getTaskDefName()` and `execute()`, how input flows from the workflow to the worker via `task.getInputData()`, and how output flows back via `result.getOutputData()`.

## The Solution

**One worker, one task, one workflow. The simplest possible Conductor application.**

A single `greet` worker receives a name, produces a greeting, and returns it. Conductor handles the workflow lifecycle, task polling, and execution tracking -- even for this trivially simple case.

### What You Write: Workers

A single GreetWorker demonstrates the minimum Conductor contract: receive input via `getInputData()`, do your work, return output via `getOutputData()`.

| Worker | Task | What It Does | Real / Simulated |
|---|---|---|---|
| **GreetWorker** | `greet` | Takes a `name` from input, returns `"Hello, {name}! Welcome to Conductor."` -- defaults to `"World"` if name is blank or missing. | Real (pure logic) |

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically -- configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status -- no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
greet
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
java -jar target/hello-world-1.0.0.jar
```

### Option 3: Use the run script

```bash
./run.sh

# Or on a custom port:
CONDUCTOR_PORT=9090 ./run.sh

# Or pointing at an existing Conductor:
CONDUCTOR_BASE_URL=http://localhost:9090/api ./run.sh
```

### Example Output

```
=== Hello World: Your First Conductor Workflow ===

Step 1: Registering task definition...
  Registered: greet

Step 2: Registering workflow 'hello_world_workflow'...
  Workflow registered.

Step 3: Starting worker...
  1 worker polling.

Step 4: Starting workflow...

  [greet worker] Hello, Developer!
  Workflow ID: <workflow-id>

Step 5: Waiting for completion...
  Status: COMPLETED
  Output: {greeting=Hello, Developer! Welcome to Conductor.}

Result: PASSED
```

## Configuration

| Environment Variable | Default | Description |
|---|---|---|
| `CONDUCTOR_BASE_URL` | `http://localhost:8080/api` | Conductor server URL |
| `CONDUCTOR_PORT` | `8080` | Host port for Conductor (Docker Compose only) |

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/hello-world-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow hello_world_workflow \
  --version 1 \
  --input '{"name": "Developer"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w hello_world_workflow -s COMPLETED -c 5
```

## How to Extend

- **Replace the greeting with a personalized welcome email** -- swap `GreetWorker`'s in-memory string with a SendGrid or AWS SES API call. Read the recipient's name and email from `task.getInputData()`, build an HTML template, and send via the email SDK. The worker returns `{emailId, sentAt}` instead of a plain greeting string.
- **Add a second worker to log the greeting to an audit database** -- create an `AuditLogWorker` (`audit_log`) that receives the greeting output from `GreetWorker` and writes it to PostgreSQL or DynamoDB with a timestamp and request ID. Add it as a second task in `workflow.json` after `greet`.
- **Chain greet with a notification worker** -- add a `NotifyWorker` (`notify`) that takes the greeting and posts it to a Slack channel via webhook or sends it as an SMS via Twilio. Wire it in `workflow.json` so it runs after `greet`, reading `${greet_ref.output.greeting}` as input.
- **Use workflow input to customize the greeting language** -- add a `language` field to the workflow input (e.g., `"language": "es"`). In `GreetWorker.execute()`, read `task.getInputData().get("language")` and select from a map of localized templates (`"es"` -> `"Hola, {name}! Bienvenido a Conductor."`, `"fr"` -> `"Bonjour, {name}! Bienvenue sur Conductor."`). Defaults to English when the field is missing.

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
hello-world/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/helloworld/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── HelloWorldExample.java       # Main entry point (supports --workers mode)
│   └── workers/
│       └── GreetWorker.java
└── src/test/java/helloworld/workers/
    └── GreetWorkerTest.java
```
