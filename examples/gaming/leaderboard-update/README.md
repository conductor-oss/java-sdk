# Leaderboard Update in Java Using Conductor

Updates a game's leaderboard for a season: collecting match scores, validating for integrity, ranking players, updating the public leaderboard, and broadcasting results to connected players. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.
## The Problem

You need to update a game's leaderboard for a season. The workflow collects scores from all completed matches, validates them for integrity (checking for cheating flags, impossible scores, or data corruption), ranks players by their validated scores, updates the public leaderboard, and broadcasts the updated rankings to all connected players. Posting invalid scores to the leaderboard undermines competitive integrity; stale leaderboards reduce player engagement.

Without orchestration, you'd build a batch leaderboard job that queries match results, filters invalid scores, sorts by rank, updates the leaderboard database, and pushes updates to clients. manually handling ties, pagination for large player bases, and real-time update latency.

## The Solution

**You just write the score collection, integrity validation, ranking, leaderboard update, and broadcast logic. Conductor handles score validation retries, ranking recalculation, and leaderboard audit trails.**

Each leaderboard concern is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of executing them in order (collect, validate, rank, update, broadcast), retrying if the leaderboard service is slow, tracking every leaderboard update cycle, and resuming from the last step if the process crashes. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Score validation, ranking calculation, leaderboard update, and notification workers each handle one aspect of competitive standings.

| Worker | Task | What It Does |
|---|---|---|
| **BroadcastWorker** | `lbu_broadcast` | Broadcasts the updated leaderboard with top-3 players to all connected clients |
| **CollectScoresWorker** | `lbu_collect_scores` | Collects match scores for all players in the game with player IDs and score values |
| **RankWorker** | `lbu_rank` | Computes player rankings from validated scores, assigning rank positions |
| **UpdateWorker** | `lbu_update` | Writes the computed rankings to the leaderboard store for the current season |
| **ValidateWorker** | `lbu_validate` | Validates collected scores for integrity, filtering out flagged or impossible entries |

Workers implement game backend operations. matchmaking, score processing, reward distribution,  with realistic outputs. Replace with real game server and database integrations and the workflow stays the same.

### The Workflow

```
lbu_collect_scores
    │
    ▼
lbu_validate
    │
    ▼
lbu_rank
    │
    ▼
lbu_update
    │
    ▼
lbu_broadcast

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
java -jar target/leaderboard-update-1.0.0.jar

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
java -jar target/leaderboard-update-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow leaderboard_update_742 \
  --version 1 \
  --input '{"gameId": "TEST-001", "season": "sample-season"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w leaderboard_update_742 -s COMPLETED -c 5

```

## How to Extend

Connect each worker to your real leaderboard stack. your match results database for score collection, your ranking engine for position calculations, your live service for real-time player broadcasts, and the workflow runs identically in production.

- **Score collector**: pull match results from your game server database or match history API with time-range filtering
- **Score validator**: cross-check scores against anti-cheat verdicts, statistical outlier detection, and server-authoritative game state
- **Ranker**: implement ranking algorithms (dense rank, fractional rank) with tie-breaking rules; compute percentiles and tier boundaries
- **Leaderboard updater**: write rankings to your leaderboard store (Redis sorted sets, DynamoDB) with pagination support
- **Broadcaster**: push leaderboard updates to connected players via WebSocket, Firebase Realtime Database, or your game's notification system

Modify scoring formulas or ranking algorithms and the leaderboard pipeline handles them transparently.

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
leaderboard-update/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/leaderboardupdate/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── LeaderboardUpdateExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── BroadcastWorker.java
│       ├── CollectScoresWorker.java
│       ├── RankWorker.java
│       ├── UpdateWorker.java
│       └── ValidateWorker.java
└── src/test/java/leaderboardupdate/workers/
    ├── BroadcastWorkerTest.java
    └── CollectScoresWorkerTest.java

```
