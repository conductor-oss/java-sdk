# Distributed Lock for Safe Concurrent Resource Updates

Two services trying to update the same resource simultaneously can corrupt it. Without a
lock, both read the current value, apply their change, and the second write silently
overwrites the first. This workflow acquires a distributed lock with a TTL, executes the
critical operation within the lock, and releases it afterward -- ensuring only one writer
accesses the resource at a time.

## Workflow

```
resourceId, operation, ttlSeconds
              |
              v
+---------------------+     +------------------------+     +---------------------+
| dl_acquire_lock     | --> | dl_execute_critical    | --> | dl_release_lock     |
+---------------------+     +------------------------+     +---------------------+
  lockToken: LOCK-...         result: "updated"              released: true
  acquired: true              version: 5
```

## Workers

**AcquireLockWorker** -- Acquires a lock on `resourceId` with `ttlSeconds`. Returns
`lockToken: "LOCK-{timestamp}"`, `acquired: true`.

**ExecuteCriticalWorker** -- Executes the `operation` on `resourceId` within the lock.
Returns `result: "updated"`, `version: 5`.

**ReleaseLockWorker** -- Releases the lock using `lockToken`, freeing the resource for other
callers. Returns `released: true`.

## Tests

6 unit tests cover lock acquisition, critical section execution, and lock release.
The lock token is timestamp-based for uniqueness across concurrent requests.

## Running

See [../../RUNNING.md](../../RUNNING.md) for setup and execution instructions.
