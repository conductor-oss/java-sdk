# Fan-Out/Fan-In in Java with Conductor

Fan-Out/Fan-In. scatter-gather image processing using FORK_JOIN_DYNAMIC. Splits a variable-length image list into parallel processing tasks, compresses each independently, then aggregates results into a manifest with total savings. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers, you write the business logic, Conductor handles retries, failure routing, durability, and observability.

## The Problem

You need to process a batch of images in parallel: resizing, generating thumbnails, extracting metadata, or running object detection, where the number of images varies per request. A user uploads 5 images or 500, and each one must be processed independently at the same time. After every image is processed, the results must be aggregated into a single manifest with processed count, total size, and per-image metadata.

Without orchestration, you'd build a thread pool, submit each image as a task, manage futures for each one, wait for all completions with a barrier, and merge results manually. If processing crashes after 47 of 50 images complete, you lose the 47 already-processed results and start over. There is no way to see which images are still in flight, which have completed, or which failed. You get either all results or nothing.

## The Solution

**You just write the image preparation, processing, and aggregation workers. Conductor handles fanning out to N parallel branches via FORK_JOIN_DYNAMIC and joining the results.**

This example demonstrates the scatter-gather pattern using Conductor's FORK_JOIN_DYNAMIC. The PrepareWorker inspects the input image list and generates one `fo_process_image` task per image, each with a unique reference name (img_0_ref, img_1_ref...). Conductor fans out to N parallel branches. where N equals the number of images submitted. Each ProcessImageWorker processes its assigned image independently, compressing to 1/3 of original size and converting to WebP format. A JOIN task waits until every branch completes, then the AggregateWorker collects all per-image results from the join output into a unified manifest with processed count, total original and processed sizes, and savings percentage. If one image fails to process, Conductor retries just that branch, the other images are unaffected.

### What You Write: Workers

Three workers implement scatter-gather: PrepareWorker generates one task per image at runtime, ProcessImageWorker compresses each image independently in its own parallel branch, and AggregateWorker assembles a manifest with total savings after the join.

| Worker | Task | What It Does |
|---|---|---|
| **PrepareWorker** | `fo_prepare` | Takes an images list and generates dynamicTasks (one fo_process_image SIMPLE task per image with reference names img_0_ref, img_1_ref, etc.) and dynamicTasksInput (per-task input map with image data and index). Returns empty lists for null/empty input. |
| **ProcessImageWorker** | `fo_process_image` | Processes a single image: compresses to processedSize = originalSize / 3, converts to WebP format, and computes a deterministic processingTime based on name length and index. Returns name, originalSize, processedSize, format, and processingTime. |
| **AggregateWorker** | `fo_aggregate` | Collects all per-image results from the JOIN output, sorted by reference name. Computes processedCount, totalOriginal, totalProcessed, and savings percentage. Ignores non-image keys in the join output. |

Workers implement their processing steps so you can see the pattern in action without external services. Replace the simulation with real processing logic, the task pattern and Conductor orchestration remain unchanged.

### The Workflow

```
fo_prepare
    │
    ▼
FORK_JOIN_DYNAMIC (parallel, determined at runtime)
    │
    ▼
JOIN (wait for all branches)
fo_aggregate

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
java -jar target/fan-out-fan-in-1.0.0.jar

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
java -jar target/fan-out-fan-in-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow fan_out_fan_in_demo \
  --version 1 \
  --input '{"images": [{"name": "hero.jpg", "size": 2400}, {"name": "banner.png", "size": 3600}, {"name": "thumb.jpg", "size": 800}]}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w fan_out_fan_in_demo -s COMPLETED -c 5

```

## How to Extend

Replace the image compression stub with real processing (ImageMagick, Thumbnailator, AWS Rekognition), and the scatter-gather parallelism works unchanged.

- **PrepareWorker** (`fo_prepare`): query a database or S3 bucket for the list of images to process, filter by format or size, and generate the dynamic task definitions with per-image configuration (target resolution, output format, quality settings)
- **ProcessImageWorker** (`fo_process_image`): perform real image processing using ImageMagick, Thumbnailator, or a cloud vision API (AWS Rekognition, Google Vision); resize, generate thumbnails, extract EXIF metadata, run object detection, or apply watermarks
- **AggregateWorker** (`fo_aggregate`): merge all per-image results into a processing manifest, compute statistics (total bytes saved, success/failure rate, average processing time), upload the manifest to S3, and update the image catalog database

Replacing the demo compression with real image processing does not change the scatter-gather workflow, as long as each branch returns the expected name, size, and format fields.

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
fan-out-fan-in/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/fanoutfanin/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── FanOutFanInExample.java      # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AggregateWorker.java
│       ├── PrepareWorker.java
│       └── ProcessImageWorker.java
└── src/test/java/fanoutfanin/workers/
    ├── AggregateWorkerTest.java     # 7 tests
    ├── PrepareWorkerTest.java       # 6 tests
    └── ProcessImageWorkerTest.java  # 8 tests

```
