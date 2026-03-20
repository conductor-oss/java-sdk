# Tournament Bracket in Java Using Conductor

Runs a competitive tournament from registration to finals: accepting registrations, seeding by skill, generating brackets, managing match rounds, and finalizing results with prize distribution. Uses [Conductor](https://github.## The Problem

You need to run a competitive tournament from registration to finals. The workflow accepts player registrations, seeds participants based on skill rating, generates the tournament bracket (single elimination, double elimination, or round robin), manages each round of matches, and finalizes results with prize distribution. Unseeded brackets produce lopsided first-round matchups; mismanaged rounds can deadlock the tournament.

Without orchestration, you'd build a tournament platform that manages registration, generates brackets, coordinates match scheduling, records results, and advances winners .  manually handling disqualifications, byes for odd player counts, and the complex state management of a multi-round elimination bracket.

## The Solution

**You just write the registration, skill seeding, bracket generation, match round management, and prize distribution logic. Conductor handles bracket generation retries, match scheduling, and tournament progression tracking.**

Each tournament concern is a simple, independent worker .  a plain Java class that does one thing. Conductor takes care of executing them in order (register, seed, create bracket, manage rounds, finalize), tracking every tournament's complete state, and resuming from the last step if the process crashes. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Registration, seeding, bracket generation, and match scheduling workers each own one structural aspect of tournament organization.

| Worker | Task | What It Does |
|---|---|---|
| **CreateBracketWorker** | `tbk_create_bracket` | Generates the tournament bracket structure with round count and total matches based on the chosen format |
| **FinalizeWorker** | `tbk_finalize` | Declares the champion and runner-up, distributes prizes, and marks the tournament as completed |
| **ManageRoundsWorker** | `tbk_manage_rounds` | Manages each round of matches in the bracket, recording results and advancing winners |
| **RegisterWorker** | `tbk_register` | Registers players for the tournament and validates entry requirements |
| **SeedWorker** | `tbk_seed` | Seeds registered players by skill ranking for balanced bracket placement |

Workers simulate game backend operations .  matchmaking, score processing, reward distribution ,  with realistic outputs. Replace with real game server and database integrations and the workflow stays the same.

### The Workflow

```
tbk_register
    │
    ▼
tbk_seed
    │
    ▼
tbk_create_bracket
    │
    ▼
tbk_manage_rounds
    │
    ▼
tbk_finalize
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
java -jar target/tournament-bracket-1.0.0.jar
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
java -jar target/tournament-bracket-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow tournament_bracket_743 \
  --version 1 \
  --input '{"tournamentName": "test", "format": "test-value", "maxPlayers": "test-value"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w tournament_bracket_743 -s COMPLETED -c 5
```

## How to Extend

Connect each worker to your real tournament systems .  your registration platform for signups, your bracket engine for seeding, your match server for round management and prize distribution, and the workflow runs identically in production.

- **Registration handler**: accept registrations with eligibility checks (rank requirements, region restrictions) and manage waitlists for capacity limits
- **Seeder**: seed players using their MMR/ELO ratings with proper bye placement for non-power-of-2 player counts
- **Bracket creator**: generate bracket structures (single/double elimination, Swiss, round robin) using tournament libraries or custom algorithms
- **Round manager**: schedule matches, create game lobbies, record results, and advance winners through the bracket
- **Finalizer**: declare winners, distribute prizes (in-game currency, real-money, cosmetics), and publish final standings

Update seeding logic or bracket formats and the tournament pipeline handles them with no code changes.

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
tournament-bracket/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/tournamentbracket/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── TournamentBracketExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CreateBracketWorker.java
│       ├── FinalizeWorker.java
│       ├── ManageRoundsWorker.java
│       ├── RegisterWorker.java
│       └── SeedWorker.java
└── src/test/java/tournamentbracket/workers/
    ├── FinalizeWorkerTest.java
    └── RegisterWorkerTest.java
```
