# Task Domains in Java with Conductor

Task Domains demo. route tasks to specific worker groups using domains. ## The Problem

You need to route tasks to specific worker groups, for example, sending GPU-intensive work to GPU-equipped workers or routing region-specific tasks to workers in that region. Task domains let you tag workers with domain labels so only workers in the matching domain pick up the task, without changing the workflow definition.

Without task domains, you'd need separate task types for each worker group (e.g., `process_gpu`, `process_cpu`), duplicating workflow definitions. Task domains decouple routing from task identity, the same `td_process` task can go to different worker pools based on the domain configuration at runtime.

## The Solution

**You just write the processing worker and register it with domain labels. Conductor handles routing tasks to the correct worker pool based on runtime domain configuration.**

This example runs two instances of the same `td_process` worker, each registered with a different domain label (e.g., "gpu" and "cpu"). The TdProcessWorker tags its output with the worker group name so you can verify which domain handled the task. The example code starts the workflow with a `taskToDomain` mapping that routes `td_process` to a specific domain at runtime, the same workflow definition, the same task name, but different worker pools handling it depending on the domain configuration passed at execution time. No workflow JSON changes are needed to shift work between groups.

### What You Write: Workers

A single worker demonstrates domain-based routing: TdProcessWorker tags its output with the worker group name so you can verify which domain pool (GPU, CPU, region) handled the task, while the workflow definition stays identical across all domain configurations.

| Worker | Task | What It Does |
|---|---|---|
| **TdProcessWorker** | `td_process` | Processes data and tags output with the worker group name. When used with task domains, this worker can be routed to ... |

Workers implement their processing steps so you can see the pattern in action without external services. Replace the simulation with real processing logic. the task pattern and Conductor orchestration remain unchanged.

### The Workflow

```
td_process

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
