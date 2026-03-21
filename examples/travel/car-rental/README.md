# Car Rental in Java with Conductor

Car rental: search, select, book, pickup, return. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## The Problem

You need to manage a car rental for a business traveler. Searching available vehicles at the pickup location, selecting the right vehicle class (compact, midsize, SUV) based on traveler needs and company policy, booking the reservation, processing the vehicle pickup with documentation, and handling the vehicle return with final charges. Each step depends on the previous one's output.

If the booking succeeds but the selected vehicle class is unavailable at pickup, you need the reservation details to arrange an upgrade or alternate vehicle. If the return step fails to record mileage and fuel level, the final charges are wrong and the company disputes the invoice. Without orchestration, you'd build a monolithic rental handler that mixes fleet inventory queries, policy checks, reservation API calls, and return processing. Making it impossible to swap rental providers, test vehicle selection logic independently, or track which policy rules drove which vehicle class selection.

## The Solution

**You just write the vehicle search, class selection, booking, pickup inspection, and return processing logic. Conductor handles availability retries, reservation sequencing, and rental audit trails.**

SearchWorker queries available rental vehicles at the pickup location and dates. SelectWorker picks the best vehicle class based on the traveler's needs and company policy (midsize default, SUV for group travel). BookWorker reserves the selected vehicle and returns a reservation number. PickupWorker processes the vehicle pickup. Recording the odometer reading, fuel level, and damage inspection. ReturnWorker handles the vehicle return, calculates final charges (mileage, fuel, insurance), and closes the rental. Each worker is a standalone Java class. Conductor handles the sequencing, retries, and crash recovery.

### What You Write: Workers

Availability lookup, rate comparison, reservation, and pickup confirmation workers each manage one stage of the car rental booking process.

| Worker | Task | What It Does |
|---|---|---|
| **BookWorker** | `crl_book` | Books the input and computes reservation id, confirmation code |
| **PickupWorker** | `crl_pickup` | Vehicle picked up .  reservation |
| **ReturnWorker** | `crl_return` | Processes the vehicle return. Records the reservation as returned, calculates total cost, and captures ending mileage |
| **SearchWorker** | `crl_search` | Searching rentals at |
| **SelectWorker** | `crl_select` | Selected midsize vehicle |

Workers simulate travel operations .  booking, approval, itinerary generation ,  with realistic outputs. Replace with real GDS and travel API integrations and the workflow stays the same.

### The Workflow

```
crl_search
    │
    ▼
crl_select
    │
    ▼
crl_book
    │
    ▼
crl_pickup
    │
    ▼
crl_return

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
java -jar target/car-rental-1.0.0.jar

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
java -jar target/car-rental-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow crl_car_rental \
  --version 1 \
  --input '{"travelerId": "TEST-001", "location": "us-east-1", "pickupDate": "2026-01-01T00:00:00Z", "returnDate": "2026-01-01T00:00:00Z"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w crl_car_rental -s COMPLETED -c 5

```

## How to Extend

Connect each worker to real rental APIs. Enterprise or Hertz for vehicle search, your policy engine for class selection, the rental company's reservation system for booking, and the workflow runs identically in production.

- **SearchWorker** (`crl_search`): query rental company APIs (Enterprise, Hertz, Avis) or aggregators for available vehicles at the pickup location and dates
- **SelectWorker** (`crl_select`): apply company travel policy rules for vehicle class selection based on trip purpose, number of travelers, and corporate rate agreements
- **BookWorker** (`crl_book`): create the reservation via the rental company's booking API, applying corporate discount codes and loyalty program numbers
- **PickupWorker** (`crl_pickup`): integrate with the rental company's fleet management system to record vehicle checkout, odometer, and insurance selection
- **ReturnWorker** (`crl_return`): process the vehicle return, calculate final charges including fuel and mileage overages, and submit the invoice to your expense management system

Change rental providers or rate sources and the pipeline continues operating identically.

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
car-rental-car-rental/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/carrental/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── CarRentalExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── BookWorker.java
│       ├── PickupWorker.java
│       ├── ReturnWorker.java
│       ├── SearchWorker.java
│       └── SelectWorker.java
└── src/test/java/carrental/workers/
    ├── BookWorkerTest.java        # 2 tests
    ├── PickupWorkerTest.java        # 2 tests
    ├── ReturnWorkerTest.java        # 2 tests
    ├── SearchWorkerTest.java        # 2 tests
    └── SelectWorkerTest.java        # 2 tests

```
