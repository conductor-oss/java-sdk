# Implementing Compensation Workflows in Java with Conductor: Forward Steps with Automatic Undo on Failure

A Java Conductor workflow example demonstrating the compensation pattern. Executing a sequence of steps (create resource, insert record, send notification) where a failure at any step triggers a separate compensation workflow that undoes completed steps in reverse order to restore consistency.

## The Problem

You have a multi-step provisioning process where each step makes a side effect that needs to be reversed if a later step fails. Step A creates a cloud resource, Step B inserts a database record, Step C sends a notification. If Step C fails (external service unavailable), the database record from Step B must be deleted and the cloud resource from Step A must be cleaned up. In reverse order. Without this rollback, you end up with orphaned resources, dangling database records, and a system in an inconsistent state.

### What Goes Wrong Without Compensation

Consider this provisioning flow without compensation:

1. Step A creates an EC2 instance. **success** (resource exists)
2. Step B inserts a billing record. **success** (row exists in database)
3. Step C sends a confirmation email. **FAILS** (SMTP server down)

The workflow fails, but the EC2 instance is still running (costing money) and the billing record still exists (customer gets charged). Nobody knows these orphans exist until the monthly bill arrives or a customer complains.

The compensation pattern solves this with a separate compensation workflow that runs undo operations in reverse order: `Undo B` (delete billing record) then `Undo A` (terminate EC2 instance). Each undo worker receives the output of its corresponding forward step, so it knows exactly what to clean up.

## The Solution

**You just write the forward steps and their matching undo operations. Conductor handles forward execution sequencing, reverse-order compensation on failure, retries on each undo step, and a full audit trail of every forward and compensation action with their inputs and outputs.**

Each forward step and its corresponding undo are simple, independent workers. Step A creates a resource, UndoA deletes it. Step B inserts a record, UndoB removes it. When Step C fails, the main workflow ends with FAILED status. You then start the compensation workflow, which runs the undo workers in reverse order automatically. Every compensation action is tracked, so you can see exactly which steps were undone and whether the rollback completed successfully. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Three forward workers. CompStepAWorker, CompStepBWorker, and CompStepCWorker. Execute provisioning actions, while CompUndoBWorker and CompUndoAWorker reverse completed steps in reverse order when any forward step fails.

| Worker | Task | What It Does |
|---|---|---|
| **CompStepAWorker** | `comp_step_a` | Creates a resource. Always succeeds, returning `{result: "resource-A-created"}`. |
| **CompStepBWorker** | `comp_step_b` | Inserts a database record. Always succeeds, returning `{result: "record-B-inserted"}`. Receives Step A's result as `prevResult`. |
| **CompStepCWorker** | `comp_step_c` | Sends a notification. Fails if `failAtStep="C"` (simulating an external service outage), otherwise returns `{result: "notification-C-sent"}`. |
| **CompUndoAWorker** | `comp_undo_a` | Compensation: reverses Step A by deleting the created resource. Receives the `original` value from Step A's output. Returns `{undone: true}`. |
| **CompUndoBWorker** | `comp_undo_b` | Compensation: reverses Step B by removing the inserted record. Receives the `original` value from Step B's output. Returns `{undone: true}`. |

Workers simulate success and failure scenarios so you can observe the resilience pattern end-to-end. Swap in real service calls and the retry, compensation, and recovery behavior works identically.

### The Workflows

**Main workflow** (`compensatable_workflow`):

```
comp_step_a  -->  comp_step_b  -->  comp_step_c
                                     (can fail)

```

**Compensation workflow** (`compensation_workflow`): runs when main fails:

```
comp_undo_b  -->  comp_undo_a
(reverse order: undo B first, then A. Skip C since it never completed)

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
java -jar target/compensation-workflows-1.0.0.jar

```

### Option 3: Use the run script

```bash
./run.sh

# Or on a custom port:
CONDUCTOR_PORT=9090 ./run.sh

# Or pointing at an existing Conductor:
CONDUCTOR_BASE_URL=http://localhost:9090/api ./run.sh

```

