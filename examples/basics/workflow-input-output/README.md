# Workflow Input/Output in Java with Conductor: Data Flow via JSONPath in a Price Calculator

Your workflow runs, all tasks complete, but the output is empty, or worse, silently wrong. You passed `{"orderId": "123"}` but the task expected `${workflow.input.order_id}` with an underscore, not camelCase. Data flow in Conductor has rules: JSONPath expressions like `${taskRef.output.field}` wire each task's output to the next task's input, and a single typo means a null that propagates silently through the entire pipeline. This example makes those rules visible with a four-step price calculator (lookup, discount, tax, invoice) where you can trace every value from workflow input through each JSONPath expression to the final formatted output.

## Understanding Data Flow Between Tasks

The most important concept in Conductor workflows is how data flows: workflow inputs feed into the first task, each task's output can be referenced by subsequent tasks using JSONPath expressions, and the workflow output assembles results from any task in the pipeline. This price calculator makes the data flow concrete and traceable.

Starting with `productId`, `quantity`, and `couponCode` as workflow input: the price lookup produces a `unitPrice`, the discount applier reads that price and the coupon to produce a `discountedPrice`, the tax calculator reads the discounted price to produce a `taxAmount` and `total`, and the invoice formatter assembles everything into a formatted output.

## The Solution

**You just write the price lookup, discount application, tax calculation, and invoice formatting logic. Conductor handles the data flow between them via JSONPath.**

Four workers form the price calculation pipeline. Price lookup, discount application, tax calculation, and invoice formatting. The workflow JSON declares how each task reads from previous tasks' outputs using JSONPath, making the data flow explicit and inspectable in the Conductor UI.

### What You Write: Workers

The price calculator workers show how JSONPath expressions route specific fields from workflow input to each worker and aggregate their outputs.

| Worker | Task | What It Does |
|---|---|---|
| **LookupPriceWorker** | `lookup_price` | Resolves a product ID against a built-in catalog (PROD-001 = Wireless Keyboard $79.99, PROD-002 = 27" Monitor $349.99, PROD-003 = USB-C Hub $49.99) and multiplies by quantity to produce a subtotal. |
| **ApplyDiscountWorker** | `apply_discount` | Looks up a coupon code (SAVE10 = 10%, SAVE20 = 20%, HALF = 50%) and subtracts the discount from the subtotal. Unknown or missing coupons apply 0% discount. |
| **CalculateTaxWorker** | `calculate_tax` | Applies a fixed 8.25% sales tax rate to the discounted total and returns the tax amount and final total. |
| **FormatInvoiceWorker** | `format_invoice` | Assembles all prior outputs (product, price, discount, tax) into a formatted plaintext invoice string. |

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically. Configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status.; no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
lookup_price
    │
    ▼
apply_discount
    │
    ▼
calculate_tax
    │
    ▼
format_invoice
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
java -jar target/workflow-input-output-1.0.0.jar
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
=== Price Calculator: Data Flow Between Workflow Tasks ===

Step 1: Registering task definitions...
  Registered: lookup_price, apply_discount, calculate_tax, format_invoice

Step 2: Registering workflow 'price_calculator'...
  Workflow registered.

Step 3: Starting workers...
  4 workers polling.

Step 4: Starting workflow with sample input...
  Input: {productId=PROD-002, quantity=2, couponCode=SAVE20}

  [lookup_price] 27" Monitor x2 = $699.98
  [apply_discount] Coupon 'SAVE20' → 20% off $699.98 = -$140.00 → $559.98
  [calculate_tax] $559.98 + 8.25% tax ($46.20) = $606.18
  [format_invoice] Invoice generated.
  Workflow ID: <workflow-id>

Step 5: Waiting for completion...
  Status: COMPLETED

========================================
              INVOICE
========================================
Product:    27" Monitor
Unit Price: $349.99
Quantity:   2
Subtotal:   $699.98
----------------------------------------
Coupon:     SAVE20 (20% off)
Discount:  -$140.00
After Disc: $559.98
Tax (8.25%): $46.20
========================================
TOTAL:      $606.18
========================================

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
java -jar target/workflow-input-output-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow price_calculator \
  --version 1 \
  --input '{"productId": "PROD-001", "quantity": 3, "couponCode": "SAVE10"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w price_calculator -s COMPLETED -c 5
```

## How to Extend

- **LookupPriceWorker** (`lookup_price`): replace the in-memory catalog with a database query or product API call (e.g., Stripe Products, Shopify).
- **ApplyDiscountWorker** (`apply_discount`): validate coupons against a promotions database with usage limits and expiry dates.
- **CalculateTaxWorker** (`calculate_tax`): integrate a tax service like Avalara or TaxJar to calculate location-based tax rates.
- **FormatInvoiceWorker** (`format_invoice`): generate a PDF invoice or post it to an accounting system like QuickBooks.

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
workflow-input-output/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/workflowinputoutput/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── PriceCalculatorExample.java  # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ApplyDiscountWorker.java
│       ├── CalculateTaxWorker.java
│       ├── FormatInvoiceWorker.java
│       └── LookupPriceWorker.java
└── src/test/java/workflowinputoutput/workers/
    ├── ApplyDiscountWorkerTest.java
    ├── CalculateTaxWorkerTest.java
    ├── FormatInvoiceWorkerTest.java
    └── LookupPriceWorkerTest.java
```
