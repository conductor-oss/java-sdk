# Data Export Request in Java Using Conductor

A Java Conductor workflow example demonstrating Data Export Request. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers .  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## The Problem

You need to fulfill a user's data export request. Validating the request parameters and user identity, collecting their data across multiple categories (profile, activity, preferences, uploaded content), packaging everything into the requested format (JSON, CSV, ZIP), and delivering a secure download link to the user. Each step depends on the previous one's output.

If the collection step misses a data category, the export is incomplete and you fail GDPR Article 20 portability requirements. If packaging succeeds but delivery fails, the user's data sits in a temporary location with no notification that it's ready. Without orchestration, you'd build a monolithic export handler that mixes request validation, cross-service data queries, file packaging, and email delivery. Making it impossible to add new data sources, support additional export formats, or track which categories were included in which export for compliance auditing.

## The Solution

**You just write the request-validation, data-collection, packaging, and delivery workers. Conductor handles the export pipeline and cross-service data aggregation.**

ValidateExportWorker verifies the user's identity and checks that the requested export format (JSON, CSV) is supported and the data categories are valid. CollectDataWorker queries the user's data across all requested categories: profile information, activity logs, stored files, preferences, aggregating records from multiple services and databases. PackageDataWorker assembles the collected data into the requested format, compresses it into a downloadable archive, and uploads it to secure storage with a time-limited access URL. DeliverExportWorker sends the user a notification with the download link, recording that the export was successfully delivered. Each worker is a standalone Java class. Conductor handles the sequencing, retries, and crash recovery.

### What You Write: Workers

ValidateExportWorker verifies identity and format, CollectDataWorker aggregates profile and activity records, PackageDataWorker creates a downloadable archive, and DeliverExportWorker sends the expiring download link.

| Worker | Task | What It Does |
|---|---|---|
| **CollectDataWorker** | `der_collect` | Collects user data across requested categories (profile, activity, purchases), returning the total record count |
| **DeliverExportWorker** | `der_deliver` | Sends the export download link to the user with a 7-day expiration |
| **PackageDataWorker** | `der_package` | Packages the collected data into the requested format (JSON, CSV) and uploads it to a downloadable URL |
| **ValidateExportWorker** | `der_validate` | Validates the export request by verifying the user's identity and checking the requested format |

Workers simulate user lifecycle operations .  account creation, verification, profile setup ,  with realistic outputs. Replace with real identity provider and database calls and the workflow stays the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
der_validate
    │
    ▼
der_collect
    │
    ▼
der_package
    │
    ▼
der_deliver
```

## Example Output

```
=== Example 608: Data Export Request ===

Step 1: Registering task definitions...
  Registered: der_validate, der_collect, der_package, der_deliver

Step 2: Registering workflow 'der_data_export'...
  Workflow registered.

Step 3: Starting workers...
  4 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [collect] Collected data from
  [deliver] Export download link sent to user
  [package] Data packaged as
  [validate] Export request validated for

  Status: COMPLETED
  Output: {collectedData=..., totalRecords=..., delivered=..., expiresAt=...}

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
java -jar target/data-export-request-1.0.0.jar
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
java -jar target/data-export-request-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow der_data_export \
  --version 1 \
  --input '{"userId": "USR-EXP001", "USR-EXP001": "exportFormat", "exportFormat": "json", "json": "dataCategories", "dataCategories": ["item-1", "item-2", "item-3"]}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w der_data_export -s COMPLETED -c 5
```

## How to Extend

Each worker handles one export step .  connect your data stores for multi-source collection and your object storage (S3, GCS) for secure file delivery, and the data-export workflow stays the same.

- **ValidateExportWorker** (`der_validate`): verify the user exists in your identity provider (Auth0, Okta) and validate that requested data categories match your data catalog schema
- **CollectDataWorker** (`der_collect`): query user data across your services: profile from PostgreSQL, activity from Elasticsearch, files from S3, preferences from Redis. Aggregating everything into a unified structure
- **PackageDataWorker** (`der_package`): serialize the collected data into JSON, CSV, or machine-readable format per GDPR Article 20, compress it, and upload the archive to S3 with a pre-signed URL that expires after 72 hours
- **DeliverExportWorker** (`der_deliver`): send the download notification via email (SendGrid, SES) or in-app notification, logging the delivery timestamp for compliance audit trails

Connect your data stores and S3 for real packaging and the validate-collect-package-deliver export pipeline keeps functioning as defined.

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
data-export-request/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/dataexportrequest/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── DataExportRequestExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CollectDataWorker.java
│       ├── DeliverExportWorker.java
│       ├── PackageDataWorker.java
│       └── ValidateExportWorker.java
└── src/test/java/dataexportrequest/workers/
    ├── CollectDataWorkerTest.java        # 4 tests
    ├── DeliverExportWorkerTest.java        # 3 tests
    ├── PackageDataWorkerTest.java        # 3 tests
    └── ValidateExportWorkerTest.java        # 3 tests
```
