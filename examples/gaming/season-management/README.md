# Season Management in Java Using Conductor

Manages a competitive season lifecycle: creating the season with a theme, defining reward tiers and battle pass structure, launching to all players, tracking progress, and closing with final reward distribution. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers ,  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## The Problem

You need to manage a competitive season lifecycle in your game. The workflow creates a new season with a theme and duration, defines the reward tiers and battle pass structure, launches the season to all players, tracks progress and engagement throughout, and closes the season with final reward distribution. Launching without properly defined rewards means players have nothing to earn; not closing properly means lingering rewards and confused players.

Without orchestration, you'd manage seasons through a mix of database scripts, admin panel updates, deployment pipelines, and scheduled jobs .  manually coordinating the launch across all platforms (iOS, Android, PC, console), tracking season progress, and ensuring rewards are distributed correctly when the season ends.

## The Solution

**You just write the season creation, reward tier definition, player launch, progress tracking, and final reward distribution logic. Conductor handles reward distribution retries, progress tracking, and season lifecycle audit trails.**

Each season concern is a simple, independent worker .  a plain Java class that does one thing. Conductor takes care of executing them in order (create, define rewards, launch, track, close), retrying if a deployment fails, tracking every season's lifecycle, and resuming from the last step if the process crashes. You get all of that for free, without writing a single line of orchestration code.

### What You Write: Workers

Season creation, reward track setup, progress tracking, and season closure workers handle competitive seasons as discrete lifecycle phases.

| Worker | Task | What It Does |
|---|---|---|
| **CloseWorker** | `smg_close` | Closes the season, distributes final rewards to participants, and returns total player and reward stats |
| **CreateSeasonWorker** | `smg_create_season` | Creates a new season with the given number and theme, and assigns a season ID |
| **DefineRewardsWorker** | `smg_define_rewards` | Defines the reward tiers and unlockable items for the season pass |
| **LaunchWorker** | `smg_launch` | Launches the season to all players and records the launch date |
| **TrackWorker** | `smg_track` | Tracks season progress including active players, average level, top level, and pass holders |

Workers simulate game backend operations .  matchmaking, score processing, reward distribution ,  with realistic outputs. Replace with real game server and database integrations and the workflow stays the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
smg_create_season
    │
    ▼
smg_define_rewards
    │
    ▼
smg_launch
    │
    ▼
smg_track
    │
    ▼
smg_close
```

## Example Output

```
=== Example 749: Season Management ===

Step 1: Registering task definitions...
  Registered: smg_create_season, smg_define_rewards, smg_launch, smg_track, smg_close

Step 2: Registering workflow 'season_management_749'...
  Workflow registered.

Step 3: Starting workers...
  5 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [close] Season
  [create] Creating Season
  [rewards] Defining reward tiers for
  [launch] Season
  [track] Tracking season

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
java -jar target/season-management-1.0.0.jar
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
java -jar target/season-management-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow season_management_749 \
  --version 1 \
  --input '{"seasonNumber": 5, "theme": "sample-theme", "Frozen Frontier": "sample-Frozen Frontier", "durationWeeks": "sample-durationWeeks"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w season_management_749 -s COMPLETED -c 5
```

## How to Extend

Swap each worker for your real season systems .  your content management for themes and tiers, your game server for season launch, your reward engine for end-of-season distribution, and the workflow runs identically in production.

- **Season creator**: set up the season in your game backend with theme, duration, matchmaking pools, and rank reset rules
- **Reward definer**: configure battle pass tiers, milestone rewards, and ranked rewards in your game config service
- **Launcher**: deploy season content across all platforms simultaneously via your CI/CD pipeline and feature flag system
- **Progress tracker**: monitor player engagement, tier progression, and completion rates via your analytics platform
- **Season closer**: distribute final rewards, archive season data, reset ranks, and prepare for the next season

Change reward structures or season durations and the management pipeline keeps working.

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
season-management/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/seasonmanagement/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── SeasonManagementExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CloseWorker.java
│       ├── CreateSeasonWorker.java
│       ├── DefineRewardsWorker.java
│       ├── LaunchWorker.java
│       └── TrackWorker.java
└── src/test/java/seasonmanagement/workers/
    ├── CloseWorkerTest.java
    └── CreateSeasonWorkerTest.java
```
