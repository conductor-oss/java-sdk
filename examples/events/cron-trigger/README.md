# Cron Trigger in Java Using Conductor

Cron-like scheduled workflow: check if the current time matches a schedule expression, decide whether to run via SWITCH, and execute or skip accordingly. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers .  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## The Problem

You need to execute scheduled jobs based on cron expressions. The workflow must evaluate whether the current time falls within a cron schedule window, decide whether to run or skip the job, execute the scheduled tasks if triggered, and record the run for audit. Skipping a scheduled job without logging it means you lose visibility into why a task did not execute.

Without orchestration, you'd build a cron daemon that parses expressions, forks processes for each job, manages PID files, and writes to a run log .  manually handling overlapping executions, retrying failed jobs, and grepping through log files to figure out why Tuesday's report did not run.

## The Solution

**You just write the schedule-check, task-execution, skip-logging, and run-recording workers. Conductor handles cron-based SWITCH routing, durable run recording, and automatic retry of failed job executions.**

Each scheduling concern is a simple, independent worker .  a plain Java class that does one thing. Conductor takes care of executing the schedule check, routing via a SWITCH task to either execute or skip, recording the result, retrying if the job execution fails, and tracking every scheduled run with full input/output details. You get all of that for free, without writing a single line of orchestration code.

### What You Write: Workers

Four workers implement scheduled execution: CheckScheduleWorker evaluates a cron expression against the current time, ExecuteTasksWorker runs the job, LogSkipWorker records skipped windows, and RecordRunWorker logs completed executions.

| Worker | Task | What It Does |
|---|---|---|
| **CheckScheduleWorker** | `cn_check_schedule` | Checks whether a cron schedule matches the current time window. |
| **ExecuteTasksWorker** | `cn_execute_tasks` | Executes the scheduled tasks for a cron job. |
| **LogSkipWorker** | `cn_log_skip` | Logs that a scheduled job was skipped. |
| **RecordRunWorker** | `cn_record_run` | Records a completed cron job run. |

Workers simulate event processing with realistic payloads so you can trace the full event flow without external message brokers. Replace the simulation with real event sources .  the workflow and routing logic stay the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |
| **Conditional routing** | SWITCH tasks route execution to different paths based on worker output |

### The Workflow

```
cn_check_schedule
    │
    ▼
SWITCH (switch_ref)
    ├── yes: cn_execute_tasks -> cn_record_run
    ├── no: cn_log_skip
```

## Example Output

```
=== Cron Trigger Demo ===

Step 1: Registering task definitions...
  Registered: cn_check_schedule, cn_execute_tasks, cn_record_run, cn_log_skip

Step 2: Registering workflow 'cron_trigger'...
  Workflow registered.

Step 3: Starting workers...
  4 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [cn_check_schedule] Checking cron \"" + cronExpression
                + "\" for job \"" + jobName + "\"
  [cn_execute_tasks] Running job \"" + jobName
                + "\" at
  [cn_log_skip] Job \"" + jobName + "\" skipped:
  [cn_record_run] Job \"" + jobName
                + "\" completed with result:

  Status: COMPLETED
  Output: {shouldRun=..., scheduledTime=..., cronExpression=..., result=...}

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
java -jar target/cron-trigger-1.0.0.jar
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
java -jar target/cron-trigger-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow cron_trigger \
  --version 1 \
  --input '{"cronExpression": "sample-cronExpression", "0 */5 * * *": "sample-0 */5 * * *", "jobName": "sample-name"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w cron_trigger -s COMPLETED -c 5
```

## How to Extend

Connect each worker to your real cron expression evaluator, scheduled task logic, and run-history database, the check-decide-execute-record workflow stays exactly the same.

- **CheckScheduleWorker** (`cn_check_schedule`): parse real cron expressions using a library like cron-utils or Quartz CronExpression and evaluate against the current system time
- **ExecuteTasksWorker** (`cn_execute_tasks`): invoke your actual job logic: database maintenance, report generation, data pipeline triggers, or API calls
- **RecordRunWorker** (`cn_record_run`): persist run history to your scheduling database or observability platform (Datadog, Prometheus) for audit and monitoring
- **LogSkipWorker** (`cn_log_skip`): write skip events to your logging infrastructure (ELK, Splunk) so you have visibility into why a scheduled job did not execute

Wiring ExecuteTasksWorker to real job logic or RecordRunWorker to a database preserves the schedule-check-execute-record flow.

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
cron-trigger/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/crontrigger/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── CronTriggerExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CheckScheduleWorker.java
│       ├── ExecuteTasksWorker.java
│       ├── LogSkipWorker.java
│       └── RecordRunWorker.java
└── src/test/java/crontrigger/workers/
    ├── CheckScheduleWorkerTest.java        # 8 tests
    ├── ExecuteTasksWorkerTest.java        # 8 tests
    ├── LogSkipWorkerTest.java        # 8 tests
    └── RecordRunWorkerTest.java        # 8 tests
```
