# Digital Asset Management in Java Using Conductor :  Ingestion, AI Tagging, Version Control, Storage, and CDN Distribution

A Java Conductor workflow example that orchestrates a digital asset management pipeline .  ingesting media files with checksum and dimension extraction, running AI-powered auto-tagging (object detection, color palette extraction, content classification), managing version history with change tracking, storing assets with searchable indexes, and distributing to CDN endpoints for global delivery. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers ,  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## Why Digital Asset Management Needs Orchestration

Managing media assets at scale involves a pipeline where each step enriches the asset with metadata and makes it available through more channels. You ingest the raw file .  extracting dimensions, file size, format, and computing a checksum for integrity. You run AI auto-tagging to detect objects, extract the dominant color palette, and classify the content with confidence scores. You create a version record linking to the previous version with change type tracking. You store the asset in your primary storage with searchable indexing. Finally, you distribute to CDN nodes for low-latency global access.

If ingestion fails, you need to retry without creating duplicate assets. If AI tagging produces low-confidence results, that should be flagged but not block storage. If CDN distribution fails for one region, the asset should still be available in others. Without orchestration, you'd build a monolithic asset processor that mixes file handling, ML inference, database operations, and CDN APIs .  making it impossible to swap your tagging model, add a new storage tier, or trace which version of an asset is live on which CDN.

## How This Workflow Solves It

**You just write the DAM workers. Asset ingestion, AI tagging, version control, storage, and CDN distribution. Conductor handles ingestion-to-CDN sequencing, upload retries, and a complete version history for every managed asset.**

Each DAM stage is an independent worker .  ingest asset, auto-tag, version control, store, distribute. Conductor sequences them, passes file paths, checksums, and tags between stages, retries if a CDN push times out, and maintains a complete version history for every asset from ingestion through distribution.

### What You Write: Workers

Five workers manage the DAM pipeline: IngestAssetWorker uploads files with checksum extraction, AutoTagWorker runs AI classification, VersionControlWorker tracks revisions, StoreAssetWorker indexes for search, and DistributeAssetWorker pushes to CDN endpoints.

| Worker | Task | What It Does |
|---|---|---|
| **AutoTagWorker** | `dam_auto_tag` | Auto-tags an asset using AI-based analysis. |
| **DistributeAssetWorker** | `dam_distribute_asset` | Distributes the asset to configured channels (website, mobile, portal). |
| **IngestAssetWorker** | `dam_ingest_asset` | Ingests a digital asset from the source URL and stores the original. |
| **StoreAssetWorker** | `dam_store_asset` | Stores the tagged and versioned asset in the DAM repository. |
| **VersionControlWorker** | `dam_version_control` | Creates a version entry for the asset. |

Workers simulate media processing stages .  transcoding, thumbnail generation, metadata extraction ,  with realistic output artifacts. Replace with real media tools (FFmpeg, ImageMagick) and the pipeline stays the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
Input -> AutoTagWorker -> DistributeAssetWorker -> IngestAssetWorker -> StoreAssetWorker -> VersionControlWorker -> Output
```

## Example Output

```
=== Example 521: Digital Asset Management ===

Step 1: Registering task definitions...
  Registered: dam_ingest_asset, dam_auto_tag, dam_version_control, dam_store_asset, dam_distribute_asset

Step 2: Registering workflow 'digital_asset_management'...
  Workflow registered.

Step 3: Starting workers...
  5 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [tag] Auto-tagging
  [distribute] Distributing asset to project
  [ingest] Ingesting
  [store] Storing asset v
  [version] Creating version for project

  Status: COMPLETED
  Output: {tags=..., aiConfidence=..., colorPalette=..., distributedTo=...}

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
java -jar target/digital-asset-management-1.0.0.jar
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
java -jar target/digital-asset-management-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow digital_asset_management \
  --version 1 \
  --input '{}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w digital_asset_management -s COMPLETED -c 5
```

## How to Extend

Connect IngestAssetWorker to your file storage, AutoTagWorker to your vision AI service (Google Vision, AWS Rekognition), and DistributeAssetWorker to your CDN (CloudFront, Akamai). The workflow definition stays exactly the same.

- **IngestAssetWorker** (`dam_ingest_asset`): handle real file uploads via multipart HTTP or S3 presigned URLs, extract EXIF/IPTC metadata, compute SHA-256 checksums, and read image dimensions
- **AutoTagWorker** (`dam_auto_tag`): call computer vision APIs (AWS Rekognition, Google Vision, Clarifai) for object detection, color palette extraction, and content classification with confidence scores
- **VersionControlWorker** (`dam_version_control`): store version records in your DAM database, link to previous versions, and track change types (new upload, edit, crop, recolor)
- **StoreAssetWorker** (`dam_store_asset`): write the asset to your primary storage (S3, Azure Blob, GCS), update the search index (Elasticsearch, Algolia), and generate access URLs
- **DistributeAssetWorker** (`dam_distribute_asset`): push assets to CDN origins (CloudFront, Fastly, Akamai) and generate regionally-optimized URLs for global delivery

Integrate any worker with your storage backend or tagging service while preserving output fields, and the asset pipeline needs no workflow changes.

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
digital-asset-management/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/digitalassetmanagement/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── DigitalAssetManagementExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AutoTagWorker.java
│       ├── DistributeAssetWorker.java
│       ├── IngestAssetWorker.java
│       ├── StoreAssetWorker.java
│       └── VersionControlWorker.java
└── src/test/java/digitalassetmanagement/workers/
    ├── AutoTagWorkerTest.java        # 8 tests
    ├── DistributeAssetWorkerTest.java        # 8 tests
    ├── IngestAssetWorkerTest.java        # 8 tests
    ├── StoreAssetWorkerTest.java        # 8 tests
    └── VersionControlWorkerTest.java        # 8 tests
```
