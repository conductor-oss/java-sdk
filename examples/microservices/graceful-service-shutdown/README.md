# Graceful Service Shutdown: Drain, Checkpoint, Deregister

Killing a service instance abruptly drops in-flight requests and loses progress. This
workflow stops accepting new tasks, drains 3 in-flight tasks to completion, checkpoints
pending state, and deregisters the instance from the service registry.

## Workflow

```
serviceName, instanceId
           |
           v
+---------------------+     +-------------------+     +-------------------+     +---------------------+
| gs_stop_accepting   | --> | gs_drain_tasks    | --> | gs_checkpoint     | --> | gs_deregister       |
+---------------------+     +-------------------+     +-------------------+     +---------------------+
  stopped: true               drained: true             saved: true              deregistered: true
  no longer accepting         pendingTasks: 0           pendingTasks count       removed from registry
  new tasks                   3 in-flight completed     persisted
```

## Workers

**StopAcceptingWorker** -- Marks `instanceId` as no longer accepting new tasks for
`serviceName`. Returns `stopped: true`.

**DrainTasksWorker** -- Waits for 3 in-flight tasks to complete. Returns `drained: true`,
`pendingTasks: 0`.

**CheckpointWorker** -- Saves state for `instanceId` with the current pending task count.
Returns `saved: true`.

**DeregisterWorker** -- Removes `instanceId` from the `serviceName` registry. Returns
`deregistered: true`.

## Tests

32 unit tests cover stop-accepting, task draining, checkpointing, and deregistration.

## Running

See [../../RUNNING.md](../../RUNNING.md) for setup and execution instructions.
