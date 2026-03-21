# Image Processing in Java Using Conductor :  Parallel Resize, Watermark, Optimize, and Finalize

A Java Conductor workflow example for image processing: loading an image from a URL, then running three operations in parallel (resizing to multiple size variants, applying a text watermark, and compressing/optimizing the file size), then finalizing by assembling all outputs into a single result. Uses a FORK_JOIN to process resize, watermark, and optimize concurrently, no operation waits for another, so a 4000x3000 image gets all three treatments at the same time. Uses [Conductor](https://github.

## The Problem

When a user uploads a product photo, you need to generate multiple size variants (thumbnail, medium, large for responsive images), stamp it with a copyright watermark, and compress it for web delivery. all before the image is available on the site. These three operations are independent of each other: resizing doesn't need the watermark, and optimization doesn't need the resized variants. Running them sequentially wastes time, a 10-megapixel image takes seconds per operation, and waiting for resize before starting watermark triples the latency. But they all depend on the same loaded image data, and the final step needs all three results to assemble the output.

Without orchestration, you'd spawn three threads manually, manage a CountDownLatch or CompletableFuture chain, handle what happens when watermarking fails but resize succeeds, and figure out how to retry just the failed operation without re-running the ones that already completed. If the process crashes after resize finishes but before optimize completes, you'd re-run everything from scratch. Including the resize that already succeeded. There's no record of which operations completed, how long each took, or what the intermediate results looked like.

## The Solution

**You just write the image loading, resize, watermark, optimize, and finalize workers. Conductor handles parallel image operations via FORK_JOIN, per-branch retries, and crash recovery that preserves completed branches while resuming only the failed one.**

Each image operation is a simple, independent worker. The loader fetches the image from the source URL and extracts metadata (format, dimensions, file size). The resizer generates multiple size variants from the configurable sizes list. The watermarker stamps text onto the image at the specified position and opacity. The optimizer compresses the image for the target format, reducing file size while preserving acceptable quality. The finalizer assembles resize variants, the watermarked version, and the optimized version into a single output bundle with a destination URL. Conductor runs resize, watermark, and optimize in parallel via FORK_JOIN, waits for all three to complete, then runs the finalizer. If optimize fails, Conductor retries just that branch while the resize and watermark results are preserved. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Five workers handle image processing: loading from a URL, then running resize, watermark, and optimize in parallel via FORK_JOIN, and finally assembling all outputs into a single result bundle.

| Worker | Task | What It Does |
|---|---|---|
| **FinalizeWorker** | `ip_finalize` | Finalizes image processing by assembling all outputs. |
| **LoadImageWorker** | `ip_load_image` | Loads an image from a URL. |
| **OptimizeWorker** | `ip_optimize` | Optimizes/compresses an image. |
| **ResizeWorker** | `ip_resize` | Resizes an image to multiple variants. |
| **WatermarkWorker** | `ip_watermark` | Applies a watermark to an image. |

Workers implement data processing stages with representative outputs so the pipeline runs end-to-end without external data stores. Swap in real data sources and sinks. the pipeline structure and error handling stay the same.

### The Workflow

```
ip_load_image
    │
    ▼
FORK_JOIN
    ├── ip_resize
    ├── ip_watermark
    └── ip_optimize
    │
    ▼
JOIN (wait for all branches)
ip_finalize

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
java -jar target/image-processing-1.0.0.jar

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
java -jar target/image-processing-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow image_processing \
  --version 1 \
  --input '{"imageUrl": "https://example.com", "sizes": 10, "watermarkText": "Process this order for customer C-100"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w image_processing -s COMPLETED -c 5

```

## How to Extend

Swap in ImageMagick or libvips for resizing, apply real watermarks via Graphics2D, compress with mozjpeg or cwebp, and the parallel image processing workflow runs unchanged.

- **LoadImageWorker** → fetch images from real storage (S3, GCS, Azure Blob, or CDN URLs) using the AWS SDK or HTTP client, and extract EXIF metadata (dimensions, color space, orientation) for downstream operations
- **ResizeWorker** → use ImageMagick, libvips, or Thumbnailator to generate real size variants with proper aspect ratio preservation, sharpening after downscale, and format-specific quality settings (WebP for web, AVIF for modern browsers)
- **WatermarkWorker** → apply real watermarks using Graphics2D or ImageMagick: semi-transparent text or logo overlay with configurable position, opacity, font, and tiling for stock photo protection
- **OptimizeWorker** → compress images with real tools (mozjpeg for JPEG, pngquant for PNG, cwebp for WebP) targeting specific file size budgets or quality thresholds, and strip unnecessary metadata
- **FinalizeWorker** → upload all variants to CDN storage (S3 + CloudFront, GCS + Cloud CDN), write the variant manifest to a database, and invalidate CDN cache for the previous version

Swapping in real ImageMagick resizing, a production watermark service, or WebP optimization leaves the parallel workflow unchanged, as long as each worker returns the expected image metadata and output paths.

**Add new branches** to the FORK_JOIN in `workflow.json`, for example, a face detection branch that identifies faces for auto-cropping, an NSFW content moderation branch that flags inappropriate images before publishing, or a background removal branch for e-commerce product photos.

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
image-processing/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/imageprocessing/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ImageProcessingExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── FinalizeWorker.java
│       ├── LoadImageWorker.java
│       ├── OptimizeWorker.java
│       ├── ResizeWorker.java
│       └── WatermarkWorker.java
└── src/test/java/imageprocessing/workers/
    ├── FinalizeWorkerTest.java        # 8 tests
    ├── LoadImageWorkerTest.java        # 8 tests
    ├── OptimizeWorkerTest.java        # 8 tests
    ├── ResizeWorkerTest.java        # 8 tests
    └── WatermarkWorkerTest.java        # 8 tests

```
