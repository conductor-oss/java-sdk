# Reservation System in Java with Conductor

Manages restaurant reservations end-to-end: checking table availability, booking, sending confirmation and reminder, and seating the party on arrival. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## The Problem

You need to manage restaurant reservations from booking to seating. The workflow checks table availability for the requested date, time, and party size, creates the reservation, sends a confirmation to the guest, sends a reminder before the reservation, and seats the party when they arrive. Double-booking a table ruins the dining experience; forgetting to send reminders leads to no-shows.

Without orchestration, you'd build a single reservation service that queries availability, inserts bookings, sends confirmation emails, schedules reminder jobs, and updates table status. manually handling overlapping reservations, cancellations, waitlist management, and the timing of reminder notifications.

## The Solution

**You just write the availability check, booking, confirmation, reminder, and seating logic. Conductor handles availability retries, table assignment, and reservation lifecycle tracking.**

Each reservation concern is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of executing them in order (check availability, book, confirm, remind, seat), retrying if the notification service is unavailable, tracking every reservation from booking to seating, and resuming from the last step if the process crashes. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Availability checking, table assignment, confirmation, and reminder workers handle restaurant reservations as a sequence of independent steps.

| Worker | Task | What It Does |
|---|---|---|
| **BookWorker** | `rsv_book` | Creates the reservation for the guest and returns a reservation ID |
| **CheckAvailabilityWorker** | `rsv_check_availability` | Checks table availability for the requested date, time, and party size, returning the table ID and section |
| **ConfirmWorker** | `rsv_confirm` | Sends a confirmation to the guest and returns a confirmation code |
| **RemindWorker** | `rsv_remind` | Sends a reminder to the guest before the reservation via SMS |
| **SeatWorker** | `rsv_seat` | Seats the party at the assigned table and marks the reservation as seated |

Workers implement food service operations. order processing, kitchen routing, delivery coordination,  with realistic outputs. Replace with real POS and delivery integrations and the workflow stays the same.

### The Workflow

```
rsv_check_availability
    │
    ▼
rsv_book
    │
    ▼
rsv_confirm
    │
    ▼
rsv_remind
    │
    ▼
rsv_seat

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
java -jar target/reservation-system-1.0.0.jar

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
java -jar target/reservation-system-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow reservation_system_736 \
  --version 1 \
  --input '{"guestName": "test", "date": "2026-01-01T00:00:00Z", "time": "2026-01-01T00:00:00Z", "partySize": 10}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w reservation_system_736 -s COMPLETED -c 5

```

## How to Extend

Connect each worker to your real reservation stack. OpenTable or Resy for availability, your notification service for confirmations and reminders, your host stand system for seating, and the workflow runs identically in production.

- **Availability checker**: query your reservation system (OpenTable, Resy, custom table management) for available time slots and table configurations
- **Booking handler**: create the reservation with party size, seating preferences, and special requests (high chair, wheelchair accessible)
- **Confirmation sender**: send confirmation via SMS (Twilio) or email (SendGrid) with date, time, and cancellation policy
- **Reminder sender**: send reminders 24 hours and 2 hours before the reservation; offer easy cancellation/modification links
- **Seating handler**: update table status in your floor management system when the party arrives; handle early/late arrivals and walk-in overflow

Replace your table management system or notification service and the reservation flow persists unchanged.

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
reservation-system/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/reservationsystem/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ReservationSystemExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── BookWorker.java
│       ├── CheckAvailabilityWorker.java
│       ├── ConfirmWorker.java
│       ├── RemindWorker.java
│       └── SeatWorker.java
└── src/test/java/reservationsystem/workers/
    ├── CheckAvailabilityWorkerTest.java
    └── SeatWorkerTest.java

```
