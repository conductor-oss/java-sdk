# Feature Flag Rollout in Java with Conductor

Manages feature flag lifecycle: create flag, staged rollout, monitor impact, and full activation or rollback. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers .## The Problem

Rolling out a feature flag involves creating the flag, enabling it for a target user segment at a specified percentage, monitoring its impact on key metrics (conversion rate, error rate), and then deciding whether to fully activate or roll back. Each step depends on the previous one. You cannot monitor impact until the flag is enabled for a segment.

Without orchestration, feature flag rollouts are ad-hoc: an engineer creates a flag in LaunchDarkly or a config file, manually checks dashboards after a while, and makes a gut-call about full activation. There is no structured lifecycle, no automatic monitoring window, and no rollback trigger.

## The Solution

**You just write the flag creation, segment targeting, impact monitoring, and rollout workers. Conductor handles staged flag lifecycle execution, durable monitoring windows, and a complete rollout audit trail.**

Each worker represents a service boundary. Conductor manages cross-service orchestration, compensating transactions, timeout enforcement, and distributed tracing .  your workers just make the service calls.

### What You Write: Workers

Four workers manage the flag lifecycle: CreateFlagWorker provisions the flag, EnableSegmentWorker targets a user segment, MonitorImpactWorker measures conversion and error rates, and FullRolloutWorker promotes to 100% or rolls back.

| Worker | Task | What It Does |
|---|---|---|
| **CreateFlagWorker** | `ff_create_flag` | Creates a new feature flag in the flag management system and returns its ID. |
| **EnableSegmentWorker** | `ff_enable_segment` | Enables the flag for a target user segment at a specified rollout percentage. |
| **FullRolloutWorker** | `ff_full_rollout` | Fully activates the flag for all users if monitoring shows healthy metrics, or rolls back if not. |
| **MonitorImpactWorker** | `ff_monitor_impact` | Monitors the flag's impact on conversion rate and error rate during a timed window. |

Workers simulate service calls with realistic request/response shapes so you can see the coordination pattern without running the full service mesh. Replace with real HTTP clients .  the workflow coordination stays the same.

### The Workflow

```
ff_create_flag
    │
    ▼
ff_enable_segment
    │
    ▼
ff_monitor_impact
    │
    ▼
ff_full_rollout
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
java -jar target/feature-flag-rollout-1.0.0.jar
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
java -jar target/feature-flag-rollout-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow feature_flag_rollout \
  --version 1 \
  --input '{"flagName": "test", "targetSegment": "test-value", "rolloutPercentage": "test-value"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w feature_flag_rollout -s COMPLETED -c 5
```

## How to Extend

Connect each worker to your real feature flag provider (LaunchDarkly, Unleash, Flagsmith) and analytics dashboard, the create-enable-monitor-rollout lifecycle workflow stays exactly the same.

- **CreateFlagWorker** (`ff_create_flag`): create the flag in LaunchDarkly, Unleash, Flagsmith, or your own feature-flag service
- **EnableSegmentWorker** (`ff_enable_segment`): configure segment targeting and percentage rollout via your flag provider's API
- **FullRolloutWorker** (`ff_full_rollout`): set the flag to 100% in your flag provider (LaunchDarkly, Unleash) or roll back by disabling it

Connecting to LaunchDarkly or Unleash instead of a simulated flag store preserves the create-enable-monitor-rollout pipeline.

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
feature-flag-rollout/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/featureflagrollout/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── FeatureFlagRolloutExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CreateFlagWorker.java
│       ├── EnableSegmentWorker.java
│       ├── FullRolloutWorker.java
│       └── MonitorImpactWorker.java
```
