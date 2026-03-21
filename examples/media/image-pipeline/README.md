# Image Processing Pipeline in Java Using Conductor : Upload, Resize, Optimize, Watermark, and CDN Push

## Why Image Processing Pipelines Need Orchestration

Processing images for web delivery requires a strict transformation chain. You upload the original and extract its dimensions, format, and file size. You resize it to multiple breakpoints (thumbnail, mobile, tablet, desktop, retina). You optimize each variant. reducing file size by 40-60% while maintaining visual quality. You apply watermarks to protect intellectual property. Finally, you push all variants to the CDN with appropriate cache headers and TTLs.

Each stage depends on the previous one. you cannot optimize before resizing, and you cannot push to CDN before watermarking. If optimization fails for one variant, you need to retry just that variant without re-uploading the original or re-resizing everything. Without orchestration, you'd build a monolithic image processor that mixes file I/O, image manipulation libraries, compression algorithms, and CDN APIs, making it impossible to swap your compression engine, add a new output format (WebP, AVIF), or trace which processing step introduced a visual artifact.

## How This Workflow Solves It

**You just write the image processing workers. Upload handling, resizing, optimization, watermarking, and CDN push. Conductor handles transformation ordering, CDN push retries, and file-size tracking at every stage for compression analysis.**

Each processing stage is an independent worker. upload, resize, optimize, watermark, push to CDN. Conductor sequences them, passes storage paths and variant lists between stages, retries if a CDN push times out, and tracks file sizes at every step so you can measure compression effectiveness.

### What You Write: Workers

Five workers process each image: UploadImageWorker handles ingestion with dimension detection, ResizeImageWorker creates responsive breakpoints, OptimizeImageWorker compresses for web delivery, WatermarkImageWorker applies brand protection, and PushCdnWorker distributes to edge nodes.

| Worker | Task | What It Does |
|---|---|---|
| **OptimizeImageWorker** | `imp_optimize_image` | Optimizes the image |
| **PushCdnWorker** | `imp_push_cdn` | Handles push cdn |
| **ResizeImageWorker** | `imp_resize_image` | Handles resize image |
| **UploadImageWorker** | `imp_upload_image` | Uploads the image |
| **WatermarkImageWorker** | `imp_watermark_image` | Handles watermark image |

Workers implement media processing stages. transcoding, thumbnail generation, metadata extraction, with realistic output artifacts. Replace with real media tools (FFmpeg, ImageMagick) and the pipeline stays the same.

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

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
