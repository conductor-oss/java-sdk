# Video Processing Pipeline in Java Using Conductor: Upload, Adaptive Transcode, Thumbnail Generation, Metadata Indexing, and Publishing

A creator uploads a 742MB 4K video. Your monolithic transcoder starts the 1080p rendition, runs out of memory halfway through the 720p, and dies. The thumbnail generator, which somebody wired to run concurrently. extracts a frame from the half-transcoded file and gets a green-and-black glitch image. The metadata indexer never ran at all, so the video doesn't appear in search. The publish step has no idea any of this happened and pushes a broken HLS manifest to the CDN. Now there's a live watch URL serving a video that plays for 40 seconds and then freezes. This example orchestrates the full video pipeline with Conductor: upload with codec detection, adaptive bitrate transcoding (1080p/720p/480p), thumbnail extraction at a computed keyframe, metadata indexing, and publishing, each step sequenced with proper dependency handling and per-step retries. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers, you write the business logic, Conductor handles retries, failure routing, durability, and observability.

## Why Video Processing Needs Orchestration

Processing video for delivery involves a pipeline where each stage produces assets that downstream stages depend on. You ingest the source file: detecting the codec (h264, h265, VP9), measuring duration, and storing the raw file to object storage with its original quality preserved. You transcode to adaptive bitrate HLS, generating resolution renditions at 1080p (5000k), 720p (2500k), and 480p (1000k) so the player can switch quality based on network conditions. You generate a thumbnail by extracting a frame at a computed position (one-third of the duration, avoiding black intro frames). You index metadata, title, duration, available resolutions, content type, so the video appears in search results and recommendation feeds. Finally, you publish the video with its HLS manifest, thumbnail, and metadata to a live watch URL.

Each stage depends on specific outputs from earlier stages: transcoding needs the storage path from upload, thumbnail generation needs the duration, metadata indexing needs the resolution list from transcoding, and publishing needs the HLS URL, thumbnail URL, and metadata. If transcoding fails partway through (the 720p rendition succeeds but 480p times out), you need to retry just the failed rendition, not re-upload the 742 MB source file. Without orchestration, you'd build a monolithic video processor that mixes FFmpeg calls, S3 uploads, thumbnail extraction, and database writes, making it impossible to swap your transcoding backend (FFmpeg to MediaConvert), add new rendition profiles, or trace why a specific video's thumbnail shows a black frame.

## How This Workflow Solves It

**You just write the video processing workers. Upload ingestion, adaptive transcode, thumbnail generation, metadata indexing, and publishing. Conductor handles transcoding sequencing, FFmpeg timeout retries, and end-to-end tracking from upload through live publication.**

Each video processing stage is an independent worker. Upload, transcode, thumbnail, metadata, publish. Conductor sequences them, passes the storage path from upload into transcode and thumbnail, feeds the resolution list and HLS URL from transcode into metadata and publish, retries if an FFmpeg process times out, and tracks every video from upload through live publication.

### What You Write: Workers

Five workers process each video: UploadWorker ingests the source with codec detection, TranscodeWorker creates adaptive bitrate renditions, ThumbnailWorker extracts a preview frame, MetadataWorker builds the search index, and PublishWorker makes the video live.

| Worker | Task | What It Does |
|---|---|---|
| **UploadWorker** | `vid_upload` | Ingests source video from a URL, stores it to object storage, and detects codec (`h264`), duration (`185s`), and file size (`742 MB`). Returns `storagePath`, `duration`, `fileSizeMb`, `codec`. |
| **TranscodeWorker** | `vid_transcode` | Transcodes the raw video to adaptive bitrate HLS with three resolution renditions: 1080p (5000k), 720p (2500k), 480p (1000k). Returns `resolutions` list, `hlsUrl`, `format`. |
| **ThumbnailWorker** | `vid_thumbnail` | Generates a thumbnail by extracting a frame at one-third of the video duration (avoiding black intro frames). Returns `thumbnailUrl`, `capturedAtSecond`, `width`, `height`. |
| **MetadataWorker** | `vid_metadata` | Assembles a metadata index from upstream outputs: title, duration, resolution list, content type, for search and discovery. Returns a `metadata` map. |
| **PublishWorker** | `vid_publish` | Publishes the video with its HLS manifest, thumbnail, and metadata to a live watch URL. Returns `publishUrl`, `publishedAt`, `status`. |

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

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
