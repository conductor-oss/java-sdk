# Fan-Out/Fan-In in Java with Conductor

Fan-Out/Fan-In. scatter-gather image processing using FORK_JOIN_DYNAMIC. Splits a variable-length image list into parallel processing tasks, compresses each independently, then aggregates results into a manifest with total savings. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers, you write the business logic, Conductor handles retries, failure routing, durability, and observability.

## The Problem

You need to process a batch of images in parallel: resizing, generating thumbnails, extracting metadata, or running object detection, where the number of images varies per request. A user uploads 5 images or 500, and each one must be processed independently at the same time. After every image is processed, the results must be aggregated into a single manifest with processed count, total size, and per-image metadata.

Without orchestration, you'd build a thread pool, submit each image as a task, manage futures for each one, wait for all completions with a barrier, and merge results manually. If processing crashes after 47 of 50 images complete, you lose the 47 already-processed results and start over. There is no way to see which images are still in flight, which have completed, or which failed. You get either all results or nothing.

## The Solution

**You just write the image preparation, processing, and aggregation workers. Conductor handles fanning out to N parallel branches via FORK_JOIN_DYNAMIC and joining the results.**

This example demonstrates the scatter-gather pattern using Conductor's FORK_JOIN_DYNAMIC. The PrepareWorker inspects the input image list and generates one `fo_process_image` task per image, each with a unique reference name (img_0_ref, img_1_ref...). Conductor fans out to N parallel branches. where N equals the number of images submitted. Each ProcessImageWorker processes its assigned image independently, compressing to 1/3 of original size and converting to WebP format. A JOIN task waits until every branch completes, then the AggregateWorker collects all per-image results from the join output into a unified manifest with processed count, total original and processed sizes, and savings percentage. If one image fails to process, Conductor retries just that branch, the other images are unaffected.

### What You Write: Workers

Three workers implement scatter-gather: PrepareWorker generates one task per image at runtime, ProcessImageWorker compresses each image independently in its own parallel branch, and AggregateWorker assembles a manifest with total savings after the join.

| Worker | Task | What It Does |
|---|---|---|
| **PrepareWorker** | `fo_prepare` | Takes an images list and generates dynamicTasks (one fo_process_image SIMPLE task per image with reference names img_0_ref, img_1_ref, etc.) and dynamicTasksInput (per-task input map with image data and index). Returns empty lists for null/empty input. |
| **ProcessImageWorker** | `fo_process_image` | Processes a single image: compresses to processedSize = originalSize / 3, converts to WebP format, and computes a deterministic processingTime based on name length and index. Returns name, originalSize, processedSize, format, and processingTime. |
| **AggregateWorker** | `fo_aggregate` | Collects all per-image results from the JOIN output, sorted by reference name. Computes processedCount, totalOriginal, totalProcessed, and savings percentage. Ignores non-image keys in the join output. |

Workers implement their processing steps so you can see the pattern in action without external services. Replace the simulation with real processing logic, the task pattern and Conductor orchestration remain unchanged.

### The Workflow

```
fo_prepare
 │
 ▼
FORK_JOIN_DYNAMIC (parallel, determined at runtime)
 │
 ▼
JOIN (wait for all branches)
fo_aggregate

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
