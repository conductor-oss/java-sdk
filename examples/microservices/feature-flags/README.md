# Feature Flags in Java with Conductor

Route execution based on feature flag status. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers .  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## The Problem

Feature flags let you route users to different code paths (new feature vs legacy) without redeploying. This workflow checks a flag's status for a specific user, routes execution to either the new feature path or the legacy path based on the result, and logs the flag usage for analytics. If the flag status is unknown, a safe default path is used.

Without orchestration, feature flag checks are scattered across application code with if/else blocks, making it hard to see which flags are active, how many users are on each path, and whether the new feature is performing better than the legacy one.

## The Solution

**You just write the flag-check, new-feature, legacy-path, and usage-logging workers. Conductor handles conditional path routing via SWITCH, per-evaluation retries, and usage tracking for every flag decision.**

Each worker represents a service boundary. Conductor manages cross-service orchestration, compensating transactions, timeout enforcement, and distributed tracing .  your workers just make the service calls.

### What You Write: Workers

Five workers handle flag evaluation and routing: CheckFlagWorker evaluates flag status, NewFeatureWorker and LegacyPathWorker implement the two code paths, DefaultPathWorker serves as a safe fallback, and LogUsageWorker records analytics.

| Worker | Task | What It Does |
|---|---|---|
| **CheckFlagWorker** | `ff_check_flag` | Evaluates a feature flag for a specific user and returns the flag status (enabled/disabled) and rollout percentage. |
| **DefaultPathWorker** | `ff_default_path` | Executes a safe default path when the flag status is unknown. |
| **LegacyPathWorker** | `ff_legacy_path` | Executes the legacy code path when the flag is disabled (e.g., renders v1 UI). |
| **LogUsageWorker** | `ff_log_usage` | Logs the flag evaluation result for analytics and A/B test tracking. |
| **NewFeatureWorker** | `ff_new_feature` | Executes the new feature code path when the flag is enabled (e.g., renders v2 UI). |

Workers simulate service calls with realistic request/response shapes so you can see the coordination pattern without running the full service mesh. Replace with real HTTP clients .  the workflow coordination stays the same.

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
ff_check_flag
    │
    ▼
SWITCH (ff_switch_ref)
    ├── enabled: ff_new_feature
    ├── disabled: ff_legacy_path
    └── default: ff_default_path
    │
    ▼
ff_log_usage
```

## Example Output

```
=== Example 298: Feature Flags ===

Step 1: Registering task definitions...
  Registered: ff_check_flag, ff_new_feature, ff_legacy_path, ff_default_path, ff_log_usage

Step 2: Registering workflow 'feature_flags_298'...
  Workflow registered.

Step 3: Starting workers...
  5 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [check] Flag \"" + featureName + "\" for user
  [default] Unknown flag status, using default path...
  [legacy] Executing legacy path for \"" + feature + "\"...
  [log] Logged flag usage:
  [new] Executing new feature path for \"" + feature + "\"...

  Status: COMPLETED
  Output: {flagStatus=..., rolloutPercent=..., source=..., path=...}

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
java -jar target/feature-flags-1.0.0.jar
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
java -jar target/feature-flags-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow feature_flags_298 \
  --version 1 \
  --input '{"userId": "user-100", "user-100": "featureName", "featureName": "new-checkout-ui", "new-checkout-ui": "sample-new-checkout-ui"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w feature_flags_298 -s COMPLETED -c 5
```

## How to Extend

Point each worker at your real flag evaluation SDK (LaunchDarkly, Unleash), feature code paths, and analytics pipeline, the check-route-log workflow stays exactly the same.

- **CheckFlagWorker** (`ff_check_flag`): evaluate flags via LaunchDarkly, Unleash, or Flagsmith SDK
- **DefaultPathWorker** (`ff_default_path`): execute the safe default code path when flag status is indeterminate
- **LegacyPathWorker** (`ff_legacy_path`): route to the existing legacy code path or v1 service endpoint

Wiring CheckFlagWorker to a real flag SDK changes nothing in the check-route-log workflow definition.

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
feature-flags/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/featureflags/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── FeatureFlagsExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CheckFlagWorker.java
│       ├── DefaultPathWorker.java
│       ├── LegacyPathWorker.java
│       ├── LogUsageWorker.java
│       └── NewFeatureWorker.java
└── src/test/java/featureflags/workers/
    ├── CheckFlagWorkerTest.java        # 4 tests
    ├── DefaultPathWorkerTest.java        # 2 tests
    ├── LegacyPathWorkerTest.java        # 2 tests
    ├── LogUsageWorkerTest.java        # 2 tests
    └── NewFeatureWorkerTest.java        # 2 tests
```
