# Implementing Self-Healing Workflow in Java with Conductor :  Health Check, Diagnosis, Remediation, and Retry

A Java Conductor workflow example demonstrating self-healing .  checking service health, diagnosing the issue when unhealthy, automatically applying remediation (restart, clear cache, scale up), and retrying the failed process after recovery.

## The Problem

Your service goes unhealthy .  high error rate, degraded performance, resource exhaustion. Instead of paging an engineer at 3 AM, you want the system to heal itself: detect the issue via health check, diagnose the root cause (memory leak, disk full, dependency down), apply the appropriate fix (restart the service, clear the cache, scale up replicas), and retry the original operation. Self-healing reduces mean time to recovery from minutes (human response) to seconds (automated).

Without orchestration, self-healing logic is scattered across monitoring scripts, cron jobs, and runbooks. Health checks live in Nagios, remediation scripts live on jump boxes, and the retry logic is manual. Building a coherent self-healing loop that diagnoses before remediating (don't restart if the issue is a full disk) requires gluing together multiple tools.

## The Solution

**You just write the health check and remediation actions. Conductor handles SWITCH-based routing between healthy and unhealthy paths, the check-diagnose-remediate-retry sequence, retries on remediation steps, and a complete record of every healing attempt showing what was detected, what fix was applied, and whether recovery succeeded.**

Each self-healing step is an independent worker .  health check evaluates the service, diagnose identifies the root cause, remediate applies the fix, and retry re-runs the original operation. Conductor orchestrates the flow: when the health check fails, it routes to diagnosis and remediation before retrying. Every healing attempt is tracked ,  you can see what was detected, what fix was applied, and whether the retry succeeded. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

HealthCheckWorker evaluates service status, DiagnoseWorker identifies the root cause when unhealthy, RemediateWorker applies the appropriate fix (restart, cache clear, scale up), and RetryProcessWorker re-runs the original operation after recovery.

| Worker | Task | What It Does |
|---|---|---|
| **DiagnoseWorker** | `sh_diagnose` | Worker for sh_diagnose .  diagnoses problems in an unhealthy service. Analyzes symptoms and returns a diagnosis with r.. |
| **HealthCheckWorker** | `sh_health_check` | Worker for sh_health_check .  checks the health of a service. If service is "broken-service", returns healthy="false" .. |
| **ProcessWorker** | `sh_process` | Worker for sh_process .  normal processing for healthy services. Returns result="processed-{data}" where data comes fr.. |
| **RemediateWorker** | `sh_remediate` | Worker for sh_remediate .  applies the fix recommended by diagnosis. Takes the diagnosis and action, applies the remed.. |
| **RetryProcessWorker** | `sh_retry_process` | Worker for sh_retry_process .  retries processing after remediation. Returns result="healed-{data}" to indicate the se.. |

Workers simulate success and failure scenarios so you can observe the resilience pattern end-to-end. Swap in real service calls and the retry, compensation, and recovery behavior works identically.

### The Workflow

```
sh_health_check
    │
    ▼
SWITCH (health_switch_ref)
    ├── false: sh_diagnose -> sh_remediate -> sh_retry_process
    └── default: sh_process
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
java -jar target/self-healing-1.0.0.jar
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
java -jar target/self-healing-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow self_healing_demo \
  --version 1 \
  --input '{"service": "test-value", "data": "test-value"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w self_healing_demo -s COMPLETED -c 5
```

## How to Extend

Each worker runs one healing step .  connect the health checker to your service endpoints, the remediator to Kubernetes scaling or service restarts, and the check-diagnose-remediate-retry workflow stays the same.

- **DiagnoseWorker** (`sh_diagnose`): analyze logs (ELK, Splunk), check resource utilization (CPU, memory, disk), query dependency health
- **HealthCheckWorker** (`sh_health_check`): check real service health via HTTP endpoints, Kubernetes liveness probes, or CloudWatch metrics
- **ProcessWorker** (`sh_process`): run your actual business logic (e.g., process an order, transform data, invoke a downstream service) that the health check has confirmed is safe to execute

Connect the health checker to your real service endpoints and the remediator to Kubernetes or your infrastructure APIs, and the self-healing loop operates in production with no orchestration changes.

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
self-healing/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/selfhealing/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── SelfHealingExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── DiagnoseWorker.java
│       ├── HealthCheckWorker.java
│       ├── ProcessWorker.java
│       ├── RemediateWorker.java
│       └── RetryProcessWorker.java
└── src/test/java/selfhealing/workers/
    ├── DiagnoseWorkerTest.java        # 3 tests
    ├── HealthCheckWorkerTest.java        # 6 tests
    ├── ProcessWorkerTest.java        # 4 tests
    ├── RemediateWorkerTest.java        # 3 tests
    └── RetryProcessWorkerTest.java        # 4 tests
```
