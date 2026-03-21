# Azure Integration in Java Using Conductor

A Java Conductor workflow that coordinates writes across three Azure services in parallel. uploading a blob to Azure Blob Storage, writing a document to CosmosDB, and publishing an event to Event Hub,  then verifying all three completed successfully. Uses a FORK_JOIN to run the three service calls simultaneously. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate the parallel fan-out and verification.

## Writing to Multiple Azure Services Reliably

When processing an event, you often need to persist it across Azure Blob Storage (for archival), CosmosDB (for querying), and Event Hub (for streaming to downstream consumers) simultaneously. These three operations are independent and can run in parallel, but you need all three to succeed and want visibility into which service failed if something goes wrong.

Without orchestration, you would manage three async calls, implement fan-out/join logic, handle partial failures, and build per-service retry loops. Conductor's FORK_JOIN handles the parallel execution and join automatically.

## The Solution

**You just write the Azure service workers. Blob Storage upload, CosmosDB write, Event Hub publish, and cross-service verification. Conductor handles parallel FORK_JOIN execution, individual service retries, and join verification for cross-service consistency.**

Each worker integrates with one external system. Conductor manages the integration sequence, retry logic, timeout handling, and data transformation between systems.

### What You Write: Workers

Four workers coordinate Azure writes: AzBlobUploadWorker stores blobs, AzCosmosDbWriteWorker persists documents, AzEventHubPublishWorker streams events, and AzVerifyWorker confirms all three parallel operations succeeded.

| Worker | Task | What It Does |
|---|---|---|
| **AzBlobUploadWorker** | `az_blob_upload` | Uploads the payload to Azure Blob Storage. stores the data at `events/{id}.json` in the specified container and returns the blobUrl |
| **AzCosmosDbWriteWorker** | `az_cosmosdb_write` | Writes the payload as a CosmosDB document. inserts the document into the specified database and returns the documentId |
| **AzEventHubPublishWorker** | `az_eventhub_publish` | Publishes the event to Azure Event Hub. sends the payload to the specified event hub and returns the sequenceNumber |
| **AzVerifyWorker** | `az_verify` | Verifies all three Azure services completed. checks the blobUrl, CosmosDB documentId, and Event Hub sequenceNumber from the parallel branches |

Workers implement external API calls with realistic response shapes so you can see the integration flow end-to-end. Replace with real API clients. the workflow orchestration and error handling stay the same.

### The Workflow

```
FORK_JOIN
    ├── az_blob_upload
    ├── az_cosmosdb_write
    └── az_eventhub_publish
    │
    ▼
JOIN (wait for all branches)
az_verify

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
java -jar target/azure-integration-1.0.0.jar

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
| `AZURE_CLIENT_ID` | _(none)_ | Azure service principal client ID. Currently unused, all workers run in simulated mode with `[SIMULATED]` output prefix. Swap in Azure SDK for production. |
| `AZURE_CLIENT_SECRET` | _(none)_ | Azure service principal client secret. Required alongside `AZURE_CLIENT_ID` for production use. |
| `AZURE_TENANT_ID` | _(none)_ | Azure Active Directory tenant ID. Required alongside `AZURE_CLIENT_ID` for production use. |

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/azure-integration-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow azure_integration \
  --version 1 \
  --input '{"containerName": "test", "databaseName": "test", "eventHubName": "test", "payload": {"key": "value"}}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w azure_integration -s COMPLETED -c 5

```

## How to Extend

Swap in the Azure SDK calls. BlobClient.upload() in the upload worker, CosmosContainer.createItem() in the write worker, and EventHubProducerClient.send() in the publish worker. The workflow definition stays exactly the same.

- **AzBlobUploadWorker** (`az_blob_upload`): use the Azure Storage SDK BlobClient to upload real blobs
- **AzCosmosDbWriteWorker** (`az_cosmosdb_write`): use the Azure Cosmos SDK CosmosContainer to write real documents
- **AzEventHubPublishWorker** (`az_eventhub_publish`): use the Azure Event Hubs SDK EventHubProducerClient to publish real events

Swap each simulation for a real Azure SDK call while preserving output fields, and the FORK_JOIN pipeline needs no workflow changes.

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
azure-integration/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/azureintegration/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── AzureIntegrationExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AzBlobUploadWorker.java
│       ├── AzCosmosDbWriteWorker.java
│       ├── AzEventHubPublishWorker.java
│       └── AzVerifyWorker.java

```
