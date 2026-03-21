# GPU Job Orchestration in Java Using Conductor : Check, Allocate, Submit, Collect, Release

## GPUs Are Expensive and Scarce

A team of ML engineers shares a cluster of 8 A100 GPUs. Without resource management, jobs compete for the same GPU, one engineer's runaway training job holds a GPU for 12 hours while others wait, and when a job crashes the GPU isn't released. it sits idle until someone manually frees it. At $30+/hour per GPU, wasted allocation is real money.

GPU orchestration means checking which GPUs of the requested type are available, allocating one exclusively for the job, submitting the workload to the allocated GPU, collecting the output (model checkpoints, inference results) when the job finishes, and releasing the GPU so the next job can use it. If the job crashes after allocation but before release, you need guaranteed cleanup. otherwise the GPU is leaked.

## The Solution

**You write the GPU allocation and job submission logic. Conductor handles sequencing, guaranteed cleanup, and resource tracking.**

`GpuCheckAvailabilityWorker` queries the GPU cluster for available devices of the requested type (A100, V100, T4). `GpuAllocateWorker` reserves one of the available GPUs and returns a GPU ID. `GpuSubmitJobWorker` launches the workload. loading the model from the specified path onto the allocated GPU, and returns the output path where results will be written. `GpuCollectResultsWorker` retrieves the results from the output path. `GpuReleaseWorker` frees the GPU back to the pool. Conductor ensures the release step always runs after the job completes (or fails), preventing GPU leaks. Every execution records which GPU was used, for how long, and what output it produced.

### What You Write: Workers

Five workers manage the GPU resource lifecycle: availability check, allocation, job submission, result collection, and guaranteed release, preventing GPU leaks even when jobs crash.

| Worker | Task | What It Does |
|---|---|---|
| **GpuAllocateWorker** | `gpu_allocate` | Reserves a specific GPU (e.g.### The Workflow

```
gpu_check_availability
 │
 ▼
gpu_allocate
 │
 ▼
gpu_submit_job
 │
 ▼
gpu_collect_results
 │
 ▼
gpu_release

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
