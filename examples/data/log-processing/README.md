# Log Processing

A platform engineering team collects application logs from 50 microservices. Raw log lines need parsing into structured records, enrichment with service metadata, filtering to remove health-check noise, and aggregation into per-service error rate metrics before the on-call dashboard can display them.

## Pipeline

```
[lp_ingest_logs]
     |
     v
[lp_parse_entries]
     |
     v
[lp_extract_patterns]
     |
     v
[lp_aggregate_metrics]
```

**Workflow inputs:** `logSource`, `timeRange`, `filters`

## Workers

**AggregateMetricsWorker** (task: `lp_aggregate_metrics`)

Aggregates metrics from parsed log entries.

- Formats output strings
- Reads `entries`. Writes `metrics`

**ExtractPatternsWorker** (task: `lp_extract_patterns`)

Extracts recurring patterns from parsed log entries.

- Sorts results, uses custom `comparator`
- Reads `entries`. Writes `patterns`, `patternCount`, `topPattern`

**IngestLogsWorker** (task: `lp_ingest_logs`)

Ingests raw log entries from a specified source.

- Writes `rawLogs`, `entryCount`

**ParseEntriesWorker** (task: `lp_parse_entries`)

Parses raw log entries into structured format.

- Filters with predicates
- Reads `rawLogs`. Writes `entries`, `parsedCount`, `errorCount`, `warnCount`

---

**17 tests** | Workflow: `log_processing` | Timeout: 60s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
