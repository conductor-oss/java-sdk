# Salesforce Integration in Java Using Conductor

A Java Conductor workflow that runs a Salesforce lead scoring and sync pipeline. querying leads from Salesforce, scoring them with a predictive model, updating the scored records back in Salesforce, and syncing the results to a CRM target. Given a SOQL query, scoring model, and sync target, the pipeline produces scored leads, updated record counts, and sync status. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate the query-score-update-sync pipeline.

## Scoring and Syncing Salesforce Leads

Lead scoring in Salesforce involves a strict sequence: query the leads you want to score, run them through a scoring model, write the scores back to Salesforce, and sync the results to downstream systems. Each step depends on the previous one. you cannot score leads you have not queried, and you cannot sync records you have not updated. If the scoring model fails or the update step partially succeeds, you need visibility into exactly what happened.

Without orchestration, you would chain Salesforce REST API calls manually, manage lead lists and score maps between steps, and handle partial update failures yourself. Conductor sequences the pipeline and tracks lead counts, scores, and sync status between workers automatically.

## The Solution

**You just write the Salesforce workers. Lead querying, scoring, record updating, and CRM syncing. Conductor handles query-to-sync sequencing, Salesforce API retries, and lead count tracking across scoring and update stages.**

Each worker integrates with one external system. Conductor manages the integration sequence, retry logic, timeout handling, and data transformation between systems.

### What You Write: Workers

Four workers run the lead scoring pipeline: QueryLeadsWorker fetches leads via SOQL, ScoreLeadsWorker applies the predictive model, UpdateRecordsWorker writes scores back to Salesforce, and SyncCrmWorker propagates results to downstream systems.

| Worker | Task | What It Does |
|---|---|---|
| **QueryLeadsWorker** | `sfc_query_leads` | Queries leads from Salesforce. |
| **ScoreLeadsWorker** | `sfc_score_leads` | Scores leads using a model. |
| **SyncCrmWorker** | `sfc_sync_crm` | Syncs records to a CRM target. |
| **UpdateRecordsWorker** | `sfc_update_records` | Updates lead records in Salesforce. |

Workers implement external API calls with realistic response shapes so you can see the integration flow end-to-end. Replace with real API clients. the workflow orchestration and error handling stay the same.

### The Workflow

```
sfc_query_leads
    │
    ▼
sfc_score_leads
    │
    ▼
sfc_update_records
    │
    ▼
sfc_sync_crm

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
java -jar target/salesforce-integration-1.0.0.jar

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
| `SF_CLIENT_ID` | _(none)_ | Salesforce connected app client ID. Currently unused, all workers run in simulated mode with `[SIMULATED]` output prefix. |
| `SF_CLIENT_SECRET` | _(none)_ | Salesforce connected app client secret. |
| `SF_USERNAME` | _(none)_ | Salesforce username for API access. |
| `SF_PASSWORD` | _(none)_ | Salesforce password (with security token appended). |

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/salesforce-integration-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow salesforce_integration_438 \
  --version 1 \
  --input '{"query": "What is workflow orchestration?", "scoringModel": "gpt-4o-mini", "syncTarget": "production"}'Technology' AND status = 'New'": "pending", "scoringModel": "sample-scoringModel", "ml-lead-v2": "sample-ml-lead-v2", "syncTarget": "sample-syncTarget"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w salesforce_integration_438 -s COMPLETED -c 5

```

## How to Extend

Swap in Salesforce REST API SOQL queries for lead retrieval, your scoring model for lead evaluation, and the Salesforce update API for writing scores back. The workflow definition stays exactly the same.

- **QueryLeadsWorker** (`sfc_query_leads`): use the Salesforce REST API with SOQL queries to fetch real lead records
- **ScoreLeadsWorker** (`sfc_score_leads`): integrate a real scoring model (Einstein Lead Scoring, custom ML model) for predictive lead scores
- **UpdateRecordsWorker** (`sfc_update_records`): use the Salesforce REST API composite endpoint to batch-update lead score fields

Wire each worker to the Salesforce REST API or your scoring model while keeping the same return fields, and the scoring pipeline operates unchanged.

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
salesforce-integration/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/salesforceintegration/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── SalesforceIntegrationExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── QueryLeadsWorker.java
│       ├── ScoreLeadsWorker.java
│       ├── SyncCrmWorker.java
│       └── UpdateRecordsWorker.java
└── src/test/java/salesforceintegration/workers/
    ├── QueryLeadsWorkerTest.java        # 2 tests
    ├── ScoreLeadsWorkerTest.java        # 2 tests
    ├── SyncCrmWorkerTest.java        # 2 tests
    └── UpdateRecordsWorkerTest.java        # 2 tests

```
