# Travel Booking Workflow in Java with Conductor

A traveler books a flight from SFO to JFK, a hotel in Manhattan, and then the car rental fails. the rental company's API returns a 503 at 2 AM. Now they have a confirmed flight landing at JFK, a hotel room waiting in Midtown, and no way to get from the airport. The flight is non-refundable. The hotel has a 24-hour cancellation policy that expires in 6 hours. Nobody is awake to notice the partial failure, and the traveler finds out when they land and check their itinerary. This example uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate a multi-step booking pipeline, you write the booking logic, Conductor handles retries, failure routing, durability, and observability for free.

## The Multi-Step Booking Problem

A user wants to fly from SFO to JFK on April 15th. The system needs to search three airlines for available flights, compare them on price and duration, reserve the cheapest option, confirm the booking and issue an e-ticket, and email the complete itinerary, all in the right order. If the booking succeeds but confirmation fails, you have a reserved seat with no e-ticket. If the search returns stale results and the selected flight sells out before booking, you need to restart from comparison without losing the traveler's preferences.

Without orchestration, you'd build a monolithic booking script that mixes GDS queries, fare comparison logic, reservation API calls, and email sending, making it impossible to swap airlines, test fare comparison independently, or audit which search results led to which booking.

## The Solution

**You just write the flight search, fare comparison, booking, confirmation, and itinerary delivery logic. Conductor handles booking retries with idempotency, search-to-confirmation sequencing, and full trip audit trails.**

Each worker handles one travel operation. Conductor manages the booking pipeline, approval gates, policy enforcement, and itinerary tracking.

### What You Write: Workers

Five workers divide the booking flow. Search, compare, book, confirm, and itinerary delivery, so flight search logic stays separate from payment and ticketing.

| Worker | Task | What It Does | Real / Simulated |
|---|---|---|---|
| `SearchWorker` | `tvb_search` | Queries available flights for the given origin/destination/date, returning 3 options (United $450, Delta $420, AA $480) | Simulated |
| `CompareWorker` | `tvb_compare` | Evaluates search results and selects Delta flight DL-1234 at $420 as the best value | Simulated |
| `BookWorker` | `tvb_book` | Reserves the selected flight for the traveler and returns booking ID `BK-travel-booking` with confirmation code | Simulated |
| `ConfirmWorker` | `tvb_confirm` | Finalizes the reservation, issuing e-ticket `ET-travel-booking-2024` | Simulated |
| `ItineraryWorker` | `tvb_itinerary` | Assembles and sends the complete itinerary (flight, booking ref, e-ticket) to the traveler | Simulated |

Workers simulate travel operations: booking, approval, itinerary generation, with realistic outputs. Replace with real GDS and travel API integrations and the workflow stays the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically. Configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status.; no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
tvb_search
    |
    v
tvb_compare
    |
    v
tvb_book
    |
    v
tvb_confirm
    |
    v
tvb_itinerary
```

## Example Output

```
=== Example 545: Travel Booking ===

Step 1: Registering task definitions...
  Registered: tvb_search, tvb_compare, tvb_book, tvb_confirm, tvb_itinerary

Step 2: Registering workflow 'tvb_travel_booking'...
  Workflow registered.

Step 3: Starting workers...
  5 workers polling.

Step 4: Starting workflow...
  Workflow ID: aabf227b-f21d-129c-62aa-668f83babbcf

  [book] Booked flight for
  [compare] Compared flights. Delta best value
  [confirm] Booking BOOKING-001 confirmed
  [itinerary] Itinerary sent to
  [search] Searching flights

  Status: COMPLETED
  Output: {bookingId=BK-545, totalCost=420}

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
java -jar target/travel-booking-1.0.0.jar
```

### Option 3: Use the run script

```bash
./run.sh

# Or on a custom port:
CONDUCTOR_PORT=9090 ./run.sh

# Or pointing at an existing Conductor:
CONDUCTOR_BASE_URL=http://localhost:9090/api ./run.sh
```

### Sample Output

```
=== Example 545: Travel Booking ===

  [search] Searching flights SFO -> JFK
  [compare] Compared flights. Delta best value
  [book] Booked flight for TRV-100
  [confirm] Booking BK-travel-booking confirmed
  [itinerary] Itinerary sent to TRV-100
  Status: COMPLETED
  Booking ID: BK-travel-booking
  Total cost: 420

Result: PASSED
```

## Configuration

| Environment Variable | Default | Description |
|---|---|---|
| `CONDUCTOR_BASE_URL` | `http://localhost:8080/api` | Conductor server URL |
| `CONDUCTOR_PORT` | `8080` | Host port for Conductor (Docker Compose only) |

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/travel-booking-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow tvb_travel_booking \
  --version 1 \
  --input '{"travelerId": "TRV-100", "origin": "SFO", "destination": "JFK", "departDate": "2024-04-15"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w tvb_travel_booking -s COMPLETED -c 5
```

## How to Extend

Swap each worker for real travel APIs. Amadeus or Sabre for flight search, your GDS for booking, SendGrid for itinerary emails, and the workflow runs identically in production.

- **`SearchWorker`**: Query a GDS (Amadeus, Sabre, Travelport) or airline NDC APIs for available flights matching the origin, destination, and travel dates.

- **`CompareWorker`**: Rank results by total cost, layover time, airline preference, and loyalty program benefits to find the best value option.

- **`BookWorker`**: Create the PNR (Passenger Name Record) in the GDS or airline's reservation system and return the booking reference.

- **`ConfirmWorker`**: Issue the e-ticket via the airline's ticketing API, charge the traveler's payment method, and return the ticket number.

- **`ItineraryWorker`**: Assemble the complete itinerary (flight details, booking ref, e-ticket, gate info) and deliver it via email or your corporate travel portal.

Connect real GDS and airline APIs and the booking pipeline runs without structural changes.

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
travel-booking-travel-booking/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/travelbooking/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── TravelBookingExample.java    # Main entry point (supports --workers mode)
│   └── workers/
│       ├── BookWorker.java          # Reserves selected flight, returns booking ID
│       ├── CompareWorker.java       # Selects best-value flight from search results
│       ├── ConfirmWorker.java       # Confirms booking and issues e-ticket
│       ├── ItineraryWorker.java     # Assembles and sends itinerary to traveler
│       └── SearchWorker.java        # Searches available flights across airlines
└── src/test/java/travelbooking/workers/
    ├── BookWorkerTest.java
    ├── CompareWorkerTest.java
    ├── ConfirmWorkerTest.java
    ├── ItineraryWorkerTest.java
    └── SearchWorkerTest.java
```
