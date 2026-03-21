# Image Processing in Java Using Conductor : Parallel Resize, Watermark, Optimize, and Finalize

## The Problem

When a user uploads a product photo, you need to generate multiple size variants (thumbnail, medium, large for responsive images), stamp it with a copyright watermark, and compress it for web delivery. all before the image is available on the site. These three operations are independent of each other: resizing doesn't need the watermark, and optimization doesn't need the resized variants. Running them sequentially wastes time, a 10-megapixel image takes seconds per operation, and waiting for resize before starting watermark triples the latency. But they all depend on the same loaded image data, and the final step needs all three results to assemble the output.

Without orchestration, you'd spawn three threads manually, manage a CountDownLatch or CompletableFuture chain, handle what happens when watermarking fails but resize succeeds, and figure out how to retry just the failed operation without re-running the ones that already completed. If the process crashes after resize finishes but before optimize completes, you'd re-run everything from scratch. Including the resize that already succeeded. There's no record of which operations completed, how long each took, or what the intermediate results looked like.

## The Solution

**You just write the image loading, resize, watermark, optimize, and finalize workers. Conductor handles parallel image operations via FORK_JOIN, per-branch retries, and crash recovery that preserves completed branches while resuming only the failed one.**

Each image operation is a simple, independent worker. The loader fetches the image from the source URL and extracts metadata (format, dimensions, file size). The resizer generates multiple size variants from the configurable sizes list. The watermarker stamps text onto the image at the specified position and opacity. The optimizer compresses the image for the target format, reducing file size while preserving acceptable quality. The finalizer assembles resize variants, the watermarked version, and the optimized version into a single output bundle with a destination URL. Conductor runs resize, watermark, and optimize in parallel via FORK_JOIN, waits for all three to complete, then runs the finalizer. If optimize fails, Conductor retries just that branch while the resize and watermark results are preserved. ### What You Write: Workers

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

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
