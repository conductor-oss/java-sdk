# Hotel Booking in Java with Conductor

Hotel booking: search, filter, book, confirm, reminder. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers .## The Problem

You need to book a hotel for a business traveler. Searching available hotels in the destination city for the travel dates, filtering results by company travel policy (maximum nightly rate, preferred chains, required amenities), reserving the selected hotel, confirming the booking with the hotel, and scheduling a check-in reminder for the traveler. Each step depends on the previous one's output.

If the booking succeeds but confirmation fails, the hotel may release the room and the traveler arrives with no reservation. If filtering removes all results because the policy rate cap is too low for that city, the traveler needs to know immediately so they can request a policy exception. Without orchestration, you'd build a monolithic hotel handler that mixes hotel API queries, policy rule evaluation, reservation management, and notification scheduling. Making it impossible to update policy rules, swap hotel providers, or track which policy filters drove which booking decisions.

## The Solution

**You just write the hotel search, policy filtering, reservation, confirmation, and reminder scheduling logic. Conductor handles rate comparison retries, reservation sequencing, and booking audit trails.**

SearchWorker queries hotel availability in the destination city for the check-in and check-out dates. FilterWorker applies the company's travel policy. Maximum nightly rate, preferred hotel chains, required amenities (Wi-Fi, breakfast), and removes non-compliant options. BookWorker reserves the best matching hotel and returns a reservation ID. ConfirmWorker finalizes the booking with the hotel and obtains the confirmation number. ReminderWorker schedules a check-in reminder notification for the traveler before their arrival date. Each worker is a standalone Java class. Conductor handles the sequencing, retries, and crash recovery.

### What You Write: Workers

Availability search, rate comparison, reservation, and confirmation workers each handle one step of securing hotel accommodations.

| Worker | Task | What It Does |
|---|---|---|
| **BookWorker** | `htl_book` | Books the input and computes reservation id, confirmation code |
| **ConfirmWorker** | `htl_confirm` | Confirm. Computes and returns confirmed |
| **FilterWorker** | `htl_filter` | Filtered hotels by policy compliance |
| **ReminderWorker** | `htl_reminder` | Schedules a check-in reminder notification for the guest |
| **SearchWorker** | `htl_search` | Searching hotels in |

Workers simulate travel operations .  booking, approval, itinerary generation ,  with realistic outputs. Replace with real GDS and travel API integrations and the workflow stays the same.

### The Workflow

```
htl_search
    │
    ▼
htl_filter
    │
    ▼
htl_book
    │
    ▼
htl_confirm
    │
    ▼
htl_reminder
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
java -jar target/hotel-booking-1.0.0.jar
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
java -jar target/hotel-booking-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow htl_hotel_booking \
  --version 1 \
  --input '{"travelerId": "TEST-001", "city": "test-value", "checkIn": "test-value", "checkOut": "test-value"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w htl_hotel_booking -s COMPLETED -c 5
```

## How to Extend

Point each worker at real hotel services. Booking.com or Expedia APIs for availability search, your travel policy engine for filtering, the hotel's reservation API for booking, and the workflow runs identically in production.

- **SearchWorker** (`htl_search`): query hotel aggregator APIs (Booking.com, Hotels.com, Expedia) or GDS hotel segments for availability in the destination city and dates
- **FilterWorker** (`htl_filter`): apply your company's travel policy rules from your TMS (SAP Concur, Navan). Maximum nightly rate by city tier, preferred chain agreements, and required amenities
- **BookWorker** (`htl_book`): create the reservation via the hotel chain's booking API or through your corporate hotel program, applying negotiated corporate rates
- **ConfirmWorker** (`htl_confirm`): verify the reservation with the hotel's confirmation system and store the confirmation number in your travel management platform
- **ReminderWorker** (`htl_reminder`): schedule a check-in reminder via email, push notification, or calendar invite for the day before arrival

Switch hotel aggregators or payment methods and the booking pipeline adjusts seamlessly.

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
hotel-booking-hotel-booking/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/hotelbooking/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── HotelBookingExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── BookWorker.java
│       ├── ConfirmWorker.java
│       ├── FilterWorker.java
│       ├── ReminderWorker.java
│       └── SearchWorker.java
└── src/test/java/hotelbooking/workers/
    ├── BookWorkerTest.java        # 2 tests
    ├── ConfirmWorkerTest.java        # 2 tests
    ├── FilterWorkerTest.java        # 2 tests
    ├── ReminderWorkerTest.java        # 2 tests
    └── SearchWorkerTest.java        # 2 tests
```
