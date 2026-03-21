# Live Streaming Pipeline in Java Using Conductor : Stream Setup, Encoding, CDN Distribution, Quality Monitoring, and Archival

## Why Live Streaming Workflows Need Orchestration

Managing a live stream involves a sequence of infrastructure operations that must happen in the right order. You set up the stream. provisioning an RTMP ingest URL and stream key. You configure encoding, setting up adaptive bitrate transcoding at multiple quality levels with low-latency settings. You distribute the encoded stream to CDN edge nodes across viewer regions. You monitor quality throughout, tracking peak concurrent viewers, average bitrate, buffer ratios, and computing an overall quality score. After the stream ends, you archive the recording with generated thumbnails for on-demand playback.

If stream setup fails, encoding must not start. If CDN distribution drops in one region, monitoring should detect the quality degradation. If the process crashes during archival, you need to resume from archiving. not re-encode the entire stream. Without orchestration, you'd build a monolithic streaming controller that mixes infrastructure provisioning, transcoding configuration, CDN management, real-time monitoring, and archival, making it impossible to swap CDN providers, upgrade your encoder, or trace why a specific stream had buffering issues.

## How This Workflow Solves It

**You just write the streaming workers. Stream setup, encoding, CDN distribution, quality monitoring, and archival. Conductor handles infrastructure sequencing, CDN failover retries, and quality metric recording for post-broadcast analysis.**

Each streaming stage is an independent worker. setup, encode, distribute, monitor quality, archive. Conductor sequences them, passes stream keys and playback URLs between stages, retries if a CDN node fails to respond, and records quality metrics for every stream for post-broadcast analysis.

### What You Write: Workers

Five workers manage the broadcast lifecycle: SetupStreamWorker provisions ingest URLs, EncodeStreamWorker configures adaptive bitrate transcoding, DistributeStreamWorker pushes to CDN edge nodes, MonitorQualityWorker tracks viewer metrics, and ArchiveStreamWorker creates the VOD recording.

| Worker | Task | What It Does |
|---|---|---|
| **ArchiveStreamWorker** | `lsm_archive_stream` | Archives the completed stream for VOD access. |
| **DistributeStreamWorker** | `lsm_distribute_stream` | Distributes the encoded stream to CDN edge nodes. |
| **EncodeStreamWorker** | `lsm_encode_stream` | Encodes the live stream with adaptive bitrate profiles. |
| **MonitorQualityWorker** | `lsm_monitor_quality` | Monitors stream quality metrics. |
| **SetupStreamWorker** | `lsm_setup_stream` | Sets up a live stream with ingest URL and stream key. |

Workers implement media processing stages. transcoding, thumbnail generation, metadata extraction, with realistic output artifacts. Replace with real media tools (FFmpeg, ImageMagick) and the pipeline stays the same.

### The Workflow

```
Input -> ArchiveStreamWorker -> DistributeStreamWorker -> EncodeStreamWorker -> MonitorQualityWorker -> SetupStreamWorker -> Output

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
