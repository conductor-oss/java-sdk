# Workflow Metadata in Java with Conductor

Demonstrates workflow metadata and search. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers .## The Problem

You need to tag workflow executions with searchable metadata: category, priority, team ownership, so you can query and filter them later. Conductor lets you attach custom metadata (tags, labels, description) to workflow definitions and individual executions, making it possible to search for all "billing" workflows or all "high-priority" executions across your system.

Without workflow metadata, finding specific executions means filtering by workflow name and time range alone. Metadata lets you search by business context. "show me all failed high-priority billing workflows from this week."

## The Solution

**You just write the metadata-aware processing worker. Conductor handles attaching searchable tags, categories, and priority labels to workflow executions.**

This example attaches custom metadata: category, priority, and tags, to workflow definitions and individual executions, then demonstrates searching by those fields. MetadataTaskWorker receives `category` and `priority` inputs and returns `{ processed: true }`. The example code shows how to set workflow-level metadata (owner team, description, tags) on the workflow definition, pass business context as correlation IDs or custom labels when starting executions, and then use Conductor's search API to query executions by metadata, for example, finding all failed high-priority billing workflows from the past week. The worker itself is trivial because the value is in the metadata tagging and search, not the processing logic.

### What You Write: Workers

One worker demonstrates metadata-driven categorization: MetadataTaskWorker receives category and priority inputs and returns a processing result, while the real value lies in the searchable tags, categories, and labels Conductor attaches to the workflow execution.

| Worker | Task | What It Does |
|---|---|---|
| **MetadataTaskWorker** | `md_task` | Simple metadata task worker. Receives category and priority inputs, returns processed: true. |

Workers simulate their processing steps so you can see the pattern in action without external services. Replace the simulation with real processing logic .  the task pattern and Conductor orchestration remain unchanged.

### The Workflow

```
md_task
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
java -jar target/workflow-metadata-1.0.0.jar
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
java -jar target/workflow-metadata-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow metadata_demo \
  --version 1 \
  --input '{"category": "test-value", "priority": "test-value"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w metadata_demo -s COMPLETED -c 5
```

## How to Extend

Replace the stub task with real categorized processing (invoicing, compliance auditing, ticketing), and the metadata tagging and search capabilities work unchanged.

- **MetadataTaskWorker** (`md_task`): implement the actual categorized processing (e.g., route "billing" category to your invoicing system, "compliance" to your audit trail, "support" to your ticketing platform) and use the priority metadata to drive SLA-aware processing

Implementing real categorized processing (billing, compliance, support) and using metadata-driven search to audit executions requires no changes to the tagging and search infrastructure, since Conductor indexes the metadata independently.

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
workflow-metadata/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/workflowmetadata/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── WorkflowMetadataExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       └── MetadataTaskWorker.java
└── src/test/java/workflowmetadata/workers/
    └── MetadataTaskWorkerTest.java        # 6 tests
```
