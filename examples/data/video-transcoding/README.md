# Video Transcoding in Java Using Conductor : Source Analysis, Parallel 720p/1080p/4K Encoding, and Output Packaging

## The Problem

When a creator uploads a video, your streaming platform needs to deliver it at multiple quality levels. 720p for mobile on cellular, 1080p for laptops, 4K for smart TVs. Each resolution requires a separate transcode pass with different bitrate targets, and a 2-hour 4K source video takes significant compute time per resolution. These three encodes are completely independent of each other: the 720p encode uses the same source video as the 4K encode, and neither needs the other's output. Running them sequentially triples the total transcoding time. But before any encoding can start, you need to analyze the source video to detect its codec, resolution, frame rate, and duration, because the encode settings depend on the source format. And after all three resolutions are ready, you need to package them into an adaptive bitrate manifest (HLS/DASH) that the player uses to switch quality based on network conditions.

Without orchestration, you'd spawn three FFmpeg processes manually, manage their lifecycle with process watchers, handle what happens when the 1080p encode fails after 45 minutes while the other two succeed, and figure out how to retry just that one resolution without re-encoding the others. If the process crashes after 720p and 1080p finish but before 4K completes, you'd re-encode all three from scratch. There's no record of the source video's properties, which encodes completed, or how long each resolution took.

## The Solution

**You just write the video analysis, 720p/1080p/4K encoding, and manifest packaging workers. Conductor handles parallel multi-resolution encoding via FORK_JOIN, per-resolution retries, and crash recovery that preserves completed encodes while resuming only the failed resolution.**

Each stage of the transcoding pipeline is a simple, independent worker. The video analyzer probes the source file to detect codec, resolution, frame rate, and duration. The three transcode workers (720p, 1080p, 4K) each encode the source video to their target resolution using the codec and duration information from the analysis. The packager assembles all three encoded outputs into a streaming manifest with per-resolution file sizes and a total size. Conductor runs the three transcode workers in parallel via FORK_JOIN, all three start as soon as analysis completes and run simultaneously. If the 4K encode fails, Conductor retries just that branch while the 720p and 1080p results are preserved. The packager only runs after all three encodes complete via the JOIN. ### What You Write: Workers

Five workers handle adaptive bitrate transcoding: analyzing the source video for codec and resolution, encoding to 720p, 1080p, and 4K in parallel via FORK_JOIN, and packaging all outputs into a streaming manifest.

| Worker | Task | What It Does |
|---|---|---|
| `AnalyzeVideoWorker` | `vt_analyze_video` | Probes the source video URL and extracts metadata: codec (h264), resolution (3840x2160), duration (seconds), bitrate (kbps), and frame rate |
| `Transcode720pWorker` | `vt_transcode_720p` | Encodes the source video to 720p (1280x720) using the detected codec and duration, outputs the file path and resulting size (85 MB) |
| `Transcode1080pWorker` | `vt_transcode_1080p` | Encodes the source video to 1080p (1920x1080) using the detected codec and duration, outputs the file path and resulting size (210 MB) |
| `Transcode4kWorker` | `vt_transcode_4k` | Encodes the source video to 4K (3840x2160) using the detected codec and duration, outputs the file path and resulting size (680 MB) |
| `PackageOutputsWorker` | `vt_package_outputs` | Collects the three transcoded file paths and original duration, assembles a delivery manifest listing all resolutions with a total size (975 MB) and format count |

Workers implement data processing stages with representative outputs so the pipeline runs end-to-end without external data stores. Swap in real data sources and sinks. the pipeline structure and error handling stay the same.

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

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
