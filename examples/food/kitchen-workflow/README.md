# Kitchen Workflow in Java with Conductor

Manages the kitchen flow for a restaurant order: receiving it from the POS, prepping ingredients, cooking, plating with garnishes, and serving to the table. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers ,  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## The Problem

You need to manage the kitchen workflow for a restaurant order .  from the moment it arrives in the kitchen to when it reaches the customer's table. The order is received from the POS system, ingredients are prepped (chopping, marinating, portioning), the dishes are cooked, plated with garnishes and presentation, and served to the table. Kitchen delays cascade ,  late prep means late cooking, which means cold plated food sitting under heat lamps.

Without orchestration, you'd manage kitchen flow with verbal communication and paper tickets .  manually tracking which orders are at which station, handling ticket pile-ups during rush hours, and coordinating timing so all dishes for a table come out together.

## The Solution

**You just write the order receiving, ingredient prep, cooking, plating, and table service logic. Conductor handles station handoff retries, prep sequencing, and kitchen order tracking.**

Each kitchen step is a simple, independent worker .  a plain Java class that does one thing. Conductor takes care of executing them in order (receive, prep, cook, plate, serve), tracking every order through the kitchen with timestamps at each station, and resuming from the last step if the process crashes. You get all of that for free, without writing a single line of orchestration code.

### What You Write: Workers

Order receipt, prep assignment, cooking, plating, and dispatch workers model the kitchen line as a series of handoffs between stations.

| Worker | Task | What It Does |
|---|---|---|
| **CookWorker** | `kit_cook` | Cooks the order, tracking cook time and internal temperature (e.g., 165F) |
| **PlateWorker** | `kit_plate` | Plates the order with garnish and presentation styling |
| **PrepWorker** | `kit_prep` | Preps ingredients (chopping, marinating, portioning) for the order and returns prep time |
| **ReceiveOrderWorker** | `kit_receive_order` | Receives the order in the kitchen, parses items (e.g., Salmon, Risotto, Salad), and assigns the station |
| **ServeWorker** | `kit_serve` | Serves the completed order to the table and records total time from receipt to service |

Workers simulate food service operations .  order processing, kitchen routing, delivery coordination ,  with realistic outputs. Replace with real POS and delivery integrations and the workflow stays the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
kit_receive_order
    │
    ▼
kit_prep
    │
    ▼
kit_cook
    │
    ▼
kit_plate
    │
    ▼
kit_serve
```

## Example Output

```
=== Example 735: Kitchen Workflow ===

Step 1: Registering task definitions...
  Registered: kit_receive_order, kit_prep, kit_cook, kit_plate, kit_serve

Step 2: Registering workflow 'kitchen_workflow_735'...
  Workflow registered.

Step 3: Starting workers...
  5 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [cook] Cooking order
  [plate] Plating order
  [prep] Prepping ingredients for order
  [receive] Order
  [serve] Serving order

  Status: COMPLETED

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
java -jar target/kitchen-workflow-1.0.0.jar
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
java -jar target/kitchen-workflow-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow kitchen_workflow_735 \
  --version 1 \
  --input '{"orderId": "ORD-735", "ORD-735": "tableId", "tableId": "T-5", "T-5": "items", "items": [{"name": "Widget A", "quantity": 2}, {"name": "Widget B", "quantity": 1}]}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w kitchen_workflow_735 -s COMPLETED -c 5
```

## How to Extend

Connect each worker to your real kitchen systems .  your KDS for order receipt, your prep station displays for ingredient tracking, your expo system for plating and quality checks, and the workflow runs identically in production.

- **Order receiver**: integrate with your POS system (Toast, Square, Clover) to receive orders with modifiers and special instructions
- **Prep station**: dispatch prep tasks to the appropriate stations (cold prep, hot prep) via KDS with estimated prep times
- **Cook station**: track cooking times per dish, coordinate multi-course timing, and alert when dishes are ready for plating
- **Plating station**: display plating instructions with photos and garnish requirements; signal when plates are ready for service
- **Server notifier**: alert the waitstaff via pager or app that the order is ready for table delivery

Reassign station logic or update prep procedures and the kitchen pipeline continues without modification.

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
kitchen-workflow/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/kitchenworkflow/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── KitchenWorkflowExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CookWorker.java
│       ├── PlateWorker.java
│       ├── PrepWorker.java
│       ├── ReceiveOrderWorker.java
│       └── ServeWorker.java
└── src/test/java/kitchenworkflow/workers/
    ├── ReceiveOrderWorkerTest.java
    └── ServeWorkerTest.java
```
