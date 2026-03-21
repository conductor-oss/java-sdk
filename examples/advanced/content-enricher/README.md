# Content Enricher in Java Using Conductor: Augment Messages with External Customer Data

A webhook fires: `{"customerId": "CUST-42", "orderId": "ORD-999", "amount": 1250.00}`. That's it. Your fulfillment service needs the customer's region, account tier, and credit limit to route the order. Your analytics pipeline needs the company name and lifetime value. But the event is just an ID and a dollar amount, a skeleton with no context. So your handler starts making inline API calls to the CRM, the customer database, and the geo-IP service, tangling enrichment logic with transport logic until adding one new data source means rewriting the whole consumer. This example builds a content enrichment pipeline with Conductor: extract the customer ID, look up account data from external sources, merge it into the original payload, and forward the enriched message downstream. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers, you write the business logic, Conductor handles retries, failure routing, durability, and observability.

## Incomplete Messages Need Context

An incoming order event contains a customer ID and a list of items, but the downstream analytics service needs the customer's region, account tier, and lifetime value. A signup notification has an email address, but the marketing system needs the company name, industry, and employee count. Messages arrive lean, with just an identifier, and need to be enriched with context from CRMs, geo-IP databases, or third-party APIs before they're useful downstream.

Building enrichment inline means coupling your message consumer to every data source it needs, retrying each lookup independently, and deciding what to do when one source is down but the others succeed. The enrichment logic gets tangled with the transport logic, and adding a new data source means modifying the consumer.

## The Solution

**You write the enrichment logic. Conductor handles the pipeline, retries, and state.**

`ReceiveMessageWorker` ingests the incoming message and extracts the customer ID. `LookupDataWorker` queries the configured enrichment sources for that customer. Pulling profile data, geo information, or account details. `EnrichWorker` merges the lookup results into the original message, producing an enriched payload with all the context the downstream system needs. `ForwardWorker` delivers the enriched message to the next queue or service. Conductor ensures the lookup happens after the customer ID is extracted, retries if a data source is temporarily unavailable, and records the full before-and-after so you can see exactly what was added to each message.

### What You Write: Workers

Four workers form the enrichment pipeline: message reception, external data lookup, payload merging, and downstream forwarding, each decoupled from the data sources it queries.

| Worker | Task | What It Does |
|---|---|---|
| **ReceiveMessageWorker** | `enr_receive_message` | Parses the incoming order/signup event and extracts the customer ID for downstream lookup |
| **LookupDataWorker** | `enr_lookup_data` | Queries enrichment sources (CRM, customer DB) and returns account name, tier, region, credit limit, and order history |
| **EnrichWorker** | `enr_enrich` | Merges lookup fields into the original message, producing a single enriched payload with full customer context |
| **ForwardWorker** | `enr_forward` | Delivers the enriched message to the downstream queue (e.g., `order_processing_queue`) for fulfillment or analytics |

Workers implement the pattern behavior with realistic inputs and outputs so you can observe the advanced workflow mechanics. Replace with real implementations, the pattern and Conductor orchestration stay the same.

### The Workflow

```
enr_receive_message
    │
    ▼
enr_lookup_data
    │
    ▼
enr_enrich
    │
    ▼
enr_forward

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
java -jar target/content-enricher-1.0.0.jar

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
java -jar target/content-enricher-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow enr_content_enricher \
  --version 1 \
  --input '{"message": {"customerId": "CUST-42", "orderId": "ORD-999", "amount": 1250.0}, "enrichmentSources": ["customer_db", "crm_system"]}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w enr_content_enricher -s COMPLETED -c 5

```

## How to Extend

Each worker handles one enrichment concern. Replace the demo CRM and customer lookups with real Salesforce or database queries and the enrichment pipeline runs unchanged.

- **LookupDataWorker** (`enr_lookup_data`): query real data sources: Salesforce CRM for account details, Clearbit API for company enrichment, MaxMind GeoIP for location data, or your own customer database
- **ForwardWorker** (`enr_forward`): publish the enriched message to a Kafka topic, post it to a webhook, or write it to a Snowflake/BigQuery table for downstream analytics
- **ReceiveMessageWorker** (`enr_receive_message`): consume from a real SQS queue, Kafka topic, or webhook endpoint instead of accepting the message as workflow input

The enriched-message output shape stays fixed. Adding a new data source means adding one lookup worker, not modifying the enrichment or forwarding stages.

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
content-enricher/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/contentenricher/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ContentEnricherExample.java  # Main entry point (supports --workers mode)
│   └── workers/
│       ├── EnrichWorker.java
│       ├── ForwardWorker.java
│       ├── LookupDataWorker.java
│       └── ReceiveMessageWorker.java
└── src/test/java/contentenricher/workers/
    ├── ReceiveMessageWorkerTest.java  # 7 tests
    ├── LookupDataWorkerTest.java      # 7 tests
    ├── EnrichWorkerTest.java          # 7 tests
    └── ForwardWorkerTest.java         # 7 tests

```
