# Task Input Templates in Java with Conductor

Shows reusable parameter mapping patterns. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## The Problem

You need to wire data between tasks, a user lookup returns a profile, a context builder enriches it with permissions, and an action executor needs fields from both previous steps plus the original workflow input. Input templates let you map outputs from any previous task into the next task's input using expressions like `${task_ref.output.field}`, composing complex objects from multiple sources without writing glue code.

Without input templates, you'd write mapping logic in each worker, the context builder would need to know where the user profile came from, and the action executor would need to reach back into earlier task outputs. Input templates keep the data wiring in the workflow definition where it belongs, and workers stay decoupled.

## The Solution

**You just write the user lookup, context builder, and action execution workers. Conductor handles the cross-task data wiring via input template expressions.**

This example demonstrates Conductor's input template expressions for wiring data between tasks in a user-action pipeline. LookupUserWorker takes a userId and returns a profile (name, role, email). The workflow's input template for the next task constructs a nested `context` object by pulling `${lookup_ref.output.name}` and `${lookup_ref.output.role}` alongside the original `${workflow.input.action}`. BuildContextWorker enriches that context with computed permissions based on the role. Finally, ExecuteActionWorker's input template pulls fields from both `${lookup_ref.output}` and `${context_ref.output}` plus `${workflow.input.metadata}`. Demonstrating multi-source input composition. The workers themselves know nothing about each other; all the data wiring lives in the workflow definition's `inputParameters` blocks.

### What You Write: Workers

Three workers form a user-action pipeline wired together by input templates: LookupUserWorker fetches the user profile, BuildContextWorker enriches it with role-based permissions, and ExecuteActionWorker performs the authorized action, all data mapping between them lives in the workflow definition, not in worker code.

| Worker | Task | What It Does |
|---|---|---|
| **BuildContextWorker** | `tpl_build_context` | Enriches a request context with computed permissions based on user role. |
| **ExecuteActionWorker** | `tpl_execute_action` | Executes an action, checking permissions from the enriched context. |
| **LookupUserWorker** | `tpl_lookup_user` | Looks up a user by ID and returns their profile. |

Workers simulate their processing steps so you can see the pattern in action without external services. Replace the simulation with real processing logic .  the task pattern and Conductor orchestration remain unchanged.

### The Workflow

```
tpl_lookup_user
    │
    ▼
tpl_build_context
    │
    ▼
tpl_execute_action

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
java -jar target/task-input-templates-1.0.0.jar

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
java -jar target/task-input-templates-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow input_templates_demo \
  --version 1 \
  --input '{"userId": "TEST-001", "action": "process", "metadata": {"key": "value"}}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w input_templates_demo -s COMPLETED -c 5

```

## How to Extend

Replace the user lookup and action execution with real database queries and API calls, and the input-template-driven data wiring works unchanged.

- **LookupUserWorker** (`tpl_lookup_user`): query a real identity provider (Okta, Auth0, LDAP) or user database to return the profile with name, role, email, and any custom attributes
- **BuildContextWorker** (`tpl_build_context`): call a permissions service or RBAC system to resolve the user's role into specific permissions, feature flags, and access scopes
- **ExecuteActionWorker** (`tpl_execute_action`): perform the actual action (e.g., provision a resource, update a record, trigger a deployment) using the enriched context for authorization checks

Connecting the user lookup to a real identity provider and the action executor to production APIs requires no changes to the input template expressions, since each worker's output fields feed into downstream tasks through the workflow definition.

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
task-input-templates/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/taskinputtemplates/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── TaskInputTemplatesExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── BuildContextWorker.java
│       ├── ExecuteActionWorker.java
│       └── LookupUserWorker.java
└── src/test/java/taskinputtemplates/workers/
    ├── BuildContextWorkerTest.java        # 4 tests
    ├── ExecuteActionWorkerTest.java        # 6 tests
    └── LookupUserWorkerTest.java        # 5 tests

```
