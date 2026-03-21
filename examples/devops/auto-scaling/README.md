# Auto-Scaling in Java with Conductor :  Analyze Metrics, Plan Scaling, Execute, Verify

Analyzes service metrics, plans scaling action, executes scaling, and verifies the result. Pattern: analyze -> plan -> execute -> verify. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## Scaling Decisions Need Analysis, Not Just Thresholds

CPU is at 80%. Should you scale up? Maybe, or maybe it's a transient spike from a batch job that ends in 5 minutes. Auto-scaling needs more than simple threshold crossing: analyze the metric trend (is it sustained or transient?), plan the scaling (how many instances, which instance type?), execute the scaling operation, and verify the new instances are healthy and handling traffic.

Scaling too aggressively wastes money (spinning up 10 instances for a 2-minute spike). Scaling too conservatively risks outages (waiting too long while latency degrades). The analyze step should consider metric history, time of day, and business context (is this a known traffic pattern?). The verify step must confirm new instances are healthy before declaring success.

## The Solution

**You write the metric analysis and scaling logic. Conductor handles the analyze-plan-execute-verify sequence and records every scaling decision.**

`AnalyzeWorker` examines current metrics (CPU utilization, memory pressure, request rate, queue depth) and their trends over the analysis window to determine if scaling is needed. `PlanWorker` determines the scaling action. scale up (add instances), scale down (remove instances), or maintain,  with the target instance count based on the metric analysis. `ExecuteWorker` performs the scaling operation,  launching new instances or terminating excess ones. `VerifyWorker` confirms the scaled service is healthy,  new instances passing health checks, traffic being distributed, and metrics improving. Conductor records every scaling decision with its metric context for capacity planning analysis.

### What You Write: Workers

Four workers handle the scaling decision. Analyzing current metrics, planning the scaling action, executing instance changes, and verifying the result.

| Worker | Task | What It Does |
|---|---|---|
| **Analyze** | `as_analyze` | Analyzes current service metrics (CPU, memory, request rate) to determine load. |
| **Execute** | `as_execute` | Executes the planned scaling action (scale-up, scale-down, or no-change). |
| **Plan** | `as_plan` | Plans the scaling action based on current load analysis. |
| **Verify** | `as_verify` | Verifies that the scaling action achieved the desired result. |

Workers implement infrastructure operations with realistic output so you can see the automation flow without affecting real systems. Replace with real infrastructure API calls. the workflow and rollback logic stay the same.

### The Workflow

```
as_analyze
    │
    ▼
as_plan
    │
    ▼
as_execute
    │
    ▼
as_verify

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
java -jar target/auto-scaling-1.0.0.jar

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
java -jar target/auto-scaling-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow auto_scaling_workflow \
  --version 1 \
  --input '{"service": "order-service", "metric": "sample-metric", "threshold": "sample-threshold"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w auto_scaling_workflow -s COMPLETED -c 5

```

## How to Extend

Each worker handles one scaling phase. replace the simulated calls with CloudWatch metrics, AWS Auto Scaling Groups, or Kubernetes HPA for real trend analysis and instance management, and the scaling workflow runs unchanged.

- **Analyze** (`as_analyze`): query Prometheus, CloudWatch, or Datadog for real-time metrics with trend analysis and anomaly detection to distinguish genuine load increases from transient spikes
- **Plan** (`as_plan`): compute the optimal target instance count based on metric trends, time-of-day patterns, and business context, choosing between scale-up, scale-down, or maintain
- **Execute** (`as_execute`): use AWS Auto Scaling Groups, Kubernetes HPA/VPA, or GCP Managed Instance Groups for real scaling operations with proper drain periods for scale-down
- **Verify** (`as_verify`): check load balancer health status, monitor error rates post-scaling, and ensure new instances are registered with service discovery before declaring success

Connect to AWS Auto Scaling or Kubernetes HPA; the scaling workflow preserves the same analyze-plan-execute-verify interface.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):

## Project Structure

```
auto-scaling/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/autoscaling/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── AutoScalingExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── Analyze.java
│       ├── Execute.java
│       ├── Plan.java
│       └── Verify.java
└── src/test/java/autoscaling/workers/
    ├── AnalyzeTest.java        # 10 tests
    ├── ExecuteTest.java        # 8 tests
    ├── PlanTest.java        # 10 tests
    └── VerifyTest.java        # 9 tests

```
