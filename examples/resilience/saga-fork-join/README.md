# Implementing Saga with Parallel Booking via FORK/JOIN in Java with Conductor :  Hotel, Flight, and Car in Parallel

A Java Conductor workflow example demonstrating the saga pattern with parallel execution. booking hotel, flight, and car rentals simultaneously via FORK/JOIN, then checking all results and confirming or compensating based on whether all bookings succeeded.

## The Problem

You need to book a trip. hotel, flight, and car rental, and all three must succeed for the trip to be valid. The bookings are independent and can run in parallel for speed, but if any one fails after the others succeed, the successful ones must be cancelled. This is the saga pattern with parallel branches: fan out to book all three, collect results, and confirm or roll back.

Without orchestration, parallel booking means managing threads or async calls, collecting results, and implementing cancellation logic for each combination of partial successes. The code for "flight succeeded but car failed, so cancel flight and hotel" becomes a combinatorial nightmare.

## The Solution

**You just write the booking and cancellation logic per service. Conductor handles FORK/JOIN parallel execution across all booking services, result collection, confirm-or-compensate routing, retries per booking, and independent tracking of each parallel branch with timing and outcomes.**

Conductor's FORK/JOIN runs hotel, flight, and car booking workers in parallel. A check-results worker examines all outcomes. If all succeeded, a confirm-all worker finalizes the bookings. If any failed, the workflow routes to cancellation. Every parallel branch is tracked independently. you can see exactly which booking succeeded, which failed, and how long each took. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

BookHotelWorker, BookFlightWorker, and BookCarWorker run simultaneously via FORK/JOIN for maximum speed, CheckResultsWorker verifies all bookings succeeded, and ConfirmAllWorker finalizes the trip or triggers cancellation.

| Worker | Task | What It Does |
|---|---|---|
| **BookCarWorker** | `sfj_book_car` | Worker for sfj_book_car. Books a rental car and returns a deterministic booking ID. |
| **BookFlightWorker** | `sfj_book_flight` | Worker for sfj_book_flight. Books a flight and returns a deterministic booking ID. |
| **BookHotelWorker** | `sfj_book_hotel` | Worker for sfj_book_hotel. Books a hotel and returns a deterministic booking ID. |
| **CheckResultsWorker** | `sfj_check_results` | Worker for sfj_check_results. Verifies all booking IDs are present. |
| **ConfirmAllWorker** | `sfj_confirm_all` | Worker for sfj_confirm_all. Confirms the entire trip if all bookings are valid. |

Workers implement success and failure scenarios so you can observe the resilience pattern end-to-end. Swap in real service calls and the retry, compensation, and recovery behavior works identically.

### The Workflow

```
FORK_JOIN
    ├── sfj_book_hotel
    ├── sfj_book_flight
    └── sfj_book_car
    │
    ▼
JOIN (wait for all branches)
sfj_check_results
    │
    ▼
sfj_confirm_all

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
java -jar target/saga-fork-join-1.0.0.jar

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
java -jar target/saga-fork-join-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow saga_fork_join_demo \
  --version 1 \
  --input '{"tripId": "TEST-001"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w saga_fork_join_demo -s COMPLETED -c 5

```

## How to Extend

Each worker books one service. connect the hotel worker to your hotel reservation API, the flight worker to a GDS, the car worker to a rental provider, and the parallel-book-then-confirm-or-cancel workflow stays the same.

- **BookCarWorker** (`sfj_book_car`): integrate with car rental APIs (Enterprise, Hertz, Turo)
- **BookFlightWorker** (`sfj_book_flight`): integrate with flight booking APIs (Amadeus, Sabre, Skyscanner Partner API)
- **BookHotelWorker** (`sfj_book_hotel`): integrate with hotel booking APIs (Booking.com, Expedia Affiliate Network, direct hotel chain APIs)

Connect each booking worker to your real hotel, flight, and car rental APIs, and the parallel-book-then-confirm-or-cancel workflow runs in production unmodified.

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
saga-fork-join/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/sagaforkjoin/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── SagaForkJoinExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── BookCarWorker.java
│       ├── BookFlightWorker.java
│       ├── BookHotelWorker.java
│       ├── CheckResultsWorker.java
│       └── ConfirmAllWorker.java
└── src/test/java/sagaforkjoin/workers/
    ├── BookCarWorkerTest.java        # 5 tests
    ├── BookFlightWorkerTest.java        # 5 tests
    ├── BookHotelWorkerTest.java        # 5 tests
    ├── CheckResultsWorkerTest.java        # 9 tests
    └── ConfirmAllWorkerTest.java        # 9 tests

```
