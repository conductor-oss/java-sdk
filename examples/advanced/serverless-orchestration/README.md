# Serverless Orchestration

A serverless application chains multiple Lambda-style functions: validate input, process data, store results, and send notifications. Each function is stateless and ephemeral. The orchestration layer manages the execution sequence, passes state between functions, and handles cold-start delays.

## Pipeline

```
[svl_invoke_parse]
     |
     v
[svl_invoke_enrich]
     |
     v
[svl_invoke_score]
     |
     v
[svl_aggregate]
```

**Workflow inputs:** `eventId`, `payload`

## Workers

**SvlAggregateWorker** (task: `svl_aggregate`)

Aggregates results from the serverless function chain.

- Reads `eventId`, `score`. Writes `aggregated`, `stored`

**SvlInvokeEnrichWorker** (task: `svl_invoke_enrich`)

Invokes the enrich serverless function to add user context.

- Writes `enriched`, `billedMs`

**SvlInvokeParseWorker** (task: `svl_invoke_parse`)

Invokes the parse serverless function for an incoming event.

- Reads `eventId`. Writes `parsed`, `billedMs`

**SvlInvokeScoreWorker** (task: `svl_invoke_score`)

Invokes the score serverless function to compute engagement score.

- Writes `score`, `confidence`, `billedMs`

---

**32 tests** | Workflow: `serverless_orchestration_demo` | Timeout: 60s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
