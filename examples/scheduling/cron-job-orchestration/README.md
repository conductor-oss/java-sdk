# Cron Job Orchestration in Java Using Conductor: Schedule, Execute, Log, and Clean Up

The nightly data export runs at 2 AM. It creates 4 GB of temp files in `/tmp`, writes results to S3, and is supposed to clean up after itself. Last Thursday, the S3 upload timed out. The cron job exited with code 1. Nobody noticed. Cron sent an email to root, which nobody reads. The temp files stayed. Friday night, same thing. By Monday, `/tmp` was full, and every other service on the box started failing with "No space left on device." You SSH in, find 16 GB of orphaned export files, and realize there's no log of which runs succeeded, which failed, or what they left behind.

## The Problem

You have cron jobs that need more than just `crontab -e`. You need to track execution results, clean up temp files after the job runs, and know when a scheduled job fails silently. Traditional cron fires and forgets: if the job fails, the only evidence is buried in syslog. If temp files accumulate, disk fills up. If the job takes longer than expected, the next invocation starts before the first finishes.

Without orchestration, cron job management means parsing syslog for failures, writing cleanup scripts that may or may not run, and building custom locking to prevent overlapping executions. Each job has its own ad-hoc monitoring, and there's no unified view of job health.

## The Solution

**You just write the job execution and cleanup commands. Conductor handles sequential execution with guaranteed cleanup, retries on job failures, and a unified view of every cron job's execution history, exit codes, and cleanup status.**

Each cron job concern is an independent worker. Scheduling, execution, result logging, and cleanup. Conductor orchestrates them in sequence: schedule the job, execute it, log the results, then clean up. Every job run is tracked with execution time, output, exit code, and cleanup status. You get all of that for free, without writing a single line of orchestration code.

### What You Write: Workers

Four workers manage each cron job run: ScheduleJobWorker registers the schedule, ExecuteJobWorker runs the command, LogResultWorker records stdout/stderr and exit codes, and CleanupWorker removes temporary files created during execution.

| Worker | Task | What It Does | Real / Simulated |
|---|---|---|---|
| **CleanupWorker** | `cj_cleanup` | Cleans up temporary files produced by a cron job. | Simulated |
| **ExecuteJobWorker** | `cj_execute_job` | Executes a cron job command. | Simulated |
| **LogResultWorker** | `cj_log_result` | Logs the result of a cron job execution. | Simulated |
| **ScheduleJobWorker** | `cj_schedule_job` | Schedules a cron job with the given name and expression. | Simulated |

Workers simulate scheduled operations with realistic outputs so you can see the scheduling pattern without external systems. Replace with real job logic, the schedule triggers, retry behavior, and monitoring stay the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically. Configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status.; no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
cj_schedule_job
    │
    ▼
cj_execute_job
    │
    ▼
cj_log_result
    │
    ▼
cj_cleanup
```

## Example Output

```
=== Example 401: Cron Job Orchestratio ===

Step 1: Registering task definitions...
  Registered: cj_schedule_job, cj_execute_job, cj_log_result, cj_cleanup

Step 2: Registering workflow 'cron_job_orchestration_401'...
  Workflow registered.

Step 3: Starting workers...
  4 workers polling.

Step 4: Starting workflow...
  Workflow ID: eea96911-af80-2e95-baa5-e4b05016f684

  [cleanup] Removing temp files for \"" + jobName + "\"...
  [execute] Running job \"" + jobName + "\" at
  [log] Logging result for \"" + jobName
                + "\". Exit code:
  [schedule] Scheduling cron job \"" + jobName
                + "\" with expression \"" + cronExpression + "\"...

  Status: COMPLETED
  Output: {scheduledAt=2026-03-08T02:00:00Z, exitCode=0, logged=true, cleanedUp=true}

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
java -jar target/cron-job-orchestration-1.0.0.jar
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
java -jar target/cron-job-orchestration-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow cron_job_orchestration_401 \
  --version 1 \
  --input '{"jobName": "nightly-data-export", "cronExpression": "0 2 * * *", "command": "python export_data.py --format=csv"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w cron_job_orchestration_401 -s COMPLETED -c 5
```

## How to Extend

Each worker handles one job lifecycle step. Connect the executor to run real shell commands or Kubernetes Jobs, the cleanup worker to purge temp files, and the schedule-execute-log-cleanup workflow stays the same.

- **CleanupWorker** (`cj_cleanup`): remove temp files, release temporary cloud resources, clean up staging tables created during execution
- **ExecuteJobWorker** (`cj_execute_job`): run real commands via ProcessBuilder, SSH to remote hosts, or invoke Lambda/Cloud Functions
- **LogResultWorker** (`cj_log_result`): persist job results to Elasticsearch, CloudWatch Logs, or your job management database

Replace simulated execution with real shell commands, and the schedule-execute-cleanup orchestration works unchanged.

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
cron-job-orchestration/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/cronjoborchestration/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── CronJobOrchestrationExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CleanupWorker.java
│       ├── ExecuteJobWorker.java
│       ├── LogResultWorker.java
│       └── ScheduleJobWorker.java
└── src/test/java/cronjoborchestration/workers/
    ├── CleanupWorkerTest.java        # 8 tests
    ├── ExecuteJobWorkerTest.java        # 8 tests
    ├── LogResultWorkerTest.java        # 8 tests
    └── ScheduleJobWorkerTest.java        # 8 tests
```
