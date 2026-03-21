# Azure Integration in Java Using Conductor

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

the workflow orchestration and error handling stay the same.

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

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
