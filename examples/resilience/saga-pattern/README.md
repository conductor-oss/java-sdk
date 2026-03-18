# Implementing Saga Pattern in Java with Conductor: Orchestrated Compensation for Distributed Trip Booking

A Java Conductor workflow example demonstrating the saga pattern. Booking a flight, reserving a hotel, and charging payment in sequence, with compensating transactions (cancel flight, cancel hotel, refund payment) that execute in reverse order when any step fails.

## The Problem

You need to book a trip as a distributed transaction across three independent services. Flight booking, hotel reservation, and payment. If the payment charge fails after the flight and hotel are booked, both must be cancelled. If the hotel reservation fails after the flight is booked, the flight must be cancelled. Each service has its own compensating action that must run in reverse order.

Without orchestration, saga compensation is implemented as deeply nested try/catch blocks. Each forward step must know about every previous step's undo operation. Adding a new step (e.g., travel insurance) means updating the compensation logic for every existing step. Testing all compensation paths requires simulating failures at each step.

### What Goes Wrong Without a Saga

Consider what happens when the payment step fails midway through a trip booking:

1. Hotel is reserved (HTL-TRIP-001 confirmed)
2. Flight is booked (FLT-TRIP-001 confirmed)
3. Payment is charged. **DECLINED**

Without compensation, the hotel and flight remain booked. The customer sees a "payment failed" error but the hotel holds a room and the airline holds a seat. The hotel charges a no-show fee, the flight seat is wasted, and the customer gets billed for a trip they never took.

The saga pattern solves this by defining a compensating action for every forward step. When payment fails, Conductor runs `cancel_flight` then `cancel_hotel` in reverse order. Undoing exactly the steps that completed.

## The Solution

**You just write the booking and compensation logic for each service. Conductor handles forward sequencing, SWITCH-based failure detection, reverse-order compensation execution, retries on each booking and cancellation step, and a full audit trail of every saga with its forward and rollback paths.**

Each forward step (book flight, reserve hotel, charge payment) and its compensation (cancel flight, cancel hotel, refund payment) are independent workers. Conductor runs the forward steps in sequence and, on failure, triggers the compensation workflow that runs undo steps in reverse order. Every step in both directions is tracked with full context. You get all of that for free, without writing a single line of orchestration code.

### What You Write: Workers

Six workers form the saga: ReserveHotelWorker, BookFlightWorker, and ChargePaymentWorker handle forward booking, while CancelHotelWorker, CancelFlightWorker, and RefundPaymentWorker execute compensating rollbacks in reverse order when any step fails.

| Worker | Task | What It Does | Real / Simulated |
|---|---|---|---|
| **BookFlightWorker** | `saga_book_flight` | Books a flight for the given tripId, returns a booking ID like `FLT-TRIP-001`. | Simulated |
| **CancelFlightWorker** | `saga_cancel_flight` | Compensation: cancels a previously booked flight using the tripId. Returns `{cancelled: true}`. | Simulated |
| **CancelHotelWorker** | `saga_cancel_hotel` | Compensation: cancels a previously reserved hotel using the tripId. Returns `{cancelled: true}`. | Simulated |
| **ChargePaymentWorker** | `saga_charge_payment` | Charges payment for the trip. When `shouldFail=true`, returns `{status: "failed"}` to trigger saga rollback. Otherwise returns `{status: "success", transactionId: "TXN-TRIP-001"}`. | Simulated |
| **RefundPaymentWorker** | `saga_refund_payment` | Compensation: refunds a previously charged payment. Returns `{refunded: true}`. Registered but not used in the current workflow (payment failure prevents a charge from existing). | Simulated |
| **ReserveHotelWorker** | `saga_reserve_hotel` | Reserves a hotel room for the given tripId, returns a reservation ID like `HTL-TRIP-001`. | Simulated |

Workers simulate success and failure scenarios so you can observe the resilience pattern end-to-end. Swap in real service calls and the retry, compensation, and recovery behavior works identically.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically. Configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status.; no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |
| **Conditional routing** | SWITCH tasks route execution to different paths based on worker output |

### The Workflow

```
saga_reserve_hotel
    |
    v
saga_book_flight
    |
    v
saga_charge_payment
    |
    v
SWITCH (check_payment_ref)
    |-- "failed": saga_cancel_flight -> saga_cancel_hotel -> TERMINATE(ROLLED_BACK)
    |-- default:  workflow completes with booking details
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
java -jar target/saga-pattern-1.0.0.jar
```

