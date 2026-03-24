# Edge Computing Orchestration in Java Using Conductor : Dispatch, Process, Collect, Merge

## Processing Data at the Edge

Edge computing pushes computation close to the data source. IoT sensors on a factory floor, cameras at retail locations, or CDN nodes across geographies. A central system needs to dispatch an inference job to edge nodes, wait for each node to process its local data (video frames, sensor readings, log files), collect the partial results, and merge them into a global view. If one node is slow or fails, the central system needs to know, not silently drop that node's results.

Coordinating a fleet of edge nodes means tracking which nodes received the job, which have reported back, handling nodes that go offline mid-processing, and merging heterogeneous partial results into a coherent aggregate. Building this with SSH scripts or ad-hoc HTTP polling becomes untenable past a handful of nodes.

## The Solution

**You write the dispatch and edge processing logic. Conductor handles node coordination, retries, and result merging.**

`EorDispatchWorker` takes the job ID and list of edge nodes, assigns work to each node, and returns the task assignments. `EorEdgeProcessWorker` simulates the edge-side processing. running the job on each node's local data and producing per-node results. `EorCollectWorker` gathers results from all nodes and counts how many reported back. `EorMergeWorker` combines the collected partial results into a single merged output. Conductor sequences these steps, retries dispatch or collection if a network call to an edge node fails, and records the full execution, which nodes were assigned, which responded, and what the merged result looks like.

### What You Write: Workers

Four workers coordinate edge-fleet processing: job dispatch to nodes, per-node execution, result collection, and multi-node result merging, each independent of the edge topology.

### The Workflow

```
eor_dispatch
 │
 ▼
eor_edge_process
 │
 ▼
eor_collect
 │
 ▼
eor_merge

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
