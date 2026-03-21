# Content Archival Pipeline in Java Using Conductor :  Inventory, Compression, Cold Storage, Indexing, and Integrity Verification

A Java Conductor workflow example that orchestrates content archival. scanning and identifying content eligible for archival (2,450 items totaling 15.2 GB), compressing with Zstandard to achieve 65% size reduction, transferring to cold storage (Glacier Deep Archive with 12-48 hour retrieval), building searchable indexes, and verifying data integrity with SHA-256 checksums and 7-year retention policies. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## Why Content Archival Needs Orchestration

Archiving content is a pipeline where ordering and integrity matter. You identify which content qualifies for archival based on age and access patterns. 2,450 items spanning 18 months. You compress everything into a tarball with Zstandard for 3:1 compression ratios while computing checksums. You transfer the compressed archive to cold storage (Glacier Deep Archive) where retrieval takes 12-48 hours. You index the archived content so it remains searchable without restoring from cold storage. Finally, you verify that the cold storage checksum matches the original, confirming zero data loss.

If compression fails partway through, you need to retry without re-scanning the entire content library. If the cold storage transfer succeeds but indexing fails, you need to resume at indexing. not re-upload 5 GB to Glacier. Without orchestration, you'd build a monolithic archival script that mixes content scanning, compression, cloud storage APIs, search indexing, and checksum verification,  making it impossible to change your storage tier, test indexing independently, or prove data integrity for compliance audits.

## How This Workflow Solves It

**You just write the archival workers. Content identification, compression, cold storage transfer, indexing, and integrity verification. Conductor handles ordered execution, Glacier upload retries, and provable integrity records for data retention compliance.**

Each archival stage is an independent worker. identify content, compress, store in cold storage, index, verify integrity. Conductor sequences them, passes file paths and checksums between stages, retries if a Glacier upload times out, and provides a complete audit trail proving every archived item was compressed, stored, indexed, and verified,  essential for compliance with data retention policies.

### What You Write: Workers

Five workers handle the archival pipeline: IdentifyContentWorker scans for eligible items, CompressWorker applies Zstandard compression, StoreColdWorker transfers to Glacier, IndexArchiveWorker builds searchable indexes, and VerifyIntegrityWorker confirms SHA-256 checksums.

| Worker | Task | What It Does |
|---|---|---|
| **CompressWorker** | `car_compress` | Compresses the data and computes compressed path, compressed size mb, compression ratio, checksum |
| **IdentifyContentWorker** | `car_identify_content` | Handles identify content |
| **IndexArchiveWorker** | `car_index_archive` | Indexes the archive |
| **StoreColdWorker** | `car_store_cold` | Stores the cold |
| **VerifyIntegrityWorker** | `car_verify_integrity` | Verifies the integrity |

Workers implement media processing stages. transcoding, thumbnail generation, metadata extraction,  with realistic output artifacts. Replace with real media tools (FFmpeg, ImageMagick) and the pipeline stays the same.

### The Workflow

```
car_identify_content
    │
    ▼
car_compress
    │
    ▼
car_store_cold
    │
    ▼
car_index_archive
    │
    ▼
car_verify_integrity

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
java -jar target/content-archival-1.0.0.jar

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
java -jar target/content-archival-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow content_archival_workflow \
  --version 1 \
  --input '{"archiveJobId": "TEST-001", "ageThresholdDays": "sample-ageThresholdDays", "contentCategory": "Process this order for customer C-100"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w content_archival_workflow -s COMPLETED -c 5

```

## How to Extend

Connect CompressWorker to your compression pipeline (Zstandard, gzip), StoreColdWorker to AWS Glacier or Azure Archive Storage, and IndexArchiveWorker to your search index (Elasticsearch). The workflow definition stays exactly the same.

- **IdentifyContentWorker** (`car_identify_content`): query your CMS or DAM to find content matching archival criteria (age, access count, content type) and return item counts, total size, and date ranges
- **CompressWorker** (`car_compress`): compress content using real Zstandard or gzip compression, compute SHA-256 checksums, and write the compressed archive to a staging location (S3, local disk)
- **StoreColdWorker** (`car_store_cold`): upload the compressed archive to cold storage (AWS Glacier, Azure Cool Blob, GCS Coldline) and record the storage class and estimated retrieval time
- **IndexArchiveWorker** (`car_index_archive`): index the archived content metadata in Elasticsearch or your search system so archived content remains discoverable without restoration
- **VerifyIntegrityWorker** (`car_verify_integrity`): verify the cold storage checksum matches the compression output, confirm the retention policy is applied, and generate a compliance attestation

Connect any worker to your cloud storage or search index while keeping its return schema, and the archival pipeline requires no reconfiguration.

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
content-archival/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/contentarchival/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ContentArchivalExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CompressWorker.java
│       ├── IdentifyContentWorker.java
│       ├── IndexArchiveWorker.java
│       ├── StoreColdWorker.java
│       └── VerifyIntegrityWorker.java
└── src/test/java/contentarchival/workers/
    ├── CompressWorkerTest.java        # 2 tests
    └── IdentifyContentWorkerTest.java        # 2 tests

```
