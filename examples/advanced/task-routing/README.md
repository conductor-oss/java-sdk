# Task Routing in Java Using Conductor : Analyze Requirements, Select Worker Pool, Dispatch, Verify

## Tasks Have Different Resource Needs : Send Them to the Right Pool

An ML inference task needs a GPU-equipped worker in us-east-1. A data transformation task needs a high-memory worker in any region. A simple validation task can run on any small worker anywhere. Sending all tasks to the same pool wastes GPU resources on validation and starves inference tasks when the general pool is full.

Intelligent task routing means analyzing each task's requirements (GPU type, memory needs, region affinity), matching those requirements against available worker pools, dispatching to the best-fit pool, and verifying that the task ran successfully on the selected infrastructure.

## The Solution

**You write the requirements analysis and pool selection logic. Conductor handles dispatch sequencing, retries, and routing audit trails.**

`TrtAnalyzeRequirementsWorker` examines the task type, resource needs, and region constraints to build a requirements profile. `TrtSelectPoolWorker` matches the requirements against available worker pools and selects the best fit. considering capacity, cost, and locality. `TrtDispatchWorker` sends the task to the selected pool. `TrtVerifyWorker` confirms the task executed successfully on the target infrastructure. Conductor sequences these steps and records the routing decision, which pool was selected, why, and whether the task succeeded there.

### What You Write: Workers

Four workers manage intelligent dispatch: requirements analysis, pool selection based on capacity and locality, task dispatch, and execution verification, each decoupled from the infrastructure it targets.### The Workflow

```
trt_analyze_requirements
 │
 ▼
trt_select_pool
 │
 ▼
trt_dispatch
 │
 ▼
trt_verify

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
