# Anti Cheat in Java Using Conductor

Detects and acts on cheating in an online game: monitoring player behavior, running anomaly detection, and routing to clean/suspect/cheat outcomes via a SWITCH task. Uses [Conductor](https://github.

## The Problem

You need to detect and act on cheating in an online game. The workflow monitors a player's in-game behavior during a match, runs anomaly detection algorithms on metrics like aim accuracy, movement speed, and reaction time, and routes to one of three outcomes: clean (no action), suspect (flag for manual review), or confirmed cheat (ban or penalty). Failing to detect cheats ruins the experience for honest players; false positives unfairly punish legitimate players.

Without orchestration, you'd build a single anti-cheat service that collects telemetry, runs detection heuristics, makes verdicts, and applies penalties. manually tuning detection thresholds, handling appeals for false bans, and logging every decision to defend against community backlash.

## The Solution

**You just write the behavior monitoring, anomaly detection, and clean/suspect/cheat response logic. Conductor handles detection retries, verdict routing, and enforcement audit trails for every flagged session.**

Each anti-cheat concern is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of monitoring behavior, running anomaly detection, routing via a SWITCH task to the correct outcome (clean, suspect, cheat), applying the action, and tracking every detection decision with full evidence. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Telemetry collection, anomaly detection, cheat confirmation, and enforcement workers form an anti-cheat pipeline where each stage applies independent analysis.

| Worker | Task | What It Does |
|---|---|---|
| **ActWorker** | `ach_act` | Applies the final enforcement action (ban, warning, or clear) based on the detection verdict |
| **CheatWorker** | `ach_cheat` | Confirms cheating and issues a ban for the flagged player |
| **CleanWorker** | `ach_clean` | Records a clean verdict when no cheating is detected |
| **DetectAnomalyWorker** | `ach_detect_anomaly` | Runs anomaly detection algorithms on player metrics and returns a verdict (clean, suspect, or cheat) |
| **MonitorWorker** | `ach_monitor` | Collects in-game telemetry (aim accuracy, movement speed, reaction time) for a player during a match |
| **SuspectWorker** | `ach_suspect` | Flags a player for manual review by the trust and safety team |

Workers implement game backend operations. matchmaking, score processing, reward distribution,  with realistic outputs. Replace with real game server and database integrations and the workflow stays the same.

### The Workflow

```
ach_monitor
    │
    ▼
ach_detect_anomaly
    │
    ▼
SWITCH (switch_ref)
    ├── clean: ach_clean
    ├── suspect: ach_suspect
    └── default: ach_cheat
    │
    ▼
ach_act

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
java -jar target/anti-cheat-1.0.0.jar

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
java -jar target/anti-cheat-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow anti_cheat_746 \
  --version 1 \
  --input '{"playerId": "TEST-001", "matchId": "TEST-001"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w anti_cheat_746 -s COMPLETED -c 5

```

## How to Extend

Connect each worker to your real anti-cheat stack. your telemetry pipeline for behavior data, your ML models for anomaly detection, your game admin API for ban enforcement, and the workflow runs identically in production.

- **Behavior monitor**: collect in-game telemetry (aim vectors, input timing, position data) from your game server via real-time streaming
- **Anomaly detector**: run ML-based detection models (neural networks trained on labeled cheat/clean datasets) or statistical heuristics for known cheat patterns
- **Clean handler**: record clean verdicts for building training datasets and player trust scores
- **Suspect handler**: flag for manual review by your trust & safety team; restrict matchmaking pending investigation
- **Action enforcer**: apply penalties (temporary ban, rank reset, hardware ban) via your game backend and notify the player

Update your detection algorithms or enforcement policies and the anti-cheat pipeline adjusts without restructuring.

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
anti-cheat/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/anticheat/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── AntiCheatExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ActWorker.java
│       ├── CheatWorker.java
│       ├── CleanWorker.java
│       ├── DetectAnomalyWorker.java
│       ├── MonitorWorker.java
│       └── SuspectWorker.java
└── src/test/java/anticheat/workers/
    ├── ActWorkerTest.java
    └── DetectAnomalyWorkerTest.java

```
