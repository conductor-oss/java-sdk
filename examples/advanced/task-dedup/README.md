# Task Dedup

A workflow engine occasionally schedules the same task twice due to race conditions during recovery. Processing it twice wastes resources and may cause side effects. The dedup layer computes a task fingerprint, checks for recent duplicates, and skips redundant executions.

## Pipeline

```
[tdd_hash_input]
     |
     v
[tdd_check_seen]
     |
     v
     <SWITCH>
       |-- new -> [tdd_execute_new]
       |-- dup -> [tdd_return_cached]
       +-- default -> [tdd_execute_new]
```

**Workflow inputs:** `payload`, `cacheEnabled`

## Workers

**CheckSeenWorker** (task: `tdd_check_seen`)

Checks if a hash has been seen before to determine duplicate status.

- Reads `hash`. Writes `status`, `cachedResult`

**ExecuteNewWorker** (task: `tdd_execute_new`)

Processes a new (non-duplicate) task and caches the result for future dedup.

- Sets `result` = `"processed_successfully"`
- Reads `hash`. Writes `result`, `cachedForFuture`, `hash`

**HashInputWorker** (task: `tdd_hash_input`)

Hashes the input payload to produce a deterministic deduplication key.

- Uses `math.abs()`, formats output strings
- Reads `payload`. Writes `hash`

**ReturnCachedWorker** (task: `tdd_return_cached`)

Returns a previously cached result for a duplicate task.

- Reads `hash`, `cachedResult`. Writes `result`, `fromCache`

---

**32 tests** | Workflow: `tdd_task_dedup` | Timeout: 60s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