## Example Output

```
=== Compensation Workflows: Undo Completed Steps ===

Step 1: Registering task definitions...
  Registered 5 task definitions.

Step 2: Registering workflows...
  Registered: compensatable_workflow
  Registered: compensation_workflow

Step 3: Starting workers...
  5 workers polling.

--- Scenario 1: All steps succeed ---
  Workflow ID: a1b2c3d4-...
  [Step A] Executing. Resource created
  [Step B] Executing. Record inserted
  [Step C] Executing. Notification sent
  Status: COMPLETED
  Output: {stepA=resource-A-created, stepB=record-B-inserted, stepC=notification-C-sent}

--- Scenario 2: Step C fails -> run compensation ---
  Workflow ID: e5f6a7b8-...
  [Step A] Executing. Resource created
  [Step B] Executing. Record inserted
  [Step C] FAILED. External service error
  Main workflow: FAILED

  Starting compensation workflow...
  [Undo B] Reversing: record-B-inserted
  [Undo A] Reversing: resource-A-created
  Compensation: COMPLETED

--- Compensation Pattern ---
  Forward: Step A -> Step B -> Step C (fails)
  Reverse: Undo B -> Undo A (skip C, it never completed)
  Key: compensation runs in reverse order of completed steps

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
java -jar target/compensation-workflows-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
# Happy path: all steps succeed
conductor workflow start \
  --workflow compensatable_workflow \
  --version 1 \
  --input '{"failAtStep": "none"}'

# Failure path: Step C fails (simulates external service outage)
conductor workflow start \
  --workflow compensatable_workflow \
  --version 1 \
  --input '{"failAtStep": "C"}'

# After a failure, start the compensation workflow manually:
conductor workflow start \
  --workflow compensation_workflow \
  --version 1 \
  --input '{"failedWorkflowId": "<failed_workflow_id>", "stepAResult": "resource-A-created", "stepBResult": "record-B-inserted"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w compensatable_workflow -s COMPLETED -c 5

```

## How to Extend

Each worker performs one provisioning action. Connect the resource creator to AWS EC2 or Terraform, the record inserter to your database, and the forward-plus-compensation workflow stays the same.

- **CompStepAWorker** (`comp_step_a`): create a real cloud resource (EC2 instance, Kubernetes pod, S3 bucket) and return its ID for undo tracking
- **CompUndoAWorker** (`comp_undo_a`): delete the cloud resource using the ID from Step A's output
- **CompStepBWorker** (`comp_step_b`): insert a real database record (Postgres, DynamoDB) and return the row/item key
- **CompUndoBWorker** (`comp_undo_b`): delete the database record using the key from Step B's output
- **CompStepCWorker** (`comp_step_c`): send a real notification (Slack, email, SMS) and handle failures that trigger the compensation chain
- **Auto-trigger compensation**: use Conductor's failure workflow feature to automatically start the compensation workflow when the main workflow fails, instead of triggering it manually

Connect each forward worker to your real cloud and database APIs, and the automatic compensation on failure operates in production without modification.

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
compensation-workflows/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   ├── workflow.json                # Main workflow (A -> B -> C)
│   └── compensation-workflow.json   # Compensation workflow (Undo B -> Undo A)
├── src/main/java/compensationworkflows/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── CompensationWorkflowsExample.java  # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CompStepAWorker.java
│       ├── CompStepBWorker.java
│       ├── CompStepCWorker.java
│       ├── CompUndoAWorker.java
│       └── CompUndoBWorker.java
└── src/test/java/compensationworkflows/workers/
    ├── CompStepAWorkerTest.java     # 4 tests
    ├── CompStepBWorkerTest.java     # 4 tests
    ├── CompStepCWorkerTest.java     # 8 tests
    ├── CompUndoAWorkerTest.java     # 5 tests
    └── CompUndoBWorkerTest.java     # 5 tests

```
