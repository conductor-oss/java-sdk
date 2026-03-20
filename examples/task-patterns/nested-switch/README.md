# Nested Switch in Java with Conductor

Multi-level decision tree using nested SWITCH tasks with value-param. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers .  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## The Problem

You need to route a request through a multi-level decision tree based on region and subscription tier. A US premium customer gets different processing than an EU standard customer or a customer from an unlisted region. The first level routes by region (US, EU, or other), and within each region, a second level routes by tier (premium or standard/default). Each combination. US/premium, US/standard, EU/premium, EU/standard, other/any .  runs completely different processing logic. After the region-and-tier-specific processing completes, a final completion step runs regardless of which branch was taken.

Without orchestration, you'd write deeply nested if/else or switch statements, with each branch calling different functions. Adding a new region or tier means modifying the routing code, retesting every branch, and hoping you didn't break an existing path. There is no record of which branch was taken for a given request, and debugging why a customer got the wrong processing requires tracing through nested conditionals.

## The Solution

**You just write the region-specific and tier-specific processing workers. Conductor handles the nested routing, branch tracking, and completion.**

This example demonstrates nested SWITCH tasks .  a multi-level decision tree declared in the workflow definition. The outer SWITCH routes on `region` (US, EU, or default). Within the US branch, a nested SWITCH routes on `tier` (premium or default/standard). Within the EU branch, another nested SWITCH does the same tier routing. Each leaf node is a dedicated worker. NsUsPremiumWorker handles US premium requests, NsEuStandardWorker handles EU standard requests, and NsOtherRegionWorker catches everything else. After the nested switches resolve, NsCompleteWorker runs the final completion step. Conductor records exactly which branch was taken, so you can see that a request with `region=EU, tier=premium` was routed to `ns_eu_premium`.

### What You Write: Workers

Six workers handle the multi-level decision tree: region-and-tier-specific workers (US Premium, US Standard, EU Premium, EU Standard, Other Region) each process their branch, and NsCompleteWorker runs the final completion step regardless of which path was taken.

| Worker | Task | What It Does |
|---|---|---|
| **NsCompleteWorker** | `ns_complete` | Final completion step after all nested switch branches. |
| **NsEuPremiumWorker** | `ns_eu_premium` | Handles EU region, premium tier requests. |
| **NsEuStandardWorker** | `ns_eu_standard` | Handles EU region, standard (default) tier requests. |
| **NsOtherRegionWorker** | `ns_other_region` | Handles requests from regions other than US or EU (default case). |
| **NsUsPremiumWorker** | `ns_us_premium` | Handles US region, premium tier requests. |
| **NsUsStandardWorker** | `ns_us_standard` | Handles US region, standard (default) tier requests. |

Workers simulate their processing steps so you can see the pattern in action without external services. Replace the simulation with real processing logic .  the task pattern and Conductor orchestration remain unchanged.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |
| **Conditional routing** | SWITCH tasks route execution to different paths based on worker output |

### The Workflow

```
SWITCH (region_switch_ref)
    ├── US: route_us_tier
    ├── EU: route_eu_tier
    └── default: ns_other_region
    │
    ▼
ns_complete
```

## Example Output

```
=== Nested SWITCH: Multi-level Decision Trees ===

Step 1: Registering task definitions...
  Registered: ns_us_premium, ns_us_standard, ns_eu_premium, ns_eu_standard, ns_other_region, ns_complete

Step 2: Registering workflow 'nested_switch_demo'...
  Workflow registered.

Step 3: Starting workers...
  6 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [ns_complete] region=
  [ns_eu_premium] region=
  [ns_eu_standard] region=
  [ns_other_region] region=
  [ns_us_premium] region=
  [ns_us_standard] region=

  Status: COMPLETED
  Output: {handler=..., done=...}

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
java -jar target/nested-switch-1.0.0.jar
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
java -jar target/nested-switch-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow nested_switch_demo \
  --version 1 \
  --input '{"region": "sample-region", "US": "sample-US", "tier": "sample-tier", "premium": "sample-premium", "amount": 250.0}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w nested_switch_demo -s COMPLETED -c 5
```

## How to Extend

Replace the region/tier-specific handlers with your real processing logic for each customer segment, and the multi-level decision tree workflow runs unchanged.

- **NsUsPremiumWorker** (`ns_us_premium`): apply US premium pricing rules, expedited shipping via domestic carriers, and premium support SLA handling
- **NsEuPremiumWorker** (`ns_eu_premium`): apply EU premium pricing with VAT compliance, GDPR-compliant data handling, and EU-specific carrier routing
- **NsOtherRegionWorker** (`ns_other_region`): handle international requests with customs documentation, currency conversion, and region-appropriate carrier selection
- **NsCompleteWorker** (`ns_complete`): finalize the request: send confirmation, update the order record, and trigger downstream fulfillment

Replacing the branch workers with real region-specific processing (tax calculations, compliance checks, etc.) does not alter the nested SWITCH routing, as long as each branch returns its expected processing result.

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
nested-switch/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/nestedswitch/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── NestedSwitchExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── NsCompleteWorker.java
│       ├── NsEuPremiumWorker.java
│       ├── NsEuStandardWorker.java
│       ├── NsOtherRegionWorker.java
│       ├── NsUsPremiumWorker.java
│       └── NsUsStandardWorker.java
└── src/test/java/nestedswitch/workers/
    ├── NsCompleteWorkerTest.java        # 4 tests
    ├── NsEuPremiumWorkerTest.java        # 4 tests
    ├── NsEuStandardWorkerTest.java        # 4 tests
    ├── NsOtherRegionWorkerTest.java        # 4 tests
    ├── NsUsPremiumWorkerTest.java        # 4 tests
    └── NsUsStandardWorkerTest.java        # 4 tests
```
