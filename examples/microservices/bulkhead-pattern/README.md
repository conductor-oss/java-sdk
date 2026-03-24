# Bulkhead Pattern: Isolating Requests by Service Priority

When all requests share one thread pool, a slow premium client's batch job can starve
real-time API calls. The bulkhead pattern isolates requests into separate concurrency pools.
This workflow classifies incoming requests by service name and priority, allocates a pool
slot, executes the request in isolation, and releases the slot when done.

## Workflow

```
serviceName, priority, request
              |
              v
+------------------------+     +----------------------+     +------------------------+     +----------------------+
| bh_classify_request    | --> | bh_allocate_pool     | --> | bh_execute_request     | --> | bh_release_pool      |
+------------------------+     +----------------------+     +------------------------+     +----------------------+
  pool assigned based on        slot allocated with          request processed              slot released
  serviceName + priority        maxConcurrency limit         in isolated pool               poolId returned
```

## Workers

**ClassifyRequestWorker** -- Takes `serviceName` and `priority`. Assigns a `pool` name and
`maxConcurrency` limit based on the priority level.

**AllocatePoolWorker** -- Allocates a slot in the assigned pool. Returns
`poolId: "POOL-{hashCode}"`, `allocated: true`.

**ExecuteRequestWorker** -- Processes the request within the pool. Returns
`response: {status: "ok"}`.

**ReleasePoolWorker** -- Releases the pool slot. Returns `released: true`.

## Tests

4 unit tests cover request classification, pool allocation, execution, and release.

## Running

See [../../RUNNING.md](../../RUNNING.md) for setup and execution instructions.
