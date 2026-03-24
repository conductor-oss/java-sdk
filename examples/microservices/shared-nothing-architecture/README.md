# Shared-Nothing Pipeline: Three Independent Services in Sequence

In a shared-nothing architecture, each service owns its state and communicates only through
explicit message passing -- no shared database, no shared memory. This workflow chains three
services where each receives only the output of the previous service, then aggregates all
results.

## Workflow

```
requestId, data
       |
       v
+------------------+     +------------------+     +------------------+     +------------------+
| sn_service_a     | --> | sn_service_b     | --> | sn_service_c     | --> | sn_aggregate     |
+------------------+     +------------------+     +------------------+     +------------------+
  result: "a-processed"   result: "b-processed"   result: "c-processed"   aggregated from
  instanceId: "a-1"       instanceId: "b-1"       instanceId: "c-1"       all three
  (no shared state)       (uses a's output only)  (uses b's output only)
```

## Workers

**ServiceAWorker** -- Processes independently with no shared state. Returns
`result: "a-processed"`, `instanceId: "a-1"`.

**ServiceBWorker** -- Processes using only service A's output. Returns
`result: "b-processed"`, `instanceId: "b-1"`.

**ServiceCWorker** -- Processes using only service B's output. Returns
`result: "c-processed"`, `instanceId: "c-1"`.

**AggregateWorker** -- Combines all results into a final `aggregated` response.

## Tests

8 unit tests cover each independent service and the aggregation.

## Running

See [../../RUNNING.md](../../RUNNING.md) for setup and execution instructions.
