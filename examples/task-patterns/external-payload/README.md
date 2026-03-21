# External Payload in Java with Conductor

External payload storage. generate a summary and storage reference instead of returning large data, then process the summary. ## The Problem

You need to pass large data between workflow tasks. a report with millions of rows, a dataset for ML training, or a full database export; but Conductor's task output has a size limit. Storing megabytes of raw data directly in task output would bloat the workflow execution record, slow down the Conductor server, and eventually hit payload size limits. The downstream task only needs a summary and a pointer to the full data, not the data itself.

Without orchestration, you'd write the large payload to S3 or a file share, pass the reference manually between functions, handle the case where the upload succeeds but the reference never reaches the consumer, and manage cleanup of orphaned files when processing fails. If the process crashes between writing the data and recording the reference, the data is stranded with no way to locate it.

## The Solution

**You just write the payload generation and summary processing workers. Conductor handles the lightweight reference passing without storing bulk data.**

This example demonstrates the external payload storage pattern. keeping large data out of Conductor's task output. The GenerateWorker produces the large dataset but instead of returning it inline, it writes the data to external storage (S3, GCS, or a file system) and returns only a lightweight summary and a `storageRef` pointer. The downstream ProcessWorker receives just the summary to make routing decisions, and can fetch the full data from the storage reference when needed. Conductor tracks the summary and reference without ever storing the bulk payload, keeping workflow executions fast and the server lean.

### What You Write: Workers

Two workers demonstrate the external payload pattern: GenerateWorker produces a large dataset and returns only a lightweight summary with a storage reference, while ProcessWorker consumes that summary for downstream decisions.

| Worker | Task | What It Does |
|---|---|---|
| **GenerateWorker** | `ep_generate` | Generates a summary and storage reference for a large payload. Instead of returning the full data (which could exceed... |
| **ProcessWorker** | `ep_process` | Processes the summary from the generate step. In a real system, this worker could also fetch the full data from the s... |

Workers implement their processing steps so you can see the pattern in action without external services. Replace the simulation with real processing logic. the task pattern and Conductor orchestration remain unchanged.

### The Workflow

```
ep_generate
 │
 ▼
ep_process

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
