# Live Streaming Pipeline in Java Using Conductor :  Stream Setup, Encoding, CDN Distribution, Quality Monitoring, and Archival

A Java Conductor workflow example that orchestrates a live streaming pipeline .  provisioning ingest URLs and stream keys, encoding to adaptive bitrate HLS streams with low-latency codecs, distributing across CDN nodes with multi-region viewer support, monitoring stream quality (peak viewers, average bitrate, buffer ratio, quality scores), and archiving the completed stream with thumbnails for VOD playback. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers ,  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## Why Live Streaming Workflows Need Orchestration

Managing a live stream involves a sequence of infrastructure operations that must happen in the right order. You set up the stream .  provisioning an RTMP ingest URL and stream key. You configure encoding ,  setting up adaptive bitrate transcoding at multiple quality levels with low-latency settings. You distribute the encoded stream to CDN edge nodes across viewer regions. You monitor quality throughout ,  tracking peak concurrent viewers, average bitrate, buffer ratios, and computing an overall quality score. After the stream ends, you archive the recording with generated thumbnails for on-demand playback.

If stream setup fails, encoding must not start. If CDN distribution drops in one region, monitoring should detect the quality degradation. If the process crashes during archival, you need to resume from archiving .  not re-encode the entire stream. Without orchestration, you'd build a monolithic streaming controller that mixes infrastructure provisioning, transcoding configuration, CDN management, real-time monitoring, and archival ,  making it impossible to swap CDN providers, upgrade your encoder, or trace why a specific stream had buffering issues.

## How This Workflow Solves It

**You just write the streaming workers. Stream setup, encoding, CDN distribution, quality monitoring, and archival. Conductor handles infrastructure sequencing, CDN failover retries, and quality metric recording for post-broadcast analysis.**

Each streaming stage is an independent worker .  setup, encode, distribute, monitor quality, archive. Conductor sequences them, passes stream keys and playback URLs between stages, retries if a CDN node fails to respond, and records quality metrics for every stream for post-broadcast analysis.

### What You Write: Workers

Five workers manage the broadcast lifecycle: SetupStreamWorker provisions ingest URLs, EncodeStreamWorker configures adaptive bitrate transcoding, DistributeStreamWorker pushes to CDN edge nodes, MonitorQualityWorker tracks viewer metrics, and ArchiveStreamWorker creates the VOD recording.

| Worker | Task | What It Does |
|---|---|---|
| **ArchiveStreamWorker** | `lsm_archive_stream` | Archives the completed stream for VOD access. |
| **DistributeStreamWorker** | `lsm_distribute_stream` | Distributes the encoded stream to CDN edge nodes. |
| **EncodeStreamWorker** | `lsm_encode_stream` | Encodes the live stream with adaptive bitrate profiles. |
| **MonitorQualityWorker** | `lsm_monitor_quality` | Monitors stream quality metrics. |
| **SetupStreamWorker** | `lsm_setup_stream` | Sets up a live stream with ingest URL and stream key. |

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
Input -> ArchiveStreamWorker -> DistributeStreamWorker -> EncodeStreamWorker -> MonitorQualityWorker -> SetupStreamWorker -> Output
```

## Example Output

```
=== Example 522: Live Streaming ===

Step 1: Registering task definitions...
  Registered: lsm_setup_stream, lsm_encode_stream, lsm_distribute_stream, lsm_monitor_quality, lsm_archive_stream

Step 2: Registering workflow 'live_streaming'...
  Workflow registered.

Step 3: Starting workers...
  5 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [archive] Archiving
  [distribute] Distributing
  [encode] Encoding at
  [monitor] Monitoring stream quality
  [setup] Setting up stream \"" + title + "\" at

  Status: COMPLETED
  Output: {archiveUrl=..., archiveSize=..., archivedAt=..., thumbnailUrl=...}

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
java -jar target/live-streaming-1.0.0.jar
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
java -jar target/live-streaming-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow live_streaming \
  --version 1 \
  --input '{}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w live_streaming -s COMPLETED -c 5
```

## How to Extend

Connect SetupStreamWorker to your streaming infrastructure (AWS MediaLive, Mux), EncodeStreamWorker to your transcoder, and DistributeStreamWorker to your CDN edge nodes. The workflow definition stays exactly the same.

- **SetupStreamWorker** (`lsm_setup_stream`): provision real stream infrastructure via your streaming platform API (AWS IVS, Mux, Wowza) to generate ingest URLs and stream keys
- **EncodeStreamWorker** (`lsm_encode_stream`): configure real-time transcoding with adaptive bitrate ladders (1080p/720p/480p/feature-environmentp), codec selection (H.264/H.265), and latency targets
- **DistributeStreamWorker** (`lsm_distribute_stream`): push HLS/DASH manifests to CDN edge nodes (CloudFront, Fastly) across viewer regions and return playback URLs
- **MonitorQualityWorker** (`lsm_monitor_quality`): collect real-time stream health metrics from your video analytics platform (Mux Data, Conviva) including viewer counts, bitrate, buffer ratios, and quality scores
- **ArchiveStreamWorker** (`lsm_archive_stream`): save the stream recording to cloud storage (S3, GCS), generate thumbnails at key moments, and create a VOD asset for on-demand playback

Wire each worker to your streaming infrastructure while preserving output fields, and the broadcast pipeline stays intact.

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
live-streaming/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/livestreaming/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── LiveStreamingExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ArchiveStreamWorker.java
│       ├── DistributeStreamWorker.java
│       ├── EncodeStreamWorker.java
│       ├── MonitorQualityWorker.java
│       └── SetupStreamWorker.java
└── src/test/java/livestreaming/workers/
    ├── ArchiveStreamWorkerTest.java        # 8 tests
    ├── DistributeStreamWorkerTest.java        # 8 tests
    ├── EncodeStreamWorkerTest.java        # 8 tests
    ├── MonitorQualityWorkerTest.java        # 8 tests
    └── SetupStreamWorkerTest.java        # 8 tests
```
