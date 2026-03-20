# Itinerary Planning in Java with Conductor

Itinerary planning: preferences, search, optimize, book, finalize. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers .  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## The Problem

You need to plan a complete travel itinerary for an employee .  loading their travel preferences (seat, airline, hotel chain, meal requirements), searching for flights and hotels that match, optimizing the combination for cost and convenience (minimizing layovers, grouping nearby hotels), booking the selected options, and finalizing the itinerary with all confirmation details sent to the traveler. Each step builds on the previous one's output.

If the optimization step selects a cheaper flight but the booking fails because the fare expired, you need to re-optimize without losing the hotel selection. If booking succeeds but finalization fails, the traveler has reservations but no consolidated itinerary document. Without orchestration, you'd build a monolithic planner that mixes preference lookups, GDS queries, optimization algorithms, and booking API calls .  making it impossible to swap search providers, test optimization logic independently, or track which preferences drove which booking decisions.

## The Solution

**You just write the preference loading, availability search, itinerary optimization, booking, and finalization logic. Conductor handles research retries, scheduling coordination, and itinerary version tracking.**

PreferencesWorker loads the traveler's saved preferences (preferred airlines, seat type, hotel chains, dietary needs). SearchWorker queries flight and hotel availability for the destination and dates, filtered by those preferences. OptimizeWorker ranks the options by cost and convenience .  minimizing total price, layover time, and distance from meeting venues. BookWorker reserves the selected flights and hotels. FinalizeWorker assembles the complete itinerary with all confirmation numbers, check-in times, and directions, then sends it to the traveler. Each worker is a standalone Java class. Conductor handles the sequencing, retries, and crash recovery.

### What You Write: Workers

Destination research, activity selection, scheduling, and itinerary assembly workers each own one aspect of building a coherent travel plan.

| Worker | Task | What It Does |
|---|---|---|
| **BookWorker** | `itp_book` | Flight and hotel booked |
| **FinalizeWorker** | `itp_finalize` | Finalizes the itinerary and sends the confirmed travel plan to the traveler |
| **OptimizeWorker** | `itp_optimize` | Optimized for cost and convenience |
| **PreferencesWorker** | `itp_preferences` | Loads the traveler's preferences (budget, travel style, dietary needs) |
| **SearchWorker** | `itp_search` | Searches for flights, hotels, and activities matching the trip criteria |

Workers simulate travel operations .  booking, approval, itinerary generation ,  with realistic outputs. Replace with real GDS and travel API integrations and the workflow stays the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
itp_preferences
    │
    ▼
itp_search
    │
    ▼
itp_optimize
    │
    ▼
itp_book
    │
    ▼
itp_finalize
```

## Example Output

```
=== Example 544: Itinerary Planning ===

Step 1: Registering task definitions...
  Registered: itp_preferences, itp_search, itp_optimize, itp_book, itp_finalize

Step 2: Registering workflow 'itp_itinerary_planning'...
  Workflow registered.

Step 3: Starting workers...
  5 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [book] Flight and hotel booked
  [finalize] Itinerary finalized and sent to
  [optimize] Optimized for cost and convenience
  [preferences] Loaded preferences for
  [search] Found options for

  Status: COMPLETED
  Output: {bookingIds=..., totalCost=..., itineraryId=..., finalized=...}

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
java -jar target/itinerary-planning-1.0.0.jar
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
java -jar target/itinerary-planning-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow itp_itinerary_planning \
  --version 1 \
  --input '{"travelerId": "TRV-200", "TRV-200": "destination", "destination": "Chicago", "Chicago": "days", "days": 3}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w itp_itinerary_planning -s COMPLETED -c 5
```

## How to Extend

Connect each worker to your real travel stack .  your traveler profile service for preferences, Amadeus for flight and hotel search, your booking engine for reservations, and the workflow runs identically in production.

- **PreferencesWorker** (`itp_preferences`): load traveler preferences from your travel management system (SAP Concur, Navan) or HR profile database
- **SearchWorker** (`itp_search`): query GDS APIs (Amadeus, Sabre) for flights and hotel aggregators (Booking.com, Hotels.com) for accommodations matching the dates and preferences
- **OptimizeWorker** (`itp_optimize`): run cost/convenience optimization using your travel policy rules, corporate rate agreements, and meeting location proximity
- **BookWorker** (`itp_book`): reserve the selected flights and hotels via GDS booking APIs, applying corporate discount codes and loyalty program numbers
- **FinalizeWorker** (`itp_finalize`): generate the consolidated itinerary PDF or calendar invites and deliver via email, Slack, or your corporate travel portal

Replace activity databases or scheduling algorithms and the planning pipeline keeps its structure.

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
itinerary-planning-itinerary-planning/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/itineraryplanning/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ItineraryPlanningExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── BookWorker.java
│       ├── FinalizeWorker.java
│       ├── OptimizeWorker.java
│       ├── PreferencesWorker.java
│       └── SearchWorker.java
└── src/test/java/itineraryplanning/workers/
    ├── BookWorkerTest.java        # 2 tests
    ├── FinalizeWorkerTest.java        # 2 tests
    ├── OptimizeWorkerTest.java        # 2 tests
    ├── PreferencesWorkerTest.java        # 2 tests
    └── SearchWorkerTest.java        # 2 tests
```
