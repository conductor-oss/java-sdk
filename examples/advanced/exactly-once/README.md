# Exactly Once

A financial transaction system must process each transfer exactly once. Duplicate processing means double-charging; missed processing means lost revenue. The pipeline needs idempotency keys, deduplication checks, transactional processing, and confirmation that exactly one output was produced per input.

## Pipeline

```
[exo_lock]
     |
     v
[exo_check_state]
     |
     v
[exo_process]
     |
     v
[exo_commit]
     |
     v
[exo_unlock]
```

**Workflow inputs:** `messageId`, `payload`, `resourceKey`

## Workers

**ExoCheckStateWorker** (task: `exo_check_state`)

Checks whether a message has already been processed using an in-memory ConcurrentHashMap. Returns the current processing state and sequence number.

- Reads `messageId`. Writes `currentState`, `alreadyProcessed`, `sequenceNumber`

**ExoCommitWorker** (task: `exo_commit`)

Commits the processing result by updating the state store to "completed". Uses the lock token to verify ownership before committing.

- Captures `instant.now()` timestamps
- Reads `messageId`, `lockToken`. Writes `committed`, `commitTimestamp`, `stateTransition`

**ExoLockWorker** (task: `exo_lock`)

Acquires a distributed lock for a resource key using ConcurrentHashMap. If the lock is already held, the task fails to enforce mutual exclusion.

- Reads `resourceKey`, `ttlSeconds`. Writes `lockToken`, `acquired`, `ttlSeconds`

**ExoProcessWorker** (task: `exo_process`)

Processes a message if not already processed. The result is deterministic based on the messageId (SHA-256 hash) so re-processing yields the same output. If currentState is "completed", skips processing and returns alreadyProcessed=true.

- Truncates strings to first 100 character(s), computes sha-256 hashes, formats output strings
- Reads `messageId`, `currentState`, `payload`. Writes `processed`, `skipped`, `result`

**ExoUnlockWorker** (task: `exo_unlock`)

Releases the lock held on a resource key. Verifies that the lock token matches the one currently held before releasing.

- Reads `resourceKey`, `lockToken`. Writes `unlocked`, `previousHolder`

---

**28 tests** | Workflow: `exo_exactly_once` | Timeout: 60s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
