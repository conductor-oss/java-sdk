# Performance Profiling in Java Using Conductor :  Instrument, Collect Profiles, Analyze Hotspots, and Recommend

A Java Conductor workflow example for performance profiling .  instrumenting a service, collecting CPU/memory profiles, analyzing hotspots (slow methods, memory leaks), and generating optimization recommendations.

## The Problem

Your service is slow and you need to find out why. Performance profiling requires instrumenting the service to collect profile data (CPU sampling, memory allocation tracking), analyzing the profiles to find hotspots (methods consuming the most CPU, objects causing the most GC), and generating actionable recommendations ("this method is O(n^2) .  consider a hashmap").

Without orchestration, profiling is a manual process .  an engineer SSHs to a server, runs async-profiler, downloads the flame graph, and eyeballs it. There's no automated pipeline from instrumentation to recommendation, and profiles are lost after the session ends.

## The Solution

**You just write the profiler integration and hotspot analysis logic. Conductor handles the instrument-collect-analyze-recommend sequence, retries when profiler attachment fails, and a durable record of every profiling session's findings and recommendations.**

Each profiling step is an independent worker .  instrumentation, profile collection, hotspot analysis, and recommendation generation. Conductor runs them in sequence: instrument the target, collect the profile, analyze hotspots, then generate recommendations. Every profiling session is tracked with the exact profile data, findings, and recommendations. You get all of that for free, without writing a single line of orchestration code.

### What You Write: Workers

Four workers run each profiling session: PrfInstrumentWorker attaches the profiler, CollectProfileWorker gathers CPU and memory samples, AnalyzeHotspotsWorker identifies the costliest methods, and a RecommendWorker generates specific optimization suggestions.

| Worker | Task | What It Does |
|---|---|---|
| **AnalyzeHotspotsWorker** | `prf_analyze_hotspots` | Analyzes collected profile samples to identify CPU hotspots (e.g., methods consuming the most CPU) and their percentages |
| **CollectProfileWorker** | `prf_collect_profile` | Collects CPU/memory profile data over the configured duration, returning sample count and profile size |
| **PrfInstrumentWorker** | `prf_instrument` | Attaches a profiler to the target service for the specified profile type (CPU/memory), reporting overhead percentage |
| **RecommendWorker** | `prf_recommend` | Generates optimization recommendations based on hotspot findings, with estimated improvement and a report URL |

Workers simulate scheduled operations with realistic outputs so you can see the scheduling pattern without external systems. Replace with real job logic .  the schedule triggers, retry behavior, and monitoring stay the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
prf_instrument
    │
    ▼
prf_collect_profile
    │
    ▼
prf_analyze_hotspots
    │
    ▼
prf_recommend
```

## Example Output

```
=== Example 430: Performance Profiling ===

Step 1: Registering task definitions...
  Registered: prf_instrument, prf_collect_profile, prf_analyze_hotspots, prf_recommend

Step 2: Registering workflow 'performance_profiling_430'...
  Workflow registered.

Step 3: Starting workers...
  4 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [analyze] Analyzing
  [collect] Collecting
  [instrument] Instrumenting
  [recommend] Generating

  Status: COMPLETED
  Output: {hotspotCount=..., topHotspot=..., hotspots=..., sampleCount=...}

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
java -jar target/performance-profiling-1.0.0.jar
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
java -jar target/performance-profiling-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow performance_profiling_430 \
  --version 1 \
  --input '{"serviceName": "sample-name", "order-service": "sample-order-service", "profilingDuration": "sample-profilingDuration", "60s": "sample-60s", "profileType": "standard"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w performance_profiling_430 -s COMPLETED -c 5
```

## How to Extend

Each worker runs one profiling phase .  connect the instrumentor to async-profiler or JFR, the analyzer to parse real flame graphs and heap dumps, and the instrument-collect-analyze-recommend workflow stays the same.

- **AnalyzeHotspotsWorker** (`prf_analyze_hotspots`): parse flame graphs, identify top CPU consumers, detect memory leak patterns, and flag contention hotspots
- **CollectProfileWorker** (`prf_collect_profile`): collect CPU samples, heap dumps, or allocation profiles over the specified duration and upload to S3
- **PrfInstrumentWorker** (`prf_instrument`): attach async-profiler, JFR, or eBPF-based profilers to the target JVM/process via agent APIs

Connect to async-profiler or your APM agent, and the profiling pipeline runs against real services with no workflow changes needed.

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
performance-profiling/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/performanceprofiling/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── PerformanceProfilingExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AnalyzeHotspotsWorker.java
│       ├── CollectProfileWorker.java
│       ├── PrfInstrumentWorker.java
│       └── RecommendWorker.java
└── src/test/java/performanceprofiling/workers/
    └── AnalyzeHotspotsWorkerTest.java        # 2 tests
```
