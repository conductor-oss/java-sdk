# Map Reduce

A text analytics pipeline needs to count word frequencies across a large corpus of documents. The map phase tokenizes each document and emits word counts; the reduce phase merges counts across all documents. The final output is a sorted frequency table.

## Pipeline

```
[mpr_split_input]
     |
     v
     +─────────────────────────────────────────+
     | [mpr_map_1] | [mpr_map_2] | [mpr_map_3] |
     +─────────────────────────────────────────+
     [join]
     |
     v
[mpr_reduce]
     |
     v
[mpr_output]
```

**Workflow inputs:** `documents`, `searchTerm`

## Workers

**MprMap1Worker** (task: `mpr_map_1`)

Map worker 1: counts occurrences of the search term in each document of its partition. Produces a list of {docId, count} pairs.

- Reads `partition`. Writes `mapped`

**MprMap2Worker** (task: `mpr_map_2`)

Map worker 2: counts occurrences of the search term in each document of its partition.

- Reads `partition`. Writes `mapped`

**MprMap3Worker** (task: `mpr_map_3`)

Map worker 3: counts occurrences of the search term in each document of its partition.

- Reads `partition`. Writes `mapped`

**MprOutputWorker** (task: `mpr_output`)

Output worker: formats the final map-reduce summary from the reduced result.

- Reads `reducedResult`. Writes `summary`

**MprReduceWorker** (task: `mpr_reduce`)

Reduce worker: aggregates mapped results from all 3 map workers. Sums up all counts and produces a combined result list.

- Filters with predicates
- Writes `reduced`, `totalOccurrences`, `totalDocuments`

**MprSplitInputWorker** (task: `mpr_split_input`)

Splits input documents (list of text strings) into 3 partitions for parallel map processing. Uses round-robin distribution to balance the load.

- Reads `documents`. Writes `partitions`, `totalDocuments`

---

**23 tests** | Workflow: `mpr_map_reduce` | Timeout: 60s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
