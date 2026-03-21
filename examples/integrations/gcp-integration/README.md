# GCP Integration in Java Using Conductor

## Writing to Multiple GCP Services Reliably

When processing an event, you often need to persist it across Google Cloud Storage (for archival), Firestore (for querying), and Pub/Sub (for notifying downstream consumers) simultaneously. These three operations are independent and can run in parallel, but you need all three to succeed and want visibility into which service failed if something goes wrong.

Without orchestration, you would manage three async calls, implement fan-out/join logic, handle partial failures, and build per-service retry loops. Conductor's FORK_JOIN handles the parallel execution and join automatically.

## The Solution

**You just write the GCP service workers. Cloud Storage upload, Firestore write, Pub/Sub publish, and cross-service verification. Conductor handles parallel FORK_JOIN execution, per-service retries, and join-point verification across all three GCP services.**

Each worker integrates with one external system. Conductor manages the integration sequence, retry logic, timeout handling, and data transformation between systems.

### What You Write: Workers

Four workers coordinate GCP writes: GcpGcsUploadWorker stores objects in Cloud Storage, GcpFirestoreWriteWorker persists documents, GcpPubsubPublishWorker sends messages, and GcpVerifyWorker confirms all parallel branches succeeded.

| Worker | Task | What It Does |
|---|---|---|
| **GcpGcsUploadWorker** | `gcp_gcs_upload` | Uploads the payload to Google Cloud Storage. stores the object in the specified bucket and returns the objectUrl |
| **GcpFirestoreWriteWorker** | `gcp_firestore_write` | Writes the payload as a Firestore document. inserts the document into the specified collection and returns the documentId |
| **GcpPubsubPublishWorker** | `gcp_pubsub_publish` | Publishes a message to Pub/Sub. sends the payload to the specified topic and returns the messageId |
| **GcpVerifyWorker** | `gcp_verify` | Verifies all three GCP services completed. checks the objectUrl, Firestore documentId, and Pub/Sub messageId from the parallel branches |

the workflow orchestration and error handling stay the same.

### The Workflow

```
FORK_JOIN
 ├── gcp_gcs_upload
 ├── gcp_firestore_write
 └── gcp_pubsub_publish
 │
 ▼
JOIN (wait for all branches)
gcp_verify

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
