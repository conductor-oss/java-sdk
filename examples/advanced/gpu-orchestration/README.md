# Gpu Orchestration

A deep learning training job requires GPU resources that are expensive and scarce. The orchestration pipeline needs to check GPU availability, allocate the right GPU type for the model architecture, launch the training job, monitor utilization, and release the GPU immediately when training completes.

## Pipeline

```
[gpu_check_availability]
     |
     v
[gpu_allocate]
     |
     v
[gpu_submit_job]
     |
     v
[gpu_collect_results]
     |
     v
[gpu_release]
```

**Workflow inputs:** `jobId`, `gpuType`, `modelPath`

## Workers

**GpuAllocateWorker** (task: `gpu_allocate`)

- Uses randomization
- Writes `gpuId`, `allocated`, `memoryGb`

**GpuCheckAvailabilityWorker** (task: `gpu_check_availability`)

- Writes `available`, `gpuType`, `cluster`

**GpuCollectResultsWorker** (task: `gpu_collect_results`)

- Writes `collected`, `artifacts`

**GpuReleaseWorker** (task: `gpu_release`)

- Writes `released`

**GpuSubmitJobWorker** (task: `gpu_submit_job`)

- Writes `outputPath`, `epochs`, `lossVal`

---

**20 tests** | Workflow: `gpu_orchestration_demo` | Timeout: 60s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
