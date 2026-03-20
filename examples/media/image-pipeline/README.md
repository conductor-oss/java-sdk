# Image Processing Pipeline in Java Using Conductor :  Upload, Resize, Optimize, Watermark, and CDN Push

A Java Conductor workflow example that orchestrates an image processing pipeline .  uploading original images with dimension and format detection, resizing to multiple responsive breakpoints, optimizing file sizes with quality-aware compression, applying watermarks for brand protection, and pushing final assets to CDN with cache invalidation and TTL configuration. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers ,  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## Why Image Processing Pipelines Need Orchestration

Processing images for web delivery requires a strict transformation chain. You upload the original and extract its dimensions, format, and file size. You resize it to multiple breakpoints (thumbnail, mobile, tablet, desktop, retina). You optimize each variant .  reducing file size by 40-60% while maintaining visual quality. You apply watermarks to protect intellectual property. Finally, you push all variants to the CDN with appropriate cache headers and TTLs.

Each stage depends on the previous one .  you cannot optimize before resizing, and you cannot push to CDN before watermarking. If optimization fails for one variant, you need to retry just that variant without re-uploading the original or re-resizing everything. Without orchestration, you'd build a monolithic image processor that mixes file I/O, image manipulation libraries, compression algorithms, and CDN APIs ,  making it impossible to swap your compression engine, add a new output format (WebP, AVIF), or trace which processing step introduced a visual artifact.

## How This Workflow Solves It

**You just write the image processing workers. Upload handling, resizing, optimization, watermarking, and CDN push. Conductor handles transformation ordering, CDN push retries, and file-size tracking at every stage for compression analysis.**

Each processing stage is an independent worker .  upload, resize, optimize, watermark, push to CDN. Conductor sequences them, passes storage paths and variant lists between stages, retries if a CDN push times out, and tracks file sizes at every step so you can measure compression effectiveness.

### What You Write: Workers

Five workers process each image: UploadImageWorker handles ingestion with dimension detection, ResizeImageWorker creates responsive breakpoints, OptimizeImageWorker compresses for web delivery, WatermarkImageWorker applies brand protection, and PushCdnWorker distributes to edge nodes.

| Worker | Task | What It Does |
|---|---|---|
| **OptimizeImageWorker** | `imp_optimize_image` | Optimizes the image |
| **PushCdnWorker** | `imp_push_cdn` | Handles push cdn |
| **ResizeImageWorker** | `imp_resize_image` | Handles resize image |
| **UploadImageWorker** | `imp_upload_image` | Uploads the image |
| **WatermarkImageWorker** | `imp_watermark_image` | Handles watermark image |

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
imp_upload_image
    │
    ▼
imp_resize_image
    │
    ▼
imp_optimize_image
    │
    ▼
imp_watermark_image
    │
    ▼
imp_push_cdn
```

## Example Output

```
=== Example 513: Image Pipeline ===

Step 1: Registering task definitions...
  Registered: imp_upload_image, imp_resize_image, imp_optimize_image, imp_watermark_image, imp_push_cdn

Step 2: Registering workflow 'image_pipeline_workflow'...
  Workflow registered.

Step 3: Starting workers...
  5 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [optimize] Processing
  [cdn] Processing
  [resize] Processing
  [upload] Processing
  [watermark] Processing

  Status: COMPLETED
  Output: {optimizedPaths=..., savedPercent=..., averageQuality=..., cdnUrls=...}

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
java -jar target/image-pipeline-1.0.0.jar
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
java -jar target/image-pipeline-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow image_pipeline_workflow \
  --version 1 \
  --input '{"imageId": "IMG-513-001", "IMG-513-001": "sourceUrl", "sourceUrl": "https://uploads.example.com/photos/513.jpg", "https://uploads.example.com/photos/513.jpg": "targetSizes", "targetSizes": ["item-1", "item-2", "item-3"], "Example Corp": "sample-Example Corp"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w image_pipeline_workflow -s COMPLETED -c 5
```

## How to Extend

Connect ResizeImageWorker to your image library (ImageMagick, libvips), OptimizeImageWorker to your compression service (TinyPNG, Squoosh), and PushCdnWorker to your CDN (CloudFront, Fastly). The workflow definition stays exactly the same.

- **UploadImageWorker** (`imp_upload_image`): handle real file uploads, extract EXIF metadata and dimensions using ImageIO or libvips, and store the original in S3/GCS
- **ResizeImageWorker** (`imp_resize_image`): resize images to multiple breakpoints using ImageMagick, libvips, or Sharp, outputting paths for each variant (thumbnail, mobile, desktop, retina)
- **OptimizeImageWorker** (`imp_optimize_image`): compress images using MozJPEG, pngquant, or WebP/AVIF encoders, tracking size savings and average quality scores
- **WatermarkImageWorker** (`imp_watermark_image`): overlay watermarks using image composition libraries, positioning and opacity configurable per use case (full overlay, corner stamp)
- **PushCdnWorker** (`imp_push_cdn`): upload all variants to CDN origins (CloudFront, Fastly, Imgix), set cache TTLs, invalidate stale versions, and return HTTPS delivery URLs

Swap any worker for a production image library or CDN API while keeping the same return schema, and the processing pipeline remains unchanged.

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
image-pipeline/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/imagepipeline/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ImagePipelineExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── OptimizeImageWorker.java
│       ├── PushCdnWorker.java
│       ├── ResizeImageWorker.java
│       ├── UploadImageWorker.java
│       └── WatermarkImageWorker.java
└── src/test/java/imagepipeline/workers/
    ├── ResizeImageWorkerTest.java        # 2 tests
    └── UploadImageWorkerTest.java        # 2 tests
```
