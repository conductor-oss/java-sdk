# Video Transcoding

A streaming platform accepts user-uploaded videos in arbitrary codecs and resolutions. Each upload needs probe analysis (codec, resolution, bitrate, duration), transcoding to multiple output profiles (720p, 1080p, 4K), and verification that each output file is playable and matches the target spec.

## Pipeline

```
[vt_analyze_video]
     |
     v
     +────────────────────────────────────────────────────────────────+
     | [vt_transcode_720p] | [vt_transcode_1080p] | [vt_transcode_4k] |
     +────────────────────────────────────────────────────────────────+
     [join]
     |
     v
[vt_package_outputs]
```

**Workflow inputs:** `videoUrl`, `outputFormats`

## Workers

**AnalyzeVideoWorker** (task: `vt_analyze_video`)

Analyzes the source video to extract codec, resolution, duration, and bitrate metadata.

- Reads `videoUrl`

**PackageOutputsWorker** (task: `vt_package_outputs`)

Packages all transcoded outputs into a delivery manifest with format count and total size.

- Reads `result720`, `result1080`, `result4k`, `originalDuration`

**Transcode1080pWorker** (task: `vt_transcode_1080p`)

Transcodes the source video to 1080p resolution.

- Sets `result` = `"/output/video_1080p.mp4"`
- Reads `videoUrl`, `codec`, `duration`

**Transcode4kWorker** (task: `vt_transcode_4k`)

Transcodes the source video to 4K resolution.

- Sets `result` = `"/output/video_4k.mp4"`
- Reads `videoUrl`, `codec`, `duration`

**Transcode720pWorker** (task: `vt_transcode_720p`)

Transcodes the source video to 720p resolution.

- Sets `result` = `"/output/video_720p.mp4"`
- Reads `videoUrl`, `codec`, `duration`

---

**0 tests** | Workflow: `video_transcoding` | Timeout: 60s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
