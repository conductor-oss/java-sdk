# Chaos Engineering in Java with Conductor :  Experiment Design, Fault Injection, Observation, and Recovery

Orchestrates controlled chaos experiments using [Conductor](https://github.com/conductor-oss/conductor). This workflow defines an experiment (target service, fault type like CPU stress, network latency, or pod kill), injects the failure into the live system, observes the system's behavior during the fault (error rates, latency, recovery time), and then recovers by removing the injected failure.

## Proving Your System Breaks Gracefully

Your microservices architecture claims to be resilient, but you've never actually killed a database connection mid-request or added 500ms of network latency to the payment service. Chaos engineering means deliberately injecting failures. Pod kills, CPU stress, network partitions, disk pressure, and observing whether your system degrades gracefully or falls over. Each experiment needs a clear definition (what to break and how), controlled fault injection, real-time observation of the impact, and guaranteed recovery that removes the injected failure regardless of what happened during observation.

Without orchestration, you'd SSH into a server, run a stress-ng command, watch Grafana for a few minutes, then kill the process and hope you remembered to clean up. If the observation step reveals a cascading failure, there's no automated recovery, the fault stays injected while you scramble to fix the secondary problem. There's no record of what fault was injected, how long it ran, or what the error rates looked like during the experiment.

## The Solution

**You write the fault injection and observation logic. Conductor handles experiment sequencing, guaranteed recovery, and complete experiment audit trails.**

Each stage of the chaos experiment is a simple, independent worker. The experiment definer creates a structured experiment plan. Specifying the target service, fault type (CPU stress, network latency, pod kill), blast radius, and success criteria. The fault injector applies the specified failure to the target service in a controlled way. The observer monitors the system during the fault, recording error rates, latency spikes, and whether circuit breakers engaged. The recoverer removes the injected fault, restoring the system to its pre-experiment state. Conductor executes them in strict sequence, ensures recovery always runs even if observation reveals problems, and provides a complete audit trail of every chaos experiment. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Four workers run the chaos experiment. Defining the fault parameters, injecting the failure, observing system behavior, and recovering to steady state.

| Worker | Task | What It Does |
|---|---|---|
| **DefineExperimentWorker** | `ce_define_experiment` | Defines the chaos experiment parameters: target service, fault type (e.g., latency-injection), and blast radius |
| **InjectFailureWorker** | `ce_inject_failure` | Injects the specified fault (e.g., 500ms latency on 50% of requests) into the target service |
| **ObserveWorker** | `ce_observe` | Monitors system behavior during the experiment and checks SLO compliance (e.g., p99 < 2s) |
| **RecoverWorker** | `ce_recover` | Removes the injected fault and verifies the system has returned to nominal operation |

Workers simulate infrastructure operations with realistic output so you can see the automation flow without affecting real systems. Replace with real infrastructure API calls .  the workflow and rollback logic stay the same.

### The Workflow

```
ce_define_experiment
    │
    ▼
ce_inject_failure
    │
    ▼
ce_observe
    │
    ▼
ce_recover

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
java -jar target/chaos-engineering-1.0.0.jar

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
java -jar target/chaos-engineering-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow chaos_engineering_workflow \
  --version 1 \
  --input '{"service": "order-service", "faultType": "standard"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w chaos_engineering_workflow -s COMPLETED -c 5

```

## How to Extend

Each worker handles one experiment phase .  replace the simulated calls with Gremlin, LitmusChaos, or Toxiproxy for real fault injection and system observation, and the chaos workflow runs unchanged.

- **DefineExperimentWorker** → load experiment definitions from a catalog: Gremlin experiment templates, LitmusChaos experiment CRDs, or a custom experiment registry with pre-approved blast radius limits
- **InjectFailureWorker** → inject real faults: Chaos Monkey for random pod kills, Toxiproxy for network latency/packet loss, stress-ng for CPU/memory pressure, or tc (traffic control) for bandwidth throttling
- **ObserveWorker** → collect real observability data during the experiment: Prometheus queries for error rates and latency percentiles, Kubernetes event stream for pod restarts, and distributed tracing (Jaeger/Zipkin) for request flow analysis
- **RecoverWorker** → execute real recovery: remove Toxiproxy rules, delete LitmusChaos experiment CRs, scale deployments back to baseline, and verify service health via readiness probes

Plug in LitmusChaos or Gremlin for real fault injection; the experiment pipeline uses the same interface throughout.

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
chaos-engineering-chaos-engineering/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/chaosengineering/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── MainExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── DefineExperimentWorker.java
│       ├── InjectFailureWorker.java
│       ├── ObserveWorker.java
│       └── RecoverWorker.java
└── src/test/java/chaosengineering/
    └── MainExampleTest.java        # 2 tests .  workflow resource loading, worker instantiation

```
