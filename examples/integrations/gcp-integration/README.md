# Gcp Integration

Orchestrates gcp integration through a multi-stage Conductor workflow.

**Input:** `bucketName`, `collection`, `topicName`, `payload` | **Timeout:** 60s

## Pipeline

```
    ┌────────────────┬─────────────────────┬────────────────────┐
    │ gcp_gcs_upload │ gcp_firestore_write │ gcp_pubsub_publish │
    └────────────────┴─────────────────────┴────────────────────┘
gcp_verify
```

## Workers

**GcpFirestoreWriteWorker** (`gcp_firestore_write`): Writes a document to Google Firestore.

Reads `collection`, `document`. Outputs `documentId`, `collection`, `writeTime`.

**GcpGcsUploadWorker** (`gcp_gcs_upload`): Uploads an object to Google Cloud Storage.

Reads `bucket`, `objectName`. Outputs `objectPath`, `generation`, `size`.

**GcpPubsubPublishWorker** (`gcp_pubsub_publish`): perform  publishing a message to Google Cloud Pub/Sub.

Reads `topic`. Outputs `messageId`, `topic`.

**GcpVerifyWorker** (`gcp_verify`): Verifies that all GCP services completed successfully.

```java
&& firestoreDoc != null && !firestoreDoc.isEmpty()
```

Reads `firestoreDoc`, `gcsObject`, `pubsubMsg`. Outputs `verified`.

## Tests

**32 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
