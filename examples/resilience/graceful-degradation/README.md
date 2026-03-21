# Implementing Graceful Degradation in Java with Conductor: Core Processing with Optional Enrichment and Analytics

The recommendation engine goes down at 9 AM on Black Friday. Your product page has a "You might also like" section that calls the recommendation API synchronously. Now every product page takes 30 seconds to load, then shows a blank section where the recommendations should be. Customers leave. Revenue drops. The fix was obvious in retrospect: the product page should have shown bestsellers as a fallback and loaded in 200ms. But your code doesn't distinguish between "must succeed" and "nice to have," so one optional service took down the whole experience. This example builds a graceful degradation pipeline with Conductor: the core order process always completes, while optional enrichment and analytics run in parallel via `FORK_JOIN`. If either fails, the result is flagged as "degraded" but the customer still gets their confirmation.

## The Problem

Your order processing pipeline has a core function (create the order: this must succeed) plus optional enhancements, data enrichment from a third-party API (add customer demographics, credit score) and analytics tracking (send event to Segment/Mixpanel). When the enrichment API is down or the analytics service is slow, you do not want the core pipeline to fail or stall. The order should still be created, but with a "degraded" flag indicating which optional services were unavailable.

### What Goes Wrong Without Graceful Degradation

Consider an e-commerce order pipeline without graceful degradation:

1. Core: Create order ORD-123. **success**
2. Enrichment: Look up customer credit score. **TIMEOUT** (third-party API is slow)
3. Analytics: Track purchase event. **waiting...**

Without graceful degradation, the enrichment timeout blocks the entire pipeline. The customer sees a spinning wheel for 30 seconds, then gets an error page. The order was created but never confirmed to the customer. The analytics event is never sent. Support gets a ticket.

With graceful degradation, the core order is created immediately. Enrichment and analytics run in parallel. If either fails, the result is flagged as `degraded: true` but the order still completes. The customer gets their confirmation page in 200ms instead of a 30-second timeout.

## The Solution

**You just write the core processing and optional enrichment logic. Conductor handles FORK/JOIN parallel execution of optional services, continuing the pipeline when optional tasks fail, and clear tracking of which services were available versus degraded for every execution.**

The core process worker runs first and always succeeds. Then Conductor's FORK/JOIN runs the enrichment and analytics workers in parallel. Both are optional, so their failure does not fail the workflow. The finalize worker checks which optional services succeeded, sets a degraded flag if any failed, and produces the final result. Every execution shows which services were available and which were degraded. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

CoreProcessWorker handles the required order creation that must always succeed, EnrichWorker and AnalyticsWorker run in parallel via FORK/JOIN as optional enhancements, and FinalizeWorker checks which services responded and sets a degradation flag.

| Worker | Task | What It Does |
|---|---|---|
| **CoreProcessWorker** | `gd_core_process` | The required core processing step. Accepts `{data: "order-123"}`, returns `{result: "processed-order-123"}`. Always succeeds. Uses "default" when no data input is provided. |
| **EnrichWorker** | `gd_enrich` | Optional enrichment step. When `available=true` (or not specified), returns `{enriched: true}`. When `available=false`, returns `{enriched: false}` (simulating the enrichment service being down). |
| **AnalyticsWorker** | `gd_analytics` | Optional analytics tracking step. When `available=true` (or not specified), returns `{tracked: true}`. When `available=false`, returns `{tracked: false}` (simulating the analytics service being down). |
| **FinalizeWorker** | `gd_finalize` | Checks enrichment and analytics results. Sets `degraded=true` if either `enriched` or `analytics` is false. Returns `{enriched, analytics, degraded}`. |

Workers simulate success and failure scenarios so you can observe the resilience pattern end-to-end. Swap in real service calls and the retry, compensation, and recovery behavior works identically.

### The Workflow

```
gd_core_process  (always runs, always succeeds)
    |
    v
FORK_JOIN
    |-- branch 1: gd_enrich     (optional. Can fail without failing the workflow)
    |-- branch 2: gd_analytics  (optional. Can fail without failing the workflow)
    |
    v
JOIN (wait for both branches)
    |
    v
gd_finalize  (checks which services responded, sets degraded flag)

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
java -jar target/graceful-degradation-1.0.0.jar

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
=== Graceful Degradation Demo: Continue with Reduced Functionality ===

Step 1: Registering task definitions...
  Registered: gd_core_process, gd_enrich, gd_analytics, gd_finalize

Step 2: Registering workflow 'graceful_degradation_demo'...
  Workflow registered.

Step 3: Starting workers...
  4 workers polling.

Step 4: Starting workflow with all services available...

  Workflow ID: a2b3c4d5-...
  [gd_core_process] Processing data: order-123
  [gd_enrich] available=true
  [gd_analytics] available=true
  [gd_finalize] enriched=true analytics=true degraded=True


  Status: COMPLETED
  Output: {coreResult=processed-order-123, enriched=true, analytics=true, degraded=true}


Step 5: Starting workflow with enrichment unavailable...

  Workflow ID: e6f7a8b9-...
  Status: COMPLETED
  Output: {coreResult=processed-order-123, enriched=true, analytics=true, degraded=true}


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
java -jar target/graceful-degradation-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
# All services available.; no degradation
conductor workflow start \
  --workflow graceful_degradation_demo \
  --version 1 \
  --input '{"data": "order-789", "enrichAvailable": true, "analyticsAvailable": true}'

# Enrichment down: degraded mode (core still processes)
conductor workflow start \
  --workflow graceful_degradation_demo \
  --version 1 \
  --input '{"data": "order-707", "enrichAvailable": false, "analyticsAvailable": true}'

# Both optional services down: fully degraded (core still processes)
conductor workflow start \
  --workflow graceful_degradation_demo \
  --version 1 \
  --input '{"data": "order-809", "enrichAvailable": false, "analyticsAvailable": false}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w graceful_degradation_demo -s COMPLETED -c 5

```

## How to Extend

Each worker handles one concern. Connect the core worker to your order service, the enrichment worker to a third-party data API, the analytics worker to Segment or Mixpanel, and the core-plus-optional-enrichment workflow stays the same.

- **CoreProcessWorker** (`gd_core_process`): replace with your core business logic that must always succeed (order creation, data transformation, message routing)
- **EnrichWorker** (`gd_enrich`): call a third-party API for data enrichment (geolocation, sentiment analysis, credit scoring). Failure returns empty enrichment
- **AnalyticsWorker** (`gd_analytics`): publish events to your analytics pipeline (Segment, Mixpanel, internal data warehouse). Failure is silently tolerated
- **Add more optional branches**: add recommendation, fraud scoring, or personalization as additional FORK branches. Each can fail independently without affecting the core result.

Connect the core worker to your order service and the optional workers to your enrichment and analytics APIs, and the graceful degradation behavior adapts to production seamlessly.

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
graceful-degradation/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/gracefuldegradation/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── GracefulDegradationExample.java  # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AnalyticsWorker.java
│       ├── CoreProcessWorker.java
│       ├── EnrichWorker.java
│       └── FinalizeWorker.java
└── src/test/java/gracefuldegradation/workers/
    ├── CoreProcessWorkerTest.java   # 7 tests
    ├── EnrichWorkerTest.java        # 7 tests
    ├── AnalyticsWorkerTest.java     # 7 tests
    └── FinalizeWorkerTest.java      # 8 tests

```
