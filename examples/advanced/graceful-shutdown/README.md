# Graceful Worker Shutdown in Java Using Conductor : Signal, Drain, Complete, Checkpoint, Stop

## Killing Workers Loses Work

Sending `kill -9` to a worker process terminates it instantly. any in-flight tasks get orphaned, partial results are lost, and the next worker restart has no idea where the previous instance left off. Messages get redelivered, duplicates appear, and the ops team spends an hour figuring out which tasks need to be manually retried.

Graceful shutdown means stopping new task pickup first (signal), giving the queue time to drain (with a configurable timeout so it doesn't hang forever), waiting for in-flight tasks to finish their current execution, checkpointing the state so the next instance knows exactly where to resume, and only then stopping the process. Getting this five-step sequence right. especially under time pressure during a deploy, requires careful orchestration.

## The Solution

**You write the drain and checkpoint logic. Conductor handles the shutdown sequencing, retries, and state verification.**

`GshSignalWorker` notifies the worker group to stop polling for new tasks. `GshDrainTasksWorker` waits up to the configured `drainTimeoutSec` for the task queue to empty, tracking how many tasks were drained. `GshCompleteInflightWorker` ensures every in-flight task finishes and reports how many were completed. `GshCheckpointWorker` saves the current state. checkpoint ID, completed task list, so the next startup can resume cleanly. `GshStopWorker` performs the final process termination. Conductor sequences these steps strictly, records the drain count, in-flight completion count, and checkpoint ID, and ensures nothing is lost during the shutdown.

### What You Write: Workers

Five workers enforce the shutdown protocol: signal broadcast, queue draining, in-flight completion, state checkpointing, and final stop, ensuring zero work is lost during deploys.### The Workflow

```
gsh_signal
 │
 ▼
gsh_drain_tasks
 │
 ▼
gsh_complete_inflight
 │
 ▼
gsh_checkpoint
 │
 ▼
gsh_stop

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
