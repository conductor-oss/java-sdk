# Human Task with Form Input in Java Using Conductor :  Data Collection, WAIT for Structured Form Submission, and Form Processing

A Java Conductor workflow example demonstrating human task forms .  collecting initial data, pausing at a WAIT task that simulates a HUMAN task with a form schema (name, email, review decision), and processing the structured form response. Demonstrates how workflows can pause for human input through structured forms rather than simple approve/reject buttons. Uses [Conductor](https://github.## Workflows Need to Pause for Human Input via Forms

Some workflows require a human to fill out a form. Reviewing an application, entering data, or making a decision with structured input fields. The workflow collects initial data, pauses at a WAIT task (simulating a HUMAN task with a form schema), and the human provides structured input. The form response is then processed to determine the outcome. If processing fails, you need to retry it with the form data intact.

## The Solution

**You just write the data-collection and form-processing workers. Conductor handles the durable pause for structured human input via the form schema.**

Each worker handles one stage of the approval chain. Conductor manages task assignment, wait states, timeout escalation, and audit logging .  your code handles the decision logic.

### What You Write: Workers

CollectDataWorker gathers context for the form, and ProcessFormWorker interprets the submitted fields and decision, the structured form schema and durable pause are handled by the WAIT task.

| Worker | Task | What It Does |
|---|---|---|
| **CollectDataWorker** | `ht_collect_data` | Collects initial data before the human review .  gathers context needed for the form and signals readiness for human input |
| *WAIT task* | `ht_human_review` | Pauses with a form schema for the reviewer; completed via `POST /tasks/{taskId}` with structured form data (name, email, decision, comments) | Built-in Conductor WAIT .  no worker needed |
| **ProcessFormWorker** | `ht_process_form` | Processes the submitted form data .  reads the approved flag and form fields, determines the outcome (approved or rejected), and triggers downstream actions |

Workers simulate the approval steps and human decisions so the workflow runs end-to-end without manual intervention. In production, replace the auto-approve logic with real human task assignments .  the workflow structure stays the same.

### The Workflow

```
ht_collect_data
    │
    ▼
human_review_wait [WAIT]
    │
    ▼
ht_process_form
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
java -jar target/human-task-1.0.0.jar
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
java -jar target/human-task-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow human_task_form_demo \
  --version 1 \
  --input '{"applicantName": "test"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w human_task_form_demo -s COMPLETED -c 5
```

## How to Extend

Each worker handles one side of the form interaction .  connect your data source for initial collection and your application processing backend for form responses, and the human-task workflow stays the same.

- **CollectDataWorker** (`ht_collect_data`): gather application data from a database or CRM, and prepare the form schema with pre-populated fields and validation rules
- **ProcessFormWorker** (`ht_process_form`): route the form decision to downstream systems. Approved applications go to onboarding, rejected ones trigger a notification to the applicant

Plug in your CRM or business application for real data collection and the form-based human task pipeline operates as designed.

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
human-task/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/humantask/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── HumanTaskExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CollectDataWorker.java
│       └── ProcessFormWorker.java
└── src/test/java/humantask/workers/
    ├── CollectDataWorkerTest.java        # 4 tests
    └── ProcessFormWorkerTest.java        # 7 tests
```
