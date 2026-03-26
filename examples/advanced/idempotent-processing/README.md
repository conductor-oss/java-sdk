# Idempotent Processing

A webhook endpoint occasionally receives the same event twice due to network retries. Processing it twice would create duplicate records. The idempotent processing pipeline needs to compute a deterministic idempotency key for each event, check if it has already been processed, skip duplicates, and return the cached result.

## Pipeline

```
[idp_check_processed]
     |
     v
     <SWITCH>
       |-- unprocessed -> [idp_process]
       |-- processed -> [idp_skip]
       +-- default -> [idp_process]
     |
     v
[idp_record]
```

**Workflow inputs:** `messageId`, `payload`

## Workers

**CheckProcessedWorker** (task: `idp_check_processed`)

Checks if a message has been previously processed by looking it up in the in-memory {@link DedupStore}.

- Reads `messageId`. Writes `processingState`, `previousResult`

**ProcessWorker** (task: `idp_process`)

Processes a message that has not been seen before. Outputs are deterministic — the resultHash is derived from the messageId so repeated invocations with the same input always produce the same output.

- Computes sha-256 hashes, formats output strings
- Reads `messageId`, `payload`. Writes `success`, `resultHash`

**RecordWorker** (task: `idp_record`)

Records a successfully processed message in the {@link DedupStore} so that future runs with the same messageId are detected as duplicates.

- Reads `messageId`, `resultHash`. Writes `recorded`

**SkipWorker** (task: `idp_skip`)

Skips processing for a message that was already handled. Returns the reason for skipping and the previous result hash so callers can verify the original processing outcome.

- Reads `messageId`, `previousResult`. Writes `skipped`, `reason`, `messageId`, `previousResult`

---

**15 tests** | Workflow: `idp_idempotent_processing` | Timeout: 60s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