### Option 3: Use the run script

```bash
./run.sh

# Or on a custom port:
CONDUCTOR_PORT=9090 ./run.sh

# Or pointing at an existing Conductor:
CONDUCTOR_BASE_URL=http://localhost:9090/api ./run.sh
```

## Example Output

```
=== Saga Pattern: Orchestrated Compensation for Trip Booking ===

Step 1: Registering task definitions...
  Registered: saga_reserve_hotel, saga_book_flight, saga_charge_payment, saga_cancel_flight, saga_cancel_hotel, saga_refund_payment

Step 2: Registering workflow 'trip_booking_saga'...
  Workflow registered.

Step 3: Starting workers...
  6 workers polling.

--- Scenario 1: Successful trip booking ---
  Workflow ID: 85a1c3e0-...
  [reserve_hotel] Reserving hotel for trip TRIP-001
  [book_flight] Booking flight for trip TRIP-001
  [charge_payment] Payment succeeded for trip TRIP-001
  Status: COMPLETED
  Output: {tripId=TRIP-001, hotel=HTL-TRIP-001, flight=FLT-TRIP-001, payment=success, transactionId=TXN-TRIP-001}
  As expected: trip booked successfully with all services confirmed.

--- Scenario 2: Payment failure triggers saga rollback ---
  Workflow ID: 92b4d7f1-...
  [reserve_hotel] Reserving hotel for trip TRIP-002
  [book_flight] Booking flight for trip TRIP-002
  [charge_payment] Payment FAILED for trip TRIP-002
  [cancel_flight] Cancelling flight for trip TRIP-002 (compensation)
  [cancel_hotel] Cancelling hotel for trip TRIP-002 (compensation)
  Status: COMPLETED
  Output: {tripId=TRIP-002, status=ROLLED_BACK}
  As expected: payment failed, saga compensated by cancelling flight and hotel.

Key insight: The saga pattern uses orchestrated compensation --
when a step fails, the workflow runs compensating tasks in reverse order
to undo the effects of previously completed steps.

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
java -jar target/saga-pattern-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
# Happy path: all services succeed
conductor workflow start \
  --workflow trip_booking_saga \
  --version 1 \
  --input '{"tripId": "TRIP-101", "shouldFail": false}'

# Failure path: payment fails, triggers compensation
conductor workflow start \
  --workflow trip_booking_saga \
  --version 1 \
  --input '{"tripId": "TRIP-102", "shouldFail": true}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w trip_booking_saga -s COMPLETED -c 5
```

## How to Extend

Each worker maps to a real booking service. Connect the flight worker to Amadeus or Sabre GDS, the hotel worker to your reservation system, the payment worker to Stripe, and the saga with compensating transactions stays the same.

- **BookFlightWorker** (`saga_book_flight`): book real flights via Amadeus/Sabre APIs, returning a PNR for cancellation
- **CancelFlightWorker** (`saga_cancel_flight`): cancel the flight booking via Amadeus/Sabre APIs using the PNR returned by the booking step
- **CancelHotelWorker** (`saga_cancel_hotel`): cancel the hotel reservation via the hotel booking API using the confirmation number from the reservation step
- **Add a new step**: e.g., travel insurance: add `BookInsuranceWorker` and `CancelInsuranceWorker`, insert the forward task in the workflow, add the compensation task to the rollback branch. No existing workers change.

Connect each booking worker to your real GDS, hotel reservation system, and payment gateway, and the saga with compensating transactions operates in production unmodified.

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
saga-pattern/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/sagapattern/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── SagaPatternExample.java      # Main entry point (supports --workers mode)
│   └── workers/
│       ├── BookFlightWorker.java
│       ├── CancelFlightWorker.java
│       ├── CancelHotelWorker.java
│       ├── ChargePaymentWorker.java
│       ├── RefundPaymentWorker.java
│       └── ReserveHotelWorker.java
└── src/test/java/sagapattern/workers/
    ├── BookFlightWorkerTest.java     # 4 tests
    ├── CancelFlightWorkerTest.java   # 4 tests
    ├── CancelHotelWorkerTest.java    # 4 tests
    ├── ChargePaymentWorkerTest.java  # 5 tests
    ├── RefundPaymentWorkerTest.java  # 4 tests
    └── ReserveHotelWorkerTest.java   # 4 tests
```
