# Graphql API in Java with Conductor

GraphQL API demo .  single task workflow to demonstrate REST vs GraphQL query patterns. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers ,  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## The Problem

You need to trigger a deployment operation through a GraphQL API .  sending a project name and target environment, and getting back a structured deployment result. Unlike REST endpoints where you might need multiple calls to different URLs, a GraphQL query lets the client specify exactly what data it needs in a single request. The workflow takes the project and environment as inputs, executes the deployment via a GraphQL mutation or query, and returns the result.

Without orchestration, you'd call the GraphQL endpoint directly from your application code, handling authentication, query construction, error parsing, and retries inline. If the GraphQL API returns a partial error (some fields resolved, others failed), you'd need custom logic to decide whether to retry. There is no audit trail of which deployments were triggered, what inputs were sent, or what the API returned.

## The Solution

**You just write the GraphQL query construction and execution worker. Conductor handles retries, timeout management, and audit logging.**

This example demonstrates using a Conductor worker to interact with a GraphQL API. The GraphqlTaskWorker receives a project name and target environment, constructs the appropriate GraphQL query or mutation, executes the deployment operation, and returns the structured result. Conductor wraps the GraphQL call with automatic retries (handling transient network failures or API rate limits), timeout management, and full observability .  every deployment request and response is recorded with timing data. This pattern works for any GraphQL API interaction, not just deployments.

### What You Write: Workers

A single GraphqlTaskWorker receives a project name and target environment, constructs the GraphQL query, executes the deployment operation, and returns the structured result.

| Worker | Task | What It Does |
|---|---|---|
| **GraphqlTaskWorker** | `gql_task` | GraphQL task worker .  simulates a deployment operation. Takes a project name and environment, returns a deployment re.. |

Workers simulate their processing steps so you can see the pattern in action without external services. Replace the simulation with real processing logic .  the task pattern and Conductor orchestration remain unchanged.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
gql_task
```

## Example Output

```
=== GraphQL API: REST vs GraphQL Query Patterns ===

Step 1: Registering task definitions...
  Registered: gql_task

Step 2: Registering workflow 'graphql_demo'...
  Workflow registered.

Step 3: Starting workers...
  1 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [gql_task] Deploying project=

  Status: COMPLETED
  Output: {project=..., result=...}

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
java -jar target/graphql-api-1.0.0.jar
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
java -jar target/graphql-api-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow graphql_demo \
  --version 1 \
  --input '{"project": "sample-project", "conductor-app": "sample-conductor-app", "env": "sample-env"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w graphql_demo -s COMPLETED -c 5
```

## How to Extend

Point the GraphQL worker at your real API endpoint with proper authentication, and the query/mutation execution workflow runs unchanged.

- **GraphqlTaskWorker** (`gql_task`): make real GraphQL API calls using a client library (Apollo, graphql-java, or OkHttp with raw POST); construct queries dynamically based on input parameters, handle authentication (Bearer tokens, API keys), parse GraphQL-specific error responses, and return the resolved data fields

Pointing the worker at a real GraphQL endpoint or changing the query structure does not require workflow modifications, as long as the deployment result fields are returned consistently.

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
graphql-api/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/graphqlapi/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── GraphqlApiExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       └── GraphqlTaskWorker.java
└── src/test/java/graphqlapi/workers/
    └── GraphqlTaskWorkerTest.java        # 8 tests
```
