# GCP Integration in Java Using Conductor

A Java Conductor workflow that coordinates writes across three GCP services in parallel .  uploading an object to Google Cloud Storage, writing a document to Firestore, and publishing a message to Pub/Sub ,  then verifying all three completed successfully. Uses a FORK_JOIN to run the three service calls simultaneously. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate the parallel fan-out and verification.

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
| **GcpGcsUploadWorker** | `gcp_gcs_upload` | Uploads the payload to Google Cloud Storage .  stores the object in the specified bucket and returns the objectUrl |
| **GcpFirestoreWriteWorker** | `gcp_firestore_write` | Writes the payload as a Firestore document .  inserts the document into the specified collection and returns the documentId |
| **GcpPubsubPublishWorker** | `gcp_pubsub_publish` | Publishes a message to Pub/Sub .  sends the payload to the specified topic and returns the messageId |
| **GcpVerifyWorker** | `gcp_verify` | Verifies all three GCP services completed .  checks the objectUrl, Firestore documentId, and Pub/Sub messageId from the parallel branches |

Workers simulate external API calls with realistic response shapes so you can see the integration flow end-to-end. Replace with real API clients .  the workflow orchestration and error handling stay the same.

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
java -jar target/gcp-integration-1.0.0.jar

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
| `GOOGLE_APPLICATION_CREDENTIALS` | _(none)_ | Path to GCP service account JSON key file. Currently unused, all workers run in simulated mode with `[SIMULATED]` output prefix. Swap in Google Cloud client libraries for production. |

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/gcp-integration-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow gcp_integration \
  --version 1 \
  --input '{"bucketName": "test", "collection": "sample-collection", "topicName": "test", "payload": {"key": "value"}}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w gcp_integration -s COMPLETED -c 5

```

## How to Extend

Swap in the Google Cloud client libraries. Storage.create() for uploads, Firestore client for document writes, and Publisher.publish() for Pub/Sub messaging. The workflow definition stays exactly the same.

- **GcpFirestoreWriteWorker** (`gcp_firestore_write`): use the Google Cloud Firestore SDK to write real documents to Firestore collections
- **GcpGcsUploadWorker** (`gcp_gcs_upload`): use the Google Cloud Storage SDK to upload real objects to GCS buckets
- **GcpPubsubPublishWorker** (`gcp_pubsub_publish`): use the Google Cloud Pub/Sub SDK Publisher to publish real messages to topics

Replace each simulation with a real Google Cloud client library call while maintaining the same output fields, and the FORK_JOIN pipeline stays intact.

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
gcp-integration/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/gcpintegration/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── GcpIntegrationExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── GcpFirestoreWriteWorker.java
│       ├── GcpGcsUploadWorker.java
│       ├── GcpPubsubPublishWorker.java
│       └── GcpVerifyWorker.java
└── src/test/java/gcpintegration/workers/
    ├── GcpFirestoreWriteWorkerTest.java        # 8 tests
    ├── GcpGcsUploadWorkerTest.java        # 8 tests
    ├── GcpPubsubPublishWorkerTest.java        # 8 tests
    └── GcpVerifyWorkerTest.java        # 8 tests

```
