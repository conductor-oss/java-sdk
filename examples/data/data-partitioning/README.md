# Data Partitioning

A time-series database receives millions of sensor readings per day. Query performance degrades unless the data is partitioned by time range and sensor group. The partitioning pipeline needs to analyze the data distribution, assign records to partitions, write each partition, and verify that no records were lost during the split.

## Pipeline

```
[par_split_data]
     |
     v
     +───────────────────────────────────────────────────────+
     | [par_process_partition_a] | [par_process_partition_b] |
     +───────────────────────────────────────────────────────+
     [join]
     |
     v
[par_merge_results]
```

**Workflow inputs:** `records`, `partitionKey`

## Workers

**MergeResultsWorker** (task: `par_merge_results`)

Merges results from both partitions into a single combined output.

- Reads `resultA`, `resultB`, `countA`, `countB`. Writes `mergedCount`, `summary`, `records`

**ProcessPartitionAWorker** (task: `par_process_partition_a`)

Processes partition A by adding processed:true and partition:"A" to each record.

- Reads `partition`. Writes `result`, `processedCount`

**ProcessPartitionBWorker** (task: `par_process_partition_b`)

Processes partition B by adding processed:true and partition:"B" to each record.

- Reads `partition`. Writes `result`, `processedCount`

**SplitDataWorker** (task: `par_split_data`)

Splits a list of records into two partitions (A and B). First ceil(n/2) records go to partition A, the rest to partition B.

- Applies `math.ceil()`
- Reads `records`, `partitionKey`. Writes `partitionA`, `partitionB`, `totalCount`

---

**36 tests** | Workflow: `data_partitioning_wf` | Timeout: 120s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
