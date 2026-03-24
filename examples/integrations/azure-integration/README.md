# Azure Integration

Orchestrates azure integration through a multi-stage Conductor workflow.

**Input:** `containerName`, `databaseName`, `eventHubName`, `payload` | **Timeout:** 60s

## Pipeline

```
    ┌────────────────┬───────────────────┬─────────────────────┐
    │ az_blob_upload │ az_cosmosdb_write │ az_eventhub_publish │
    └────────────────┴───────────────────┴─────────────────────┘
az_verify
```

## Workers

**AzBlobUploadWorker** (`az_blob_upload`): Uploads a blob to Azure Blob Storage.

Reads `blobName`, `container`. Outputs `blobUrl`, `etag`, `contentLength`.

**AzCosmosDbWriteWorker** (`az_cosmosdb_write`): Writes a document to Azure CosmosDB.

```java
if (idVal != null) documentId = String.valueOf(idVal);
```

Reads `database`, `document`. Outputs `documentId`, `database`, `requestCharge`.

**AzEventHubPublishWorker** (`az_eventhub_publish`): perform  publishing an event to Azure Event Hub.

Reads `eventHub`. Outputs `sequenceNumber`, `eventHub`.

**AzVerifyWorker** (`az_verify`): Verifies that all Azure services completed successfully.

```java
boolean allPresent = blobUrl != null && !String.valueOf(blobUrl).isEmpty()
```

Reads `blobUrl`, `cosmosId`, `eventHubSeq`. Outputs `verified`.

## Tests

**0 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
