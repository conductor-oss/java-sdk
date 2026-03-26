# Image Pipeline

Orchestrates image pipeline through a multi-stage Conductor workflow.

**Input:** `imageId`, `sourceUrl`, `targetSizes`, `watermarkText` | **Timeout:** 60s

## Pipeline

```
imp_upload_image
    │
imp_resize_image
    │
imp_optimize_image
    │
imp_watermark_image
    │
imp_push_cdn
```

## Workers

**OptimizeImageWorker** (`imp_optimize_image`)

Reads `optimizedPaths`. Outputs `optimizedPaths`, `savedPercent`, `averageQuality`.

**PushCdnWorker** (`imp_push_cdn`)

Reads `cdnUrls`. Outputs `cdnUrls`, `cdnBaseUrl`, `cacheInvalidated`, `ttlSeconds`.

**ResizeImageWorker** (`imp_resize_image`)

Reads `resizedPaths`. Outputs `resizedPaths`.

**UploadImageWorker** (`imp_upload_image`)

Reads `storagePath`. Outputs `storagePath`, `originalWidth`, `originalHeight`, `fileSizeKb`, `format`.

**WatermarkImageWorker** (`imp_watermark_image`)

Reads `watermarkedPaths`. Outputs `watermarkedPaths`, `watermarkApplied`.

## Tests

**4 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
