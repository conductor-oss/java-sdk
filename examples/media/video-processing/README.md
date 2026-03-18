# Video Processing Pipeline in Java Using Conductor -- Upload, Adaptive Transcode, Thumbnail Generation, Metadata Indexing, and Publishing

A creator uploads a 742MB 4K video. Your monolithic transcoder starts the 1080p rendition, runs out of memory halfway through the 720p, and dies. The thumbnail generator -- which somebody wired to run concurrently -- extracts a frame from the half-transcoded file and gets a green-and-black glitch image. The metadata indexer never ran at all, so the video doesn't appear in search. The publish step has no idea any of this happened and pushes a broken HLS manifest to the CDN. Now there's a live watch URL serving a video that plays for 40 seconds and then freezes. This example orchestrates the full video pipeline with Conductor: upload with codec detection, adaptive bitrate transcoding (1080p/720p/480p), thumbnail extraction at a computed keyframe, metadata indexing, and publishing -- each step sequenced with proper dependency handling and per-step retries. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers -- you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## Why Video Processing Needs Orchestration

Processing video for delivery involves a pipeline where each stage produces assets that downstream stages depend on. You ingest the source file -- detecting the codec (h264, h265, VP9), measuring duration, and storing the raw file to object storage with its original quality preserved. You transcode to adaptive bitrate HLS, generating resolution renditions at 1080p (5000k), 720p (2500k), and 480p (1000k) so the player can switch quality based on network conditions. You generate a thumbnail by extracting a frame at a computed position (one-third of the duration, avoiding black intro frames). You index metadata -- title, duration, available resolutions, content type -- so the video appears in search results and recommendation feeds. Finally, you publish the video with its HLS manifest, thumbnail, and metadata to a live watch URL.

Each stage depends on specific outputs from earlier stages -- transcoding needs the storage path from upload, thumbnail generation needs the duration, metadata indexing needs the resolution list from transcoding, and publishing needs the HLS URL, thumbnail URL, and metadata. If transcoding fails partway through (the 720p rendition succeeds but 480p times out), you need to retry just the failed rendition -- not re-upload the 742 MB source file. Without orchestration, you'd build a monolithic video processor that mixes FFmpeg calls, S3 uploads, thumbnail extraction, and database writes -- making it impossible to swap your transcoding backend (FFmpeg to MediaConvert), add new rendition profiles, or trace why a specific video's thumbnail shows a black frame.

## How This Workflow Solves It

**You just write the video processing workers -- upload ingestion, adaptive transcode, thumbnail generation, metadata indexing, and publishing. Conductor handles transcoding sequencing, FFmpeg timeout retries, and end-to-end tracking from upload through live publication.**

Each video processing stage is an independent worker -- upload, transcode, thumbnail, metadata, publish. Conductor sequences them, passes the storage path from upload into transcode and thumbnail, feeds the resolution list and HLS URL from transcode into metadata and publish, retries if an FFmpeg process times out, and tracks every video from upload through live publication.

### What You Write: Workers

Five workers process each video: UploadWorker ingests the source with codec detection, TranscodeWorker creates adaptive bitrate renditions, ThumbnailWorker extracts a preview frame, MetadataWorker builds the search index, and PublishWorker makes the video live.

| Worker | Task | What It Does | Real / Simulated |
|---|---|---|---|
| **UploadWorker** | `vid_upload` | Ingests source video from a URL, stores it to object storage, and detects codec (`h264`), duration (`185s`), and file size (`742 MB`). Returns `storagePath`, `duration`, `fileSizeMb`, `codec`. | Simulated -- returns fixed media properties for the given `videoId` |
| **TranscodeWorker** | `vid_transcode` | Transcodes the raw video to adaptive bitrate HLS with three resolution renditions: 1080p (5000k), 720p (2500k), 480p (1000k). Returns `resolutions` list, `hlsUrl`, `format`. | Simulated -- returns deterministic rendition profiles and an HLS manifest URL |
| **ThumbnailWorker** | `vid_thumbnail` | Generates a thumbnail by extracting a frame at one-third of the video duration (avoiding black intro frames). Returns `thumbnailUrl`, `capturedAtSecond`, `width`, `height`. | Simulated -- computes capture position from input `duration`, returns a CDN thumbnail URL |
| **MetadataWorker** | `vid_metadata` | Assembles a metadata index from upstream outputs -- title, duration, resolution list, content type -- for search and discovery. Returns a `metadata` map. | Simulated -- builds a deterministic metadata map from task inputs |
| **PublishWorker** | `vid_publish` | Publishes the video with its HLS manifest, thumbnail, and metadata to a live watch URL. Returns `publishUrl`, `publishedAt`, `status`. | Simulated -- returns a deterministic watch URL and `published` status |

Workers simulate media processing stages -- transcoding, thumbnail generation, metadata extraction -- with realistic output artifacts. Replace with real media tools (FFmpeg, ImageMagick) and the pipeline stays the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically -- configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status -- no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
vid_upload
    │
    ▼
