# Gaming Matchmaking in Java Using Conductor

Matches players into a fair, balanced game session: searching the player pool, rating skill levels, creating a balanced match, provisioning a lobby, and starting the game. Uses [Conductor](https://github.

## The Problem

You need to match players into a fair, balanced game session. The workflow searches for players queuing in the same game mode and region, evaluates each player's skill rating (ELO, MMR, Glicko), finds a balanced match that minimizes skill disparity, creates a game lobby with the matched players, and starts the session. Unbalanced matches frustrate both sides .  skilled players get bored, new players get destroyed.

Without orchestration, you'd build a matchmaking service with a queue poller, skill-rating lookup, matching algorithm, lobby creation, and session start .  manually managing queue timeouts, handling players who disconnect during matchmaking, expanding search criteria when wait times are too long, and logging match quality metrics.

## The Solution

**You just write the player search, skill rating, match balancing, lobby creation, and game start logic. Conductor handles queue retries, match formation timing, and matchmaking audit trails.**

Each matchmaking concern is a simple, independent worker .  a plain Java class that does one thing. Conductor takes care of executing them in order (search players, rate skill, match, create lobby, start), retrying if the player database is slow, tracking every matchmaking operation, and resuming from the last step if the process crashes. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Player profiling, queue management, match formation, and lobby creation workers each solve one piece of the matchmaking problem.

| Worker | Task | What It Does |
|---|---|---|
| **CreateLobbyWorker** | `gmm_create_lobby` | Creates a game lobby on a server for the matched players |
| **MatchWorker** | `gmm_match` | Creates a balanced match by grouping players with similar skill ratings and returns a match ID |
| **RateSkillWorker** | `gmm_rate_skill` | Evaluates each candidate's MMR rating and returns a ranked list of players by skill |
| **SearchPlayersWorker** | `gmm_search_players` | Searches the player pool for candidates matching skill level and region |
| **StartWorker** | `gmm_start` | Starts the game session in the lobby and assigns the map |

Workers simulate game backend operations .  matchmaking, score processing, reward distribution ,  with realistic outputs. Replace with real game server and database integrations and the workflow stays the same.

### The Workflow

```
gmm_search_players
    │
    ▼
gmm_rate_skill
    │
    ▼
gmm_match
    │
    ▼
gmm_create_lobby
    │
    ▼
gmm_start

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
java -jar target/gaming-matchmaking-1.0.0.jar

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
java -jar target/gaming-matchmaking-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow gaming_matchmaking_741 \
  --version 1 \
  --input '{"playerId": "TEST-001", "gameMode": "standard", "region": "us-east-1"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w gaming_matchmaking_741 -s COMPLETED -c 5

```

## How to Extend

Point each worker at your real matchmaking systems .  your player pool service for candidate search, your ELO/MMR rating system for skill balancing, your game server for lobby provisioning, and the workflow runs identically in production.

- **Player searcher**: query your matchmaking queue (Redis sorted sets, custom queue service) for players in the same mode, region, and platform
- **Skill rater**: look up player MMR/ELO from your ranking database; apply confidence intervals (Glicko-2) for new players
- **Match maker**: implement matching algorithms that balance skill, ping, party size, and queue wait time with configurable fairness thresholds
- **Lobby creator**: provision a game server instance (AWS GameLift, Agones on K8s) and assign matched players to the lobby
- **Session starter**: signal the game server to begin the match and record the match ID for post-game analytics

Adjust your skill rating algorithm or queue settings and the matchmaking pipeline adapts with no definition changes.

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
gaming-matchmaking/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/gamingmatchmaking/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── GamingMatchmakingExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CreateLobbyWorker.java
│       ├── MatchWorker.java
│       ├── RateSkillWorker.java
│       ├── SearchPlayersWorker.java
│       └── StartWorker.java
└── src/test/java/gamingmatchmaking/workers/
    ├── SearchPlayersWorkerTest.java
    └── StartWorkerTest.java

```
