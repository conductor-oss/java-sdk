# Runbook Automation in Java with Conductor

Your runbooks live in a Confluence wiki that was last updated eight months ago. When the database failover alert fires at 3 AM, the on-call engineer opens the page, squints at step 4 ("promote the replica: see Jira ticket DB-247 for details"), guesses at the parameters, runs the commands in the wrong order, and spends 20 minutes cleaning up the mess before starting over. The next engineer who gets this alert will have the same experience, because nobody updated the wiki after the schema changed in January. Every incident is a fresh adventure, and MTTR is a function of who happens to be on call. This workflow uses [Conductor](https://github.com/conductor-oss/conductor) to turn runbook steps into executable, versioned automation, load the procedure, execute remediation, verify the fix, and log the outcome, so incident response is identical regardless of who's holding the pager.

## The 3 AM Runbook Problem

A database failover alert fires. The on-call engineer opens the wiki, finds the "database-failover" runbook, and manually executes each step: promote the replica, verify the new primary accepts connections, update connection strings. Each step depends on the previous one, and skipping or re-ordering them risks data loss. Automating this sequence means incidents get resolved in seconds instead of the 20 minutes it takes a sleep-deprived human to follow a checklist.

Without orchestration, you'd wire all of this together in a single monolithic class. Managing execution order manually, writing try/catch blocks around every step, building retry loops with backoff, and adding logging to understand what happened when things go wrong. That code becomes brittle, hard to test, and impossible to observe at scale.

## The Solution

**You write the remediation steps. Conductor handles runbook sequencing, verification gates, and execution logging.**

Each worker automates one operational step. Conductor manages execution sequencing, rollback on failure, timeout enforcement, and full audit logging. Your workers call the infrastructure APIs.

### What You Write: Workers

Each worker handles one runbook stage. Loading the procedure, executing remediation, verifying the fix, and logging the outcome with timing data.

| Worker | Task | What It Does | Real / Simulated |
|---|---|---|---|
| `LoadRunbookWorker` | `ra_load_runbook` | Looks up the versioned runbook definition by name and returns its ID and version (e.g., "database-failover v3") | Simulated |
| `ExecuteStepWorker` | `ra_execute_step` | Runs the primary remediation action from the runbook (e.g., promotes a database replica to primary) | Simulated |
| `VerifyStepWorker` | `ra_verify_step` | Validates that the remediation succeeded (e.g., confirms the new primary is accepting connections) | Simulated |
| `LogOutcomeWorker` | `ra_log_outcome` | Records the final execution outcome and duration (45s) for audit and post-mortem review | Simulated |

Workers simulate infrastructure operations with realistic output so you can see the automation flow without affecting real systems. Replace with real infrastructure API calls, the workflow and rollback logic stay the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically. Configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status.; no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
ra_load_runbook
    |
    v
ra_execute_step
    |
    v
ra_verify_step
    |
    v
ra_log_outcome
```

## Example Output

```
=== Example runbook-automation: Runbook Automatio ===

Step 1: Registering task definitions...
  Registered: ra_load_runbook, ra_execute_step, ra_verify_step, ra_log_outcome

Step 2: Registering workflow 'runbook_automation_workflow'...
  Workflow registered.

Step 3: Starting workers...
  4 workers polling.

Step 4: Starting workflow...
  Workflow ID: 6f755406-5288-4313-024b-34ecc1a198ca

  [execute] Promoted DB replica
  [load] Runbook: database-failover v3
  [log] Outcome: completed in 45s
  [verify] New primary accepting connections

  Status: COMPLETED

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
java -jar target/runbook-automation-1.0.0.jar
```

### Option 3: Use the run script

```bash
./run.sh

# Or on a custom port:
CONDUCTOR_PORT=9090 ./run.sh

# Or pointing at an existing Conductor:
CONDUCTOR_BASE_URL=http://localhost:9090/api ./run.sh
```

### Sample Output

```
=== Example runbook-automation: Runbook Automation ===

  [load] Runbook: database-failover v3
  [execute] Promoted DB replica
  [verify] New primary accepting connections
  [log] Outcome: completed in 45s
  runbookId: RB-100
  outcome: success

Result: PASSED
```

## Configuration

| Environment Variable | Default | Description |
|---|---|---|
| `CONDUCTOR_BASE_URL` | `http://localhost:8080/api` | Conductor server URL |
| `CONDUCTOR_PORT` | `8080` | Host port for Conductor (Docker Compose only) |

## Using the Conductor CLI

```bash
conductor workflow start \
  --workflow runbook_automation_workflow \
  --version 1 \
  --input '{"runbookName": "database-failover", "trigger": "alert-db-primary-down"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w runbook_automation_workflow -s COMPLETED -c 5
```

## How to Extend

Each worker handles one runbook step. Replace the simulated calls with Ansible playbooks, AWS RDS failover APIs, or Confluence lookups, and the automation workflow runs unchanged.

- **`LoadRunbookWorker`**: Fetch runbook definitions from Confluence API, a Git repository, or an internal runbook registry instead of returning a hardcoded runbook ID.

- **`ExecuteStepWorker`**: Call the AWS RDS failover API, Kubernetes rollout commands, or Ansible playbook triggers to perform real remediation actions.

- **`VerifyStepWorker`**: Run real health checks against the remediated service. Database connection tests, HTTP readiness probes, or query latency measurements.

- **`LogOutcomeWorker`**: Write execution results to Elasticsearch, Splunk, or a Jira ticket for post-mortem tracking with real timing data.

Point the workers at your actual runbook store and infrastructure APIs; the execution pipeline remains unchanged.

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
runbook-automation-runbook-automation/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/runbookautomation/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── MainExample.java             # Main entry point
│   └── workers/
│       ├── ExecuteStepWorker.java   # Runs the remediation action (e.g., promote replica)
│       ├── LoadRunbookWorker.java   # Loads versioned runbook definition by name
│       ├── LogOutcomeWorker.java    # Records outcome and duration for audit
│       └── VerifyStepWorker.java    # Validates remediation succeeded
└── src/test/java/runbookautomation/workers/
    ├── ExecuteStepWorkerTest.java
    ├── LoadRunbookWorkerTest.java
    ├── LogOutcomeWorkerTest.java
    └── VerifyStepWorkerTest.java
```
