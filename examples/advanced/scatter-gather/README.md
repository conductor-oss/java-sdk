# Scatter Gather

A price comparison engine needs quotes from three supplier services. The scatter phase broadcasts the query to all three; the gather phase collects responses in parallel; the aggregator picks the best price. If one supplier is slow, the system should still return results from the responsive ones after a timeout.

## Pipeline

```
[sgr_broadcast]
     |
     v
     +──────────────────────────────────────────────────+
     | [sgr_gather_1] | [sgr_gather_2] | [sgr_gather_3] |
     +──────────────────────────────────────────────────+
     [join]
     |
     v
[sgr_aggregate]
```

**Workflow inputs:** `query`, `sources`

## Workers

**SgrAggregateWorker** (task: `sgr_aggregate`)

Aggregates responses from all gather workers and finds the best (lowest) price. Performs real comparison logic across all responses.

- Writes `aggregated`, `bestPrice`, `responseCount`

**SgrBroadcastWorker** (task: `sgr_broadcast`)

Broadcasts the query to all configured sources. Validates the query and sources, and prepares metadata for the gather workers.

- Lowercases strings, trims whitespace
- Reads `sources`. Writes `broadcasted`, `sourceCount`, `validatedQuery`

**SgrGather1Worker** (task: `sgr_gather_1`)

Gather worker 1: fetches data from the first source endpoint. Makes a real HTTP HEAD request to measure response time and generates a price based on the query hash for deterministic results.

- Writes `response`

**SgrGather2Worker** (task: `sgr_gather_2`)

Gather worker 2: fetches data from the second source endpoint.

- Writes `response`

**SgrGather3Worker** (task: `sgr_gather_3`)

Gather worker 3: fetches data from the third source endpoint.

- Writes `response`

---

**18 tests** | Workflow: `sgr_scatter_gather` | Timeout: 60s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
