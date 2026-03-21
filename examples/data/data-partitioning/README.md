# Data Partitioning in Java Using Conductor : Split, Parallel Process, and Merge

## The Problem

You have a large dataset that's too slow to process sequentially, but you can split it into independent partitions and process them in parallel. That means dividing records based on a partition key (region, category, hash), running the same processing logic against each partition simultaneously, and combining the results back into a single output. If one partition fails (bad data, timeout), the other partition's work shouldn't be lost.

Without orchestration, you'd manage `ExecutorService` thread pools manually, submit partition processing as `Callable` tasks, handle `Future` results, and write your own merge logic. If the process crashes after partition A finishes but before partition B completes, you'd re-process both. There's no visibility into which partition is running, how many records each processed, or where a failure occurred.

## The Solution

**You just write the data splitting, partition processing, and result merging workers. Conductor handles parallel partition processing via FORK_JOIN, independent per-partition retries, and crash recovery that resumes only the incomplete partition.**

Each concern is a simple, independent worker. The splitter divides the dataset into two partitions. The partition workers each process their half of the data independently. The merger combines results from both partitions into a unified output with counts from each. Conductor's `FORK_JOIN` runs both partitions simultaneously, waits for both to complete, and then triggers the merge. If one partition fails, Conductor retries just that partition. If the process crashes after one partition finishes, Conductor resumes only the incomplete one. You get all of that, without writing a single line of thread pool or synchronization code.

### What You Write: Workers

Four workers implement the split-process-merge pattern: dividing the dataset into two partitions by a configurable key, processing both partitions simultaneously via FORK_JOIN, and merging results into a unified output.

| Worker | Task | What It Does |
|---|---|---|
| **MergeResultsWorker** | `par_merge_results` | Merges results from both partitions into a single combined output. |
| **ProcessPartitionAWorker** | `par_process_partition_a` | Processes partition A by adding processed:true and partition:"A" to each record. |
| **ProcessPartitionBWorker** | `par_process_partition_b` | Processes partition B by adding processed:true and partition:"B" to each record. |
| **SplitDataWorker** | `par_split_data` | Split Data. Computes and returns partition a, partition b, total count |

Workers implement data processing stages with representative outputs so the pipeline runs end-to-end without external data stores. Swap in real data sources and sinks. the pipeline structure and error handling stay the same.

### The Workflow

```
par_split_data
 │
 ▼
FORK_JOIN
 ├── par_process_partition_a
 └── par_process_partition_b
 │
 ▼
JOIN (wait for all branches)
par_merge_results

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
