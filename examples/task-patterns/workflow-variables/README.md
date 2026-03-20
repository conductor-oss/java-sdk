# Workflow Variables in Java with Conductor

Shows how variables and expressions work across tasks. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers .  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## The Problem

You need to accumulate state across multiple tasks in an order pricing pipeline. Calculate the subtotal from items, apply a tier-based discount, compute shipping costs, and build a final summary. Each task depends on results from earlier tasks and the original workflow input. Workflow variables and expressions let you reference any task's output from any subsequent task using `${task_ref.output.field}` syntax.

Without workflow variables, you'd pass all accumulated state through every worker's input/output, or store intermediate results in an external database. Workflow variables keep intermediate state inside the workflow execution itself, visible and inspectable at every step.

## The Solution

**You just write the pricing, shipping, and summary workers. Conductor handles wiring each task's output into subsequent tasks via variable expressions.**

This example builds an order pricing pipeline where each task's output feeds into subsequent tasks via workflow variable expressions. CalcPriceWorker sums the items' `price * qty` to compute a subtotal. An INLINE system task (`apply_tier_discount`) applies tier-based discounts using JavaScript: gold tier gets 20% off, silver 10%, bronze 5%, reading the subtotal from `${calc_price_ref.output.subtotal}` and the tier from `${workflow.input.customerTier}`. CalcShippingWorker computes shipping cost based on the discounted subtotal and item count (free shipping for gold tier or orders over $100). Finally, BuildSummaryWorker assembles a complete order summary by pulling fields from all three preceding tasks and the original workflow input, demonstrating multi-source data composition across the full pipeline.

### What You Write: Workers

Three workers form an order pricing pipeline connected by variable expressions: CalcPriceWorker computes the subtotal from line items, CalcShippingWorker determines shipping cost based on tier and subtotal, and BuildSummaryWorker assembles the final order summary from all preceding task outputs.

| Worker | Task | What It Does |
|---|---|---|
| **BuildSummaryWorker** | `wv_build_summary` | Builds a final order summary by pulling data from workflow input and outputs of all preceding tasks. Demonstrates how... |
| **CalcPriceWorker** | `wv_calc_price` | Calculates the base price (subtotal) from a list of order items. Each item has a name, price, and qty. Returns the su... |
| **CalcShippingWorker** | `wv_calc_shipping` | Calculates shipping cost based on the discounted subtotal, item count, and customer tier. Gold tier or orders over $1... |

Workers simulate their processing steps so you can see the pattern in action without external services. Replace the simulation with real processing logic .  the task pattern and Conductor orchestration remain unchanged.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
wv_calc_price
    │
    ▼
apply_tier_discount [INLINE]
    │
    ▼
wv_calc_shipping
    │
    ▼
wv_build_summary
```

## Example Output

```
=== Workflow Variables: Persisting State Across Tasks ===

Step 1: Registering task definitions...
  Registered: wv_calc_price, wv_calc_shipping, wv_build_summary

Step 2: Registering workflow 'workflow_variables_demo'...
  Workflow registered.

Step 3: Starting workers...
  3 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [summary]
  [price]
  [shipping]

  Status: COMPLETED
  Output: {summary=..., subtotal=..., itemCount=..., shippingCost=...}

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
java -jar target/workflow-variables-1.0.0.jar
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
java -jar target/workflow-variables-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow workflow_variables_demo \
  --version 1 \
  --input '{"orderId": "ORD-5001", "ORD-5001": "customerTier", "customerTier": "gold", "gold": "items", "items": [{"name": "Widget A", "quantity": 2}, {"name": "Widget B", "quantity": 1}]}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w workflow_variables_demo -s COMPLETED -c 5
```

## How to Extend

Connect the pricing workers to your product catalog, discount service, and shipping rate APIs (UPS, FedEx), and the cross-task variable expressions work unchanged.

- **BuildSummaryWorker** (`wv_build_summary`): generate a real order confirmation by writing to your order database, producing a PDF invoice, or publishing to a notification service (email, SMS) with the final pricing breakdown
- **CalcPriceWorker** (`wv_calc_price`): query your product catalog or pricing service for current prices, apply quantity-based discounts, and handle tax calculations per jurisdiction
- **CalcShippingWorker** (`wv_calc_shipping`): call a shipping rate API (UPS, FedEx, USPS) with the order's weight, dimensions, and destination to get real shipping quotes instead of the flat-rate formula

Connecting the pricing workers to real catalog, discount, and shipping rate APIs does not affect the cross-task variable expressions, since each worker's output fields are referenced by subsequent tasks through the workflow definition.

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
workflow-variables/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/workflowvariables/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── WorkflowVariablesExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── BuildSummaryWorker.java
│       ├── CalcPriceWorker.java
│       └── CalcShippingWorker.java
└── src/test/java/workflowvariables/workers/
    ├── BuildSummaryWorkerTest.java        # 6 tests
    ├── CalcPriceWorkerTest.java        # 6 tests
    └── CalcShippingWorkerTest.java        # 6 tests
```
