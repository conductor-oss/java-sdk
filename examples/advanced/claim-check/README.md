# Claim Check Pattern in Java Using Conductor : Offload Large Payloads, Pass by Reference

## Keeping Large Payloads Out of Your Message Bus

Workflow tasks exchange data through their inputs and outputs, but passing a 50 MB medical image or a 200 MB CSV dataset directly between tasks bloats every message, slows serialization, and can hit broker size limits. The claim check pattern solves this: store the large payload in blob storage, pass a small reference ID (the "claim check") through the pipeline, and retrieve the full data only in the task that actually needs it.

The challenge is coordinating the store-reference-retrieve lifecycle reliably. If the store succeeds but the reference gets lost, the payload is orphaned. If the retrieve fails, you need to retry without re-storing. And you need to track the payload size and storage location for debugging without actually passing the payload through the orchestrator.

## The Solution

**You write the storage and retrieval logic. Conductor handles the reference passing, retries, and payload lineage.**

`StorePayloadWorker` writes the large payload to external storage and returns a lightweight claim check ID plus the storage location and payload size in bytes. `PassReferenceWorker` forwards just the claim check ID through the pipeline. no bulky data, just a pointer. `RetrieveWorker` fetches the full payload from storage using the claim check reference. `ProcessWorker` operates on the retrieved data (e.g., computing metric averages). Conductor ensures the chain executes in order, retries any failed retrieve or store operation, and records the claim check ID at every step so you can trace payload lineage.

### What You Write: Workers

Four workers manage the store-reference-retrieve lifecycle: payload storage, reference passing, retrieval, and processing, keeping large data out of the orchestration bus.

| Worker | Task | What It Does |
|---|---|---|
| **PassReferenceWorker** | `clc_pass_reference` | Passes the lightweight claim check reference through the pipeline. |
| **ProcessWorker** | `clc_process` | Processes the retrieved payload and computes metric averages. |
| **RetrieveWorker** | `clc_retrieve` | Retrieves the full payload from storage using the claim check reference. |
| **StorePayloadWorker** | `clc_store_payload` | Stores a large payload in external storage and returns a lightweight claim check reference.### The Workflow

```
clc_store_payload
 │
 ▼
clc_pass_reference
 │
 ▼
clc_retrieve
 │
 ▼
clc_process

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
