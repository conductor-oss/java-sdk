# Image Processing

A real estate platform receives listing photos in arbitrary resolutions and formats. Each image needs validation (minimum resolution, supported format), resizing to standard dimensions, metadata extraction (EXIF, dimensions), and watermarking before it can appear on the website. A corrupt JPEG should not block the entire batch.

## Pipeline

```
[ip_load_image]
     |
     v
     +──────────────────────────────────────────────+
     | [ip_resize] | [ip_watermark] | [ip_optimize] |
     +──────────────────────────────────────────────+
     [join]
     |
     v
[ip_finalize]
```

**Workflow inputs:** `imageUrl`, `sizes`, `watermarkText`

## Workers

**FinalizeWorker** (task: `ip_finalize`)

Finalizes image processing by assembling all outputs.

- Reads `resized`. Writes `outputUrl`, `totalOutputs`

**LoadImageWorker** (task: `ip_load_image`)

Loads an image from a URL.

- Sets `format` = `"png"`
- Reads `imageUrl`. Writes `imageData`, `width`, `height`, `originalSize`, `format`

**OptimizeWorker** (task: `ip_optimize`)

Optimizes/compresses an image.

- Sets `result` = `"optimized"`
- Reads `format`. Writes `result`, `optimizedSize`, `compressionRatio`, `format`

**ResizeWorker** (task: `ip_resize`)

Resizes an image to multiple variants.

- Rounds with `math.round()`
- Reads `sizes`. Writes `variants`, `variantCount`

**WatermarkWorker** (task: `ip_watermark`)

Applies a watermark to an image.

- Sets `result` = `"watermarked"`
- Reads `text`. Writes `result`, `applied`, `position`, `opacity`

---

**40 tests** | Workflow: `image_processing` | Timeout: 60s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
