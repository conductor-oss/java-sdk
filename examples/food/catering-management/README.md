# Catering Management in Java with Conductor

Orchestrates a catering order from client inquiry through menu planning, event execution, and invoicing. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers .  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## The Problem

You need to manage a catering order from initial inquiry to final invoice. A client reaches out with event details (date, guest count), a customized quote is prepared, the menu is planned based on dietary requirements and budget, the catering event is executed, and the final invoice is sent. Providing a quote without understanding guest count leads to cost overruns; executing without a planned menu results in food shortages or waste.

Without orchestration, you'd manage catering through spreadsheets and email .  manually tracking inquiry status, building quotes from templates, coordinating menu planning with kitchen staff, and chasing invoices after the event.

## The Solution

**You just write the client inquiry, menu planning, event execution, and invoicing logic. Conductor handles quote retries, event coordination sequencing, and catering engagement audit trails.**

Each catering concern is a simple, independent worker .  a plain Java class that does one thing. Conductor takes care of executing them in order (inquiry, quote, plan menu, execute, invoice), retrying if an external service fails, tracking every catering order's lifecycle, and resuming from the last step if the process crashes. You get all of that for free, without writing a single line of orchestration code.

### What You Write: Workers

Inquiry intake, menu planning, quoting, execution, and invoicing workers each manage one phase of a catering engagement.

| Worker | Task | What It Does |
|---|---|---|
| **ExecuteWorker** | `cat_execute` | Executes the catering event with menu, staff, and logistics coordination |
| **InquiryWorker** | `cat_inquiry` | Receives a catering inquiry with client name and guest count, and captures event requirements (type, dietary needs, service style) |
| **InvoiceWorker** | `cat_invoice` | Generates a final invoice with client name, total cost, and invoice ID, then marks it as sent |
| **PlanMenuWorker** | `cat_plan_menu` | Plans a menu for the given guest count and budget, selecting appetizers, mains, desserts, and beverages |
| **QuoteWorker** | `cat_quote` | Calculates a price quote at $45 per guest based on guest count and returns the total and per-guest rate |

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
cat_inquiry
    │
    ▼
cat_quote
    │
    ▼
cat_plan_menu
    │
    ▼
cat_execute
    │
    ▼
cat_invoice
```

## Example Output

```
=== Example 740: Catering Management ===

Step 1: Registering task definitions...
  Registered: cat_inquiry, cat_quote, cat_plan_menu, cat_execute, cat_invoice

Step 2: Registering workflow 'catering_management_740'...
  Workflow registered.

Step 3: Starting workers...
  5 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [execute] Executing catering for
  [inquiry] Catering inquiry from
  [invoice] Invoicing
  [menu] Planning menu for
  [quote] Quote: $

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
java -jar target/catering-management-1.0.0.jar
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
java -jar target/catering-management-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow catering_management_740 \
  --version 1 \
  --input '{"clientName": "sample-name", "Acme Corp": "sample-Acme Corp", "eventDate": "2025-01-15T10:00:00Z", "2026-04-15": "sample-2026-04-15", "guestCount": 5}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w catering_management_740 -s COMPLETED -c 5
```

## How to Extend

Connect each worker to your real catering systems .  your CRM for client inquiries, your menu planning tool, your event management platform, your invoicing system, and the workflow runs identically in production.

- **Inquiry handler**: capture client requirements in your CRM (Salesforce, HubSpot) and check kitchen capacity for the requested date
- **Quote generator**: calculate pricing based on menu selections, guest count, venue fees, and staffing using your catering cost model
- **Menu planner**: integrate with your recipe management system, check ingredient availability with suppliers, and accommodate dietary restrictions
- **Execution coordinator**: dispatch kitchen prep schedules, staffing assignments, and equipment checklists to your operations team
- **Invoice sender**: generate and send invoices via your billing system (QuickBooks, FreshBooks) with itemized line items

Switch your menu database or invoicing system and the catering pipeline retains its structure.

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
catering-management/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/cateringmanagement/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── CateringManagementExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ExecuteWorker.java
│       ├── InquiryWorker.java
│       ├── InvoiceWorker.java
│       ├── PlanMenuWorker.java
│       └── QuoteWorker.java
└── src/test/java/cateringmanagement/workers/
    ├── InquiryWorkerTest.java
    └── InvoiceWorkerTest.java
```
