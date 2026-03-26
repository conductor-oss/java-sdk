# Alert Deduplication and Context-Enriched Routing

When a metric crosses a threshold, you get 5 identical alerts because every collector fires
independently. The on-call engineer gets paged 5 times for the same issue, with no context
about what was recently deployed. This workflow evaluates alert severity, deduplicates 5 raw
alerts down to 1 unique, enriches it with deployment context, and routes to Slack.

## Workflow

```
alertName, metric, value
          |
          v
+----------------+     +--------------------+     +---------------+     +--------------+
| ma_evaluate    | --> | ma_deduplicate     | --> | ma_enrich     | --> | ma_route     |
+----------------+     +--------------------+     +---------------+     +--------------+
  severity: "warning"   1 unique from 5 raw       deployment context    sent to on-call
                                                   added                 via Slack
```

## Workers

**EvaluateWorker** -- Takes `alertName`, `metric`, and `value`. Classifies the alert as
severity `"warning"`.

**DeduplicateWorker** -- Reduces 5 raw alerts to 1 unique alert. Returns `unique: true`.

**EnrichWorker** -- Adds deployment context to the alert. Returns
`context: {"deployment": true}`.

**RouteWorker** -- Sends the enriched alert to the on-call engineer via Slack. Returns
`notified: true`.

## Tests

2 unit tests cover the alerting pipeline.

## Running

See [../../RUNNING.md](../../RUNNING.md) for setup and execution instructions.
