# Restaurant Management in Java with Conductor

Manages the full restaurant guest experience: locating a reservation, seating the party, taking a food order, preparing dishes in the kitchen, and generating the bill. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## The Problem

You need to manage the full restaurant guest experience from reservation to checkout. When a guest arrives, their reservation is located, they are seated at an appropriate table, their food order is taken and sent to the kitchen, the kitchen prepares the dishes, and the guest checks out with the bill. A breakdown at any step. lost reservation, wrong table, kitchen miscommunication, billing error,  degrades the dining experience.

Without orchestration, you'd coordinate between the host stand, waitstaff, kitchen, and cashier manually. tracking reservations on paper, yelling orders to the kitchen, managing table turns in the host's head, and hoping the POS system correctly captures every item ordered.

## The Solution

**You just write the reservation lookup, seating, order taking, kitchen preparation, and billing logic. Conductor handles scheduling retries, inventory coordination, and operational audit trails.**

Each restaurant operation is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of executing the guest flow (reservations, seating, order, kitchen, checkout), tracking every guest's experience through the restaurant, and resuming from the last step if the process crashes. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Staff scheduling, inventory ordering, revenue tracking, and compliance workers each address one operational concern of running a restaurant.

| Worker | Task | What It Does |
|---|---|---|
| **CheckoutWorker** | `rst_checkout` | Generates the bill for the table with subtotal, tax, and total |
| **KitchenWorker** | `rst_kitchen` | Prepares the ordered items in the kitchen and returns cook time |
| **OrderWorker** | `rst_order` | Takes the food order at the table and returns an order ID, items (e.g., Steak, Pasta, Wine), and total |
| **ReservationsWorker** | `rst_reservations` | Looks up the guest's reservation by name and party size and returns the confirmed booking |
| **SeatingWorker** | `rst_seating` | Assigns a table and section (e.g., patio) to the reservation and marks the party as seated |

Workers implement food service operations. order processing, kitchen routing, delivery coordination,  with realistic outputs. Replace with real POS and delivery integrations and the workflow stays the same.

### The Workflow

```
rst_reservations
    │
    ▼
rst_seating
    │
    ▼
rst_order
    │
    ▼
rst_kitchen
    │
    ▼
rst_checkout

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
java -jar target/restaurant-management-1.0.0.jar

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
java -jar target/restaurant-management-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow restaurant_management_732 \
  --version 1 \
  --input '{"guestName": "test", "partySize": 10}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w restaurant_management_732 -s COMPLETED -c 5

```

## How to Extend

Wire each worker to your real restaurant systems. your reservation platform for guest lookup, your POS for order taking and billing, your KDS for kitchen coordination, and the workflow runs identically in production.

- **Reservation handler**: integrate with your reservation platform (OpenTable, Resy) to look up and manage guest bookings
- **Seating manager**: assign tables based on party size, server sections, and table turn times using your floor management system
- **Order taker**: send orders to your POS (Toast, Square) and KDS with modifiers, allergies, and coursing instructions
- **Kitchen coordinator**: manage order queue, fire courses, and track preparation times via your KDS
- **Checkout handler**: generate bills, process payments (card, mobile pay, split checks), and trigger post-visit feedback surveys

Connect new vendor APIs or scheduling tools and the management workflows continue seamlessly.

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
restaurant-management/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/restaurantmanagement/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── RestaurantManagementExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CheckoutWorker.java
│       ├── KitchenWorker.java
│       ├── OrderWorker.java
│       ├── ReservationsWorker.java
│       └── SeatingWorker.java
└── src/test/java/restaurantmanagement/workers/
    ├── CheckoutWorkerTest.java
    └── ReservationsWorkerTest.java

```