vid_transcode
    │
    ▼
vid_thumbnail
    │
    ▼
vid_metadata
    │
    ▼
vid_publish
```

### Sample Output

When the workflow runs with `videoId=VID-001`, the workers produce these artifacts:

```
[vid_upload] Ingesting: VID-001 from https://uploads.example.com/raw/VID-001.mp4
  Storage path: s3://video-raw/VID-001/original.mp4
  Duration: 185s, Size: 742 MB, Codec: h264

[vid_transcode] Transcoding to adaptive bitrate...
  Resolutions: 1080p (5000k), 720p (2500k), 480p (1000k)
  HLS URL: https://cdn.example.com/hls/VID-001/master.m3u8
  Format: HLS

[vid_thumbnail] Generating thumbnail...
  Captured at: 61s (duration / 3)
  URL: https://cdn.example.com/thumbs/VID-001/default.jpg
  Dimensions: 1280x720

[vid_metadata] Building metadata index...
  Title: Introduction to Conductor Workflows
  Duration: 185s, Content type: video/mp4
  Resolutions: [1080p, 720p, 480p]

[vid_publish] Publishing...
  Watch URL: https://watch.example.com/v/VID-001
  Status: published
```

## Example Output

```
=== Video Processing Pipeline ===

Step 1: Registering task definitions...
  Registered: vid_upload, vid_transcode, vid_thumbnail, vid_metadata, vid_publish

Step 2: Registering workflow 'video_processing_workflow'...
  Workflow registered.

Step 3: Starting workers...
  5 workers polling.

Step 4: Starting workflow...
  Workflow ID: 35df8ee9-7189-8d71-3acb-d9f474b2c56e

  [metadata] Extracting metadata for video
  [publish] Publishing video
  [thumbnail] Generating thumbnail for video
  [transcode] Transcoding video
  [upload] Ingesting video

  Status: COMPLETED
  Output: {videoId=VID-001, publishUrl=https://api.example.com/v1, resolutions=sample-resolutions, duration=185}

Result: PASSED
```

## Running It

### Prerequisites

- **Java 21+** -- verify with `java -version`
- **Maven 3.8+** -- verify with `mvn -version`
- **Docker** -- to run Conductor

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
java -jar target/video-processing-1.0.0.jar
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
java -jar target/video-processing-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow video_processing_workflow \
  --version 1 \
  --input '{"videoId": "VID-001", "sourceUrl": "https://uploads.example.com/raw/VID-001.mp4", "title": "Introduction to Conductor Workflows", "creatorId": "creator-42"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w video_processing_workflow -s COMPLETED -c 5
```

## How to Extend

Connect TranscodeWorker to your encoding backend (FFmpeg, AWS MediaConvert), ThumbnailWorker to your frame extraction service, and PublishWorker to your video CDN. The workflow definition stays exactly the same.

- **UploadWorker** (`vid_upload`) -- ingest source video via multipart upload or URL fetch, store to S3/GCS, and probe with FFprobe to extract codec, duration, file size, and container format
- **TranscodeWorker** (`vid_transcode`) -- transcode using FFmpeg, AWS MediaConvert, or Google Transcoder API to produce adaptive bitrate HLS with configurable rendition profiles (resolution, bitrate, codec)
- **ThumbnailWorker** (`vid_thumbnail`) -- extract a frame at a computed keyframe position using FFmpeg, generate multiple thumbnail sizes for different UI contexts (player poster, grid card, social share), and upload to your CDN
- **MetadataWorker** (`vid_metadata`) -- index video metadata (title, duration, resolutions, content type) in Elasticsearch or your catalog database for search, filtering, and recommendation feeds
- **PublishWorker** (`vid_publish`) -- register the video in your CMS, set the watch page URL, configure CDN distribution rules, and update the video status to live so it appears in user-facing listings

Swap any worker for a real transcoding backend or CDN while preserving its return fields, and the video pipeline runs without changes.

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
video-processing/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/videoprocessing/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── VideoProcessingExample.java  # Main entry point (supports --workers mode)
│   └── workers/
│       ├── UploadWorker.java        # vid_upload -- ingest and store source video
│       ├── TranscodeWorker.java     # vid_transcode -- adaptive bitrate HLS
│       ├── ThumbnailWorker.java     # vid_thumbnail -- keyframe extraction
│       ├── MetadataWorker.java      # vid_metadata -- metadata index assembly
│       └── PublishWorker.java       # vid_publish -- publish to watch URL
└── src/test/java/videoprocessing/workers/
    ├── UploadWorkerTest.java        # Tests upload outputs and task def name
    ├── TranscodeWorkerTest.java     # Tests resolutions, HLS URL, determinism
    ├── ThumbnailWorkerTest.java     # Tests capture position derived from duration
    ├── MetadataWorkerTest.java      # Tests metadata map assembly
    └── PublishWorkerTest.java       # Tests publish URL and status
```
