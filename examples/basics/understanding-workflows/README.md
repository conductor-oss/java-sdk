# Understanding Workflows in Java with Conductor: A 3-Step Order Pipeline

Your team calls everything a "workflow", the JIRA board, the CI pipeline, the Slack approval chain. But a Conductor workflow is something specific: a directed acyclic graph of typed tasks with durable state, automatic retries, and observable data flow between steps. Understanding that distinction is the first step toward building reliable orchestration. This example makes it concrete with a three-step order pipeline (validate, calculate total, send confirmation) that shows how workflow input flows into the first task, how JSONPath expressions pass data between tasks, and how Conductor handles the execution lifecycle.

## Learning by Building an Order Pipeline

Workflow concepts (tasks, sequencing, input/output mapping) are easier to understand with a real-world example. This order pipeline shows how workflow input (`orderId`, `customerEmail`, `items`) flows into the first task, how each task's output becomes the next task's input via JSONPath expressions, and how the workflow's final output is assembled from task results.

The three steps are intentionally simple so you can focus on how the workflow connects them rather than on complex business logic.

## The Solution

**You just write the order validation, total calculation, and confirmation logic. Conductor handles sequencing, data passing, and the execution lifecycle.**

Three workers handle the order lifecycle: validation (checking order data), total calculation (summing prices and quantities), and confirmation (notifying the customer). The workflow definition in JSON declares the sequence and data flow. Each worker is independent, it doesn't know about the others.

### What You Write: Workers

Three workers model a simple order pipeline: validate, process, ship, to demonstrate how Conductor sequences tasks and passes data between them.

| Worker | Task | What It Does |
|---|---|---|
| **ValidateOrderWorker** | `validate_order` | Iterates through each item in the order, marks it as `validated: true` and `inStock: true`, and returns the enriched list with an item count. |
| **CalculateTotalWorker** | `calculate_total` | Sums `price * qty` for each validated item, applies 8% tax (rounded to cents), and returns subtotal, tax, and total. |
| **SendConfirmationWorker** | `send_confirmation` | Logs a confirmation message for the customer email and order total. Returns `emailSent: true` and the recipient address. |

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically. Configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status.; no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
validate_order
    │
    ▼
calculate_total
    │
    ▼
send_confirmation
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
java -jar target/understanding-workflows-1.0.0.jar
```

### Option 3: Use the run script

```bash
./run.sh

# Or on a custom port:
CONDUCTOR_PORT=9090 ./run.sh

# Or pointing at an existing Conductor:
CONDUCTOR_BASE_URL=http://localhost:9090/api ./run.sh
```

### Example Output

```
=== Order Pipeline: Understanding Workflows, Tasks, and Workers ===

Step 1: Registering task definitions...
  Registered: validate_order, calculate_total, send_confirmation

Step 2: Registering workflow 'order_pipeline'...
  Workflow registered.

Step 3: Starting workers...
  3 workers polling.

Step 4: Starting workflow...

  [validate_order] Validating order ORD-1001 with 2 items
  [calculate_total] Subtotal: $1059.97, Tax: $84.8, Total: $1144.77
  [send_confirmation] Sending confirmation to alice@example.com for order ORD-1001 ($1144.77)
  Workflow ID: <workflow-id>

Step 5: Waiting for completion...
  Status: COMPLETED
  Output: {orderId=ORD-1001, itemCount=2, subtotal=1059.97, tax=84.8, total=1144.77, emailSent=true, recipient=alice@example.com}

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
java -jar target/understanding-workflows-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow order_pipeline \
  --version 1 \
  --input '{"orderId": "ORD-2001", "customerEmail": "bob@example.com", "items": [{"name": "Keyboard", "price": 49.99, "qty": 1}]}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w order_pipeline -s COMPLETED -c 5
```

## How to Extend

- **ValidateOrderWorker** (`validate_order`): check item availability against a real inventory database, reject out-of-stock items, or apply business rules (minimum order value, shipping restrictions).
- **CalculateTotalWorker** (`calculate_total`): look up location-based tax rates or integrate with a tax calculation API like Avalara.
- **SendConfirmationWorker** (`send_confirmation`): send a real email via SendGrid, SES, or Mailgun with an order summary template.

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
understanding-workflows/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/understandingworkflows/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── OrderPipelineExample.java    # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CalculateTotalWorker.java
│       ├── SendConfirmationWorker.java
│       └── ValidateOrderWorker.java
└── src/test/java/understandingworkflows/workers/
    ├── CalculateTotalWorkerTest.java
    ├── SendConfirmationWorkerTest.java
    └── ValidateOrderWorkerTest.java
```
