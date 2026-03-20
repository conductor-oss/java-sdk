# Jq Transform Advanced in Java with Conductor

Advanced JQ data transformations .  flatten orders, aggregate by customer, classify into tiers. Uses [Conductor](https://github.## The Problem

You need to transform raw order data into customer analytics .  starting with nested order objects containing customer details and line items, flattening them into a uniform structure with computed line totals, grouping by customer to calculate total spend and average order value, and finally classifying each customer into gold/silver/bronze tiers based on their spending. These are pure data transformations with no external API calls ,  just reshaping, aggregating, and classifying JSON.

Without a server-side transformation engine, you'd write a worker for each reshape step, deploy three separate Java classes that do nothing but manipulate JSON, and pay the operational cost of three polling workers for logic that is pure data plumbing. The transformation logic lives in Java code that is harder to iterate on than a declarative query expression.

## The Solution

**You just write JQ expressions in the workflow definition. Conductor evaluates them server-side. No workers, no polling, no deployment.**

This example uses zero workers. Every task is a JSON_JQ_TRANSFORM .  a JQ expression that runs on the Conductor server. The `jq_flatten` step takes nested order objects and produces a flat list with computed line totals (`price * qty`) and order totals. The `jq_aggregate` step groups flattened orders by customer, computing order count, total spent, and average order value per customer. The `jq_classify` step assigns each customer a tier (gold for $500+, silver for $200+, bronze otherwise) and produces a tier breakdown listing customers in each category. All three transformations are declarative JQ expressions ,  no Java code, no worker deployment, no polling.

### What You Write: Workers

This example uses Conductor system tasks (JSON_JQ_TRANSFORM) .  no custom workers needed. All three transformations are declarative JQ expressions that run on the Conductor server.

### The Workflow

```
jq_flatten [JSON_JQ_TRANSFORM]
    │
    ▼
jq_aggregate [JSON_JQ_TRANSFORM]
    │
    ▼
jq_classify [JSON_JQ_TRANSFORM]
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
java -jar target/jq-transform-advanced-1.0.0.jar
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
java -jar target/jq-transform-advanced-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow jq_advanced_demo \
  --version 1 \
  --input '{"orders": "test-value"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w jq_advanced_demo -s COMPLETED -c 5
```

## How to Extend

Since this example uses only JSON_JQ_TRANSFORM tasks (no workers), extending it means modifying the JQ expressions directly in `workflow.json`.

- **jq_flatten**: add fields from nested objects, compute additional derived values (discount percentages, tax amounts), or filter out cancelled line items
- **jq_aggregate**: add more aggregation dimensions (by product category, by date range, by region), compute median order values, or identify top-spending customers
- **jq_classify**: adjust tier thresholds for your business, add more tiers, or classify on multiple dimensions (spend + frequency + recency for RFM segmentation)

For operations that require external data (looking up customer history from a database, enriching with CRM data), add a SIMPLE worker task before or after the JQ transforms .  the transformation pipeline stays unchanged.

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
jq-transform-advanced/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/jqtransformadvanced/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── JqTransformAdvancedExample.java          # Main entry point (supports --workers mode)
│   └── workers/
└── src/test/java/jqtransformadvanced/workers/
    └── WorkflowDefinitionTest.java        # 24 tests
```
