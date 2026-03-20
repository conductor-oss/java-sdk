# Video Transcoding in Java Using Conductor :  Source Analysis, Parallel 720p/1080p/4K Encoding, and Output Packaging

A Java Conductor workflow example for adaptive bitrate video transcoding: analyzing the source video to detect codec, resolution, and duration, then transcoding to three resolutions (720p, 1080p, 4K) in parallel via FORK_JOIN so all three encodes run simultaneously, then packaging all outputs into a manifest with total file sizes. The 720p encode doesn't wait for the 4K encode, and if one resolution fails, the others are unaffected. Uses [Conductor](https://github.## The Problem

When a creator uploads a video, your streaming platform needs to deliver it at multiple quality levels. 720p for mobile on cellular, 1080p for laptops, 4K for smart TVs. Each resolution requires a separate transcode pass with different bitrate targets, and a 2-hour 4K source video takes significant compute time per resolution. These three encodes are completely independent of each other: the 720p encode uses the same source video as the 4K encode, and neither needs the other's output. Running them sequentially triples the total transcoding time. But before any encoding can start, you need to analyze the source video to detect its codec, resolution, frame rate, and duration, because the encode settings depend on the source format. And after all three resolutions are ready, you need to package them into an adaptive bitrate manifest (HLS/DASH) that the player uses to switch quality based on network conditions.

Without orchestration, you'd spawn three FFmpeg processes manually, manage their lifecycle with process watchers, handle what happens when the 1080p encode fails after 45 minutes while the other two succeed, and figure out how to retry just that one resolution without re-encoding the others. If the process crashes after 720p and 1080p finish but before 4K completes, you'd re-encode all three from scratch. There's no record of the source video's properties, which encodes completed, or how long each resolution took.

## The Solution

**You just write the video analysis, 720p/1080p/4K encoding, and manifest packaging workers. Conductor handles parallel multi-resolution encoding via FORK_JOIN, per-resolution retries, and crash recovery that preserves completed encodes while resuming only the failed resolution.**

Each stage of the transcoding pipeline is a simple, independent worker. The video analyzer probes the source file to detect codec, resolution, frame rate, and duration. The three transcode workers (720p, 1080p, 4K) each encode the source video to their target resolution using the codec and duration information from the analysis. The packager assembles all three encoded outputs into a streaming manifest with per-resolution file sizes and a total size. Conductor runs the three transcode workers in parallel via FORK_JOIN, all three start as soon as analysis completes and run simultaneously. If the 4K encode fails, Conductor retries just that branch while the 720p and 1080p results are preserved. The packager only runs after all three encodes complete via the JOIN. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Five workers handle adaptive bitrate transcoding: analyzing the source video for codec and resolution, encoding to 720p, 1080p, and 4K in parallel via FORK_JOIN, and packaging all outputs into a streaming manifest.

| Worker | Task | What It Does |
|---|---|---|
| `AnalyzeVideoWorker` | `vt_analyze_video` | Probes the source video URL and extracts metadata: codec (h264), resolution (3840x2160), duration (seconds), bitrate (kbps), and frame rate |
| `Transcode720pWorker` | `vt_transcode_720p` | Encodes the source video to 720p (1280x720) using the detected codec and duration, outputs the file path and resulting size (85 MB) |
| `Transcode1080pWorker` | `vt_transcode_1080p` | Encodes the source video to 1080p (1920x1080) using the detected codec and duration, outputs the file path and resulting size (210 MB) |
| `Transcode4kWorker` | `vt_transcode_4k` | Encodes the source video to 4K (3840x2160) using the detected codec and duration, outputs the file path and resulting size (680 MB) |
| `PackageOutputsWorker` | `vt_package_outputs` | Collects the three transcoded file paths and original duration, assembles a delivery manifest listing all resolutions with a total size (975 MB) and format count |

Workers simulate data processing stages with representative outputs so the pipeline runs end-to-end without external data stores. Swap in real data sources and sinks .  the pipeline structure and error handling stay the same.

### The Workflow

```
vt_analyze_video
    │
    ▼
FORK_JOIN
    ├── vt_transcode_720p
    ├── vt_transcode_1080p
    └── vt_transcode_4k
    │
    ▼
JOIN (wait for all branches)
vt_package_outputs
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
java -jar target/video-transcoding-1.0.0.jar
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
java -jar target/video-transcoding-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow video_transcoding \
  --version 1 \
  --input '{"videoUrl": "TEST-001", "outputFormats": "test-value"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w video_transcoding -s COMPLETED -c 5
```

## How to Extend

Replace the simulated encoders with real FFmpeg commands targeting H.264/H.265 at each resolution, and package into HLS/DASH manifests, the parallel transcoding workflow runs unchanged.

- **AnalyzeVideoWorker** → use real video analysis: FFprobe for codec/resolution/duration detection, MediaInfo for detailed stream information, or cloud-based analysis via AWS MediaConvert or Google Transcoder API probe endpoints
- **Transcode720pWorker / Transcode1080pWorker / Transcode4kWorker** → call real video encoders: FFmpeg with libx264/libx265 for H.264/HEVC encoding, AWS Elastic Transcoder or MediaConvert for cloud-based encoding, or hardware-accelerated encoding via NVENC/QSV for GPU-equipped workers
- **PackageOutputsWorker** → generate real adaptive bitrate manifests: HLS (m3u8) playlists for Apple devices, DASH (mpd) manifests for cross-platform streaming, or CMAF for unified packaging, then upload to CDN storage (S3 + CloudFront, GCS + Cloud CDN)

Replacing simulated encodes with real FFmpeg processes or adding a new resolution tier leaves the parallel transcoding workflow unchanged, provided each encoder returns the expected file size and codec metadata.

**Add new branches** to the FORK_JOIN in `workflow.json`, for example, a thumbnail extraction branch that captures key frames at intervals, an audio-only branch that extracts and encodes AAC/Opus audio for podcast-style consumption, or a subtitle extraction branch that pulls embedded captions into WebVTT format.

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
video-transcoding/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/videotranscoding/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── VideoTranscodingExample.java          # Main entry point (supports --workers mode)
│   └── workers/
```
