# User-Assigned Human Task in Java Using Conductor :  Document Preparation, WAIT Assigned to Specific Reviewer, and Post-Review Finalization

A Java Conductor workflow example demonstrating user-specific task assignment .  preparing a document, pausing at a WAIT task assigned to a designated reviewer (not a group), and finalizing the document after the assigned person completes their review. Unlike group claims where anyone can pick up the task, this pattern ensures only the specified user can act. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers ,  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## Review Tasks Need to Be Assigned to a Specific Person

Unlike group assignments, some tasks must go to a specific user, the document's author, a designated reviewer, or a subject-matter expert. The workflow prepares the document, pauses at a WAIT task assigned to the specific user, and after they complete their review, a post-review step finalizes the document. If finalization fails, you need to retry it without re-assigning the review.

## The Solution

**You just write the document-preparation and post-review finalization workers. Conductor handles the user-specific assignment and the durable wait for that reviewer.**

Each worker handles one stage of the approval chain. Conductor manages task assignment, wait states, timeout escalation, and audit logging .  your code handles the decision logic.

### What You Write: Workers

HuaPrepareWorker identifies the designated reviewer from document metadata, and HuaPostReviewWorker applies their feedback. Neither handles the user-specific assignment or the wait for that person's response.

| Worker | Task | What It Does |
|---|---|---|
| **HuaPrepareWorker** | `hua_prepare` | Prepares the document for review .  formats it, identifies the assigned reviewer from the document metadata, and signals readiness |
| *WAIT task* | `hua_user_review` | Pauses until the assigned user completes their review via `POST /tasks/{taskId}` with their feedback and decision | Built-in Conductor WAIT .  no worker needed |
| **HuaPostReviewWorker** | `hua_post_review` | Finalizes the document after review .  applies the reviewer's feedback, updates the document status, and notifies the author |

Workers simulate the approval steps and human decisions so the workflow runs end-to-end without manual intervention. In production, replace the auto-approve logic with real human task assignments .  the workflow structure stays the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
hua_prepare
    │
    ▼
assigned_review [WAIT]
    │
    ▼
hua_post_review
```

## Example Output

```
=== HUMAN Task with User Assignment Demo ===

Step 1: Registering task definitions...
  Registered: ...

Step 2: Registering workflow 'human_user_assignment_demo'...
  Workflow registered.

Step 3: Starting workers...
  2 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [hua_post_review] Finalizing document after review...
  [hua_prepare] Preparing document for review...

  Status: COMPLETED
  Output: {finalized=..., prepared=...}

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
java -jar target/human-user-assignment-1.0.0.jar
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
java -jar target/human-user-assignment-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow human_user_assignment_demo \
  --version 1 \
  --input '{"documentId": "DOC-12345", "DOC-12345": "assignedTo", "assignedTo": "reviewer@example.com", "reviewer@example.com": "sample-reviewer@example.com"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w human_user_assignment_demo -s COMPLETED -c 5
```

## How to Extend

Each worker handles one end of the review cycle .  connect your document management system for preparation and your review tracking platform for finalization, and the user-assignment workflow stays the same.

- **HuaPostReviewWorker** (`hua_post_review`): apply the reviewer's changes, update document status in your DMS, send notification to the author, and archive the review record
- **HuaPrepareWorker** (`hua_prepare`): fetch the document from a DMS, look up the designated reviewer from an org chart or assignment rules engine, and set permissions

Wire up your document management system and the user-specific review assignment continues to work without workflow changes.

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
human-user-assignment/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/humanuserassignment/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── HumanUserAssignmentExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── HuaPostReviewWorker.java
│       └── HuaPrepareWorker.java
└── src/test/java/humanuserassignment/workers/
    ├── HuaPostReviewWorkerTest.java        # 5 tests
    └── HuaPrepareWorkerTest.java        # 5 tests
```
