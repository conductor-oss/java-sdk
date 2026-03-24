# Aggregator Pattern

A price comparison service queries multiple suppliers for quotes on the same product. Responses arrive at different times and in different formats. The aggregator must collect all responses, normalize them to a common schema, combine them into a ranked list, and return the result only after all sources have responded or a timeout expires.

## Pipeline

```
[agp_collect]
     |
     v
[agp_check_complete]
     |
     v
[agp_aggregate]
     |
     v
[agp_forward]
```

**Workflow inputs:** `messages`, `expectedCount`, `aggregationKey`

## Workers

**AgpAggregateWorker** (task: `agp_aggregate`)

- Captures `instant.now()` timestamps
- Writes `aggregatedResult`

**AgpCheckCompleteWorker** (task: `agp_check_complete`)

- Reads `collectedCount`, `expectedCount`. Writes `isComplete`, `collectedCount`, `expectedCount`

**AgpCollectWorker** (task: `agp_collect`)

- Writes `collectedMessages`, `collectedCount`

**AgpForwardWorker** (task: `agp_forward`)

- Captures `instant.now()` timestamps
- Writes `forwarded`, `destination`, `forwardedAt`

---

**16 tests** | Workflow: `agp_aggregator_pattern` | Timeout: 60s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
