# Claim Check

A workflow needs to pass a 500 MB payload between tasks, but the message broker has a 256 KB message size limit. The claim-check pattern stores the large payload in external storage, passes a lightweight reference (the "claim check") through the workflow, and retrieves the full payload only when a task actually needs it.

## Pipeline

```
[clc_store_payload]
     |
     v
[clc_pass_reference]
     |
     v
[clc_retrieve]
     |
     v
[clc_process]
```

**Workflow inputs:** `payload`, `storageType`

## Workers

**PassReferenceWorker** (task: `clc_pass_reference`)

Passes the lightweight claim check reference through the pipeline.

- Reads `claimCheckId`, `storageLocation`. Writes `claimCheckId`, `storageLocation`, `referenceSizeBytes`

**ProcessWorker** (task: `clc_process`)

Processes the retrieved payload and computes metric averages.

- Maps to primitives for aggregation, computes sums
- Reads `retrievedPayload`. Writes `processed`, `metricsAnalyzed`, `summary`

**RetrieveWorker** (task: `clc_retrieve`)

Retrieves the full payload from storage using the claim check reference.

- Reads `storageLocation`. Writes `payload`, `retrieved`

**StorePayloadWorker** (task: `clc_store_payload`)

Stores a large payload in external storage and returns a lightweight claim check reference.

- Reads `payload`, `storageType`. Writes `claimCheckId`, `storageLocation`, `payloadSizeBytes`

---

**32 tests** | Workflow: `clc_claim_check` | Timeout: 60s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
