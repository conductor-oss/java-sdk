# GPU Job Orchestration in Java Using Conductor :  Check, Allocate, Submit, Collect, Release

A Java Conductor workflow example for GPU resource orchestration .  checking GPU availability by type (A100, V100, T4), allocating a GPU for a job, submitting the training/inference workload, collecting results from the output path, and releasing the GPU back to the pool. Uses [Conductor](https://github.

## GPUs Are Expensive and Scarce

A team of ML engineers shares a cluster of 8 A100 GPUs. Without resource management, jobs compete for the same GPU, one engineer's runaway training job holds a GPU for 12 hours while others wait, and when a job crashes the GPU isn't released .  it sits idle until someone manually frees it. At $30+/hour per GPU, wasted allocation is real money.

GPU orchestration means checking which GPUs of the requested type are available, allocating one exclusively for the job, submitting the workload to the allocated GPU, collecting the output (model checkpoints, inference results) when the job finishes, and releasing the GPU so the next job can use it. If the job crashes after allocation but before release, you need guaranteed cleanup .  otherwise the GPU is leaked.

## The Solution

**You write the GPU allocation and job submission logic. Conductor handles sequencing, guaranteed cleanup, and resource tracking.**

`GpuCheckAvailabilityWorker` queries the GPU cluster for available devices of the requested type (A100, V100, T4). `GpuAllocateWorker` reserves one of the available GPUs and returns a GPU ID. `GpuSubmitJobWorker` launches the workload .  loading the model from the specified path onto the allocated GPU, and returns the output path where results will be written. `GpuCollectResultsWorker` retrieves the results from the output path. `GpuReleaseWorker` frees the GPU back to the pool. Conductor ensures the release step always runs after the job completes (or fails), preventing GPU leaks. Every execution records which GPU was used, for how long, and what output it produced.

### What You Write: Workers

Five workers manage the GPU resource lifecycle: availability check, allocation, job submission, result collection, and guaranteed release, preventing GPU leaks even when jobs crash.

| Worker | Task | What It Does |
|---|---|---|
| **GpuAllocateWorker** | `gpu_allocate` | Reserves a specific GPU (e.g., 80 GB A100) from the available pool for the training job |
| **GpuCheckAvailabilityWorker** | `gpu_check_availability` | Queries the GPU cluster for available devices by type and reports capacity |
| **GpuCollectResultsWorker** | `gpu_collect_results` | Gathers training artifacts (model weights, metrics) from the GPU job output |
| **GpuReleaseWorker** | `gpu_release` | Returns the allocated GPU back to the pool after the job completes |
| **GpuSubmitJobWorker** | `gpu_submit_job` | Gpus Submit Job and computes output path, epochs, loss val |

Workers simulate the pattern behavior with realistic inputs and outputs so you can observe the advanced workflow mechanics. Replace with real implementations .  the pattern and Conductor orchestration stay the same.

### The Workflow

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

## Running It

### Prerequisites

- **Java 21+**: verify with `java -version`
- **Maven 3.8+**: verify with `mvn -version`
- **Docker**: to run Conductor

### Option 1: Docker Compose (everything included)

```bash
docker compose up --build

```

Starts Conductor on port 8080 and runs the example automatically.

If port 8080 is already taken:

```bash
CONDUCTOR_PORT=9090 docker compose up --build

```

### Option 2: Run locally

```bash
# Start Conductor
docker run -d -p 8080:8080 -p 1234:5000 orkesio/orkes-conductor-standalone:1.2.3

# Wait for Conductor to be ready
until curl -sf http://localhost:8080/health > /dev/null; do sleep 2; done

# Build and run
mvn package -DskipTests
java -jar target/gpu-orchestration-1.0.0.jar

```

### Option 3: Use the run script

```bash
./run.sh

# Or on a custom port:
CONDUCTOR_PORT=9090 ./run.sh

# Or pointing at an existing Conductor:
CONDUCTOR_BASE_URL=http://localhost:9090/api ./run.sh

```

## Configuration

| Environment Variable | Default | Description |
|---|---|---|
| `CONDUCTOR_BASE_URL` | `http://localhost:8080/api` | Conductor server URL |
| `CONDUCTOR_PORT` | `8080` | Host port for Conductor (Docker Compose only) |

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/gpu-orchestration-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow gpu_orchestration_demo \
  --version 1 \
  --input '{"jobId": "TEST-001", "gpuType": "standard", "modelPath": "gpt-4o-mini"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w gpu_orchestration_demo -s COMPLETED -c 5

```

## How to Extend

Each worker handles one GPU lifecycle phase .  replace the simulated NVIDIA queries with real SLURM or Kubernetes device plugin APIs and the allocate-run-release pipeline runs unchanged.

- **GpuCheckAvailabilityWorker** (`gpu_check_availability`): query NVIDIA DCGM (`nvidia-smi`), Kubernetes device plugin (`kubectl describe node`), or SLURM (`sinfo -g gpu`) for real GPU availability
- **GpuSubmitJobWorker** (`gpu_submit_job`): submit real training jobs via SLURM (`sbatch`), Kubernetes Job with GPU resource requests, or AWS Batch with `p4d.24xlarge` instances
- **GpuReleaseWorker** (`gpu_release`): release GPU reservations via SLURM `scancel`, Kubernetes Job cleanup, or terminate the EC2 instance to stop billing

The allocation and release contract stays fixed. Swap the simulated cluster for real NVIDIA MIG or Kubernetes GPU scheduling and the allocate-submit-release lifecycle runs unchanged.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):

```xml
<dependency>
    <groupId>org.conductoross</groupId>
    <artifactId>conductor-client</artifactId>
    <version>5.0.1</version>
</dependency>

```

## Project Structure

```
gpu-orchestration/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/gpuorchestration/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── GpuOrchestrationExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── GpuAllocateWorker.java
│       ├── GpuCheckAvailabilityWorker.java
│       ├── GpuCollectResultsWorker.java
│       ├── GpuReleaseWorker.java
│       └── GpuSubmitJobWorker.java
└── src/test/java/gpuorchestration/workers/
    ├── GpuAllocateWorkerTest.java        # 4 tests
    ├── GpuCheckAvailabilityWorkerTest.java        # 4 tests
    ├── GpuCollectResultsWorkerTest.java        # 4 tests
    ├── GpuReleaseWorkerTest.java        # 4 tests
    └── GpuSubmitJobWorkerTest.java        # 4 tests

```
