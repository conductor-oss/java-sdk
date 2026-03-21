# Player Progression in Java Using Conductor

Processes player progression after completing a quest: recording the completion, awarding XP, checking for level-up, unlocking level rewards (items, abilities, titles), and notifying the player. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.
## The Problem

You need to process a player's progression after completing a quest or challenge. The player completes a task, earns experience points (XP), the system checks whether they have leveled up, unlocks any rewards associated with the new level (items, abilities, titles), and notifies the player of their achievements. Awarding XP without checking for level-up means players miss their rewards; not notifying means they do not feel the satisfaction of progression.

Without orchestration, you'd handle progression in a single game server callback that grants XP, checks level thresholds, queries the reward table, updates the player's inventory, and sends a notification. manually handling concurrent quest completions, ensuring idempotent XP grants, and managing the cascade of unlocks when a player levels up multiple times.

## The Solution

**You just write the quest completion, XP awarding, level-up checking, reward unlocking, and player notification logic. Conductor handles XP calculation retries, level-up sequencing, and progression audit trails.**

Each progression concern is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of executing them in order (complete task, award XP, check level, unlock rewards, notify), retrying if the player database is slow, tracking every progression event, and resuming from the last step if the process crashes. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

XP calculation, level evaluation, reward unlocking, and profile update workers each manage one dimension of player advancement.

| Worker | Task | What It Does |
|---|---|---|
| **AwardXpWorker** | `ppg_award_xp` | Awards XP to the player based on quest completion and returns the new total XP |
| **CheckLevelWorker** | `ppg_check_level` | Checks whether the player's total XP crosses a level threshold and determines if they leveled up |
| **CompleteTaskWorker** | `ppg_complete_task` | Records the player's quest completion and returns the quest name |
| **NotifyWorker** | `ppg_notify` | Sends a notification to the player with their progression summary and unlocked rewards |
| **UnlockRewardsWorker** | `ppg_unlock_rewards` | Unlocks level-up rewards (e.g., Gold Shield, Fire Spell, Title: Dragon Slayer) if the player leveled up |

Workers implement game backend operations. matchmaking, score processing, reward distribution,  with realistic outputs. Replace with real game server and database integrations and the workflow stays the same.

### The Workflow

```
ppg_complete_task
    │
    ▼
ppg_award_xp
    │
    ▼
ppg_check_level
    │
    ▼
ppg_unlock_rewards
    │
    ▼
ppg_notify

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
java -jar target/player-progression-1.0.0.jar

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
java -jar target/player-progression-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow player_progression_745 \
  --version 1 \
  --input '{"playerId": "TEST-001", "questId": "TEST-001", "xpEarned": "sample-xpEarned"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w player_progression_745 -s COMPLETED -c 5

```

## How to Extend

Point each worker at your real game services. your quest tracker for completion recording, your XP system for leveling, your inventory service for reward unlocks, and the workflow runs identically in production.

- **Task completer**: validate quest completion against your game server's authoritative state; prevent duplicate completions
- **XP awarder**: grant XP with multipliers (double XP events, premium pass bonuses) and update the player profile in your database
- **Level checker**: evaluate XP against level thresholds from your progression table; handle multi-level jumps
- **Reward unlocker**: grant level-up rewards (items, currency, cosmetics, abilities) to the player's inventory with catalog lookup
- **Notification sender**: send level-up and reward notifications via in-game toast, push notification, or social feed post

Adjust XP curves or reward tables and the progression pipeline remains structurally identical.

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
player-progression/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/playerprogression/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── PlayerProgressionExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AwardXpWorker.java
│       ├── CheckLevelWorker.java
│       ├── CompleteTaskWorker.java
│       ├── NotifyWorker.java
│       └── UnlockRewardsWorker.java
└── src/test/java/playerprogression/workers/
    ├── AwardXpWorkerTest.java
    └── NotifyWorkerTest.java

```
