# Log Processing in Java Using Conductor : Ingestion, Structured Parsing, Pattern Extraction, and Metric Aggregation

## The Problem

Your services generate thousands of log entries per minute, each with a raw timestamp, severity level, service name, and message. Before you can answer questions like "which service is producing the most errors?" or "is there a recurring NullPointerException pattern?", you need to ingest the logs from the source for a specific time range, parse each raw entry into structured fields (normalizing `ts` to `timestamp`, `msg` to `message`, flagging ERROR-level entries), extract recurring patterns to identify the most frequent log signatures (repeated exceptions, timeout messages, connection errors), and aggregate metrics (total entries, error count, warning count, top patterns by frequency). Each step depends on the previous one: you can't extract patterns from unparsed logs, and you can't aggregate metrics without knowing which entries are errors.

Without orchestration, you'd write a single log analysis method that reads, parses, pattern-matches, and aggregates in one pass. If the pattern extraction logic changes, you'd re-run everything from ingestion. There's no record of how many entries were ingested vs: how many were successfully parsed, making it impossible to detect silent parsing failures. When a log source produces malformed entries, the entire analysis crashes with no retry, and you'd lose the structured entries you already parsed.

## The Solution

**You just write the log ingestion, structured parsing, pattern extraction, and metric aggregation workers. Conductor handles sequential log processing, retries when log source connections time out, and entry count tracking at every stage to detect silent parsing failures.**

Each stage of the log pipeline is a simple, independent worker. The ingester reads raw log entries from the specified source within the configured time range, applying any input filters. The parser normalizes each raw entry into a structured format: mapping `ts` to `timestamp`, `level` to severity, `msg` to `message`, and computing derived fields like `isError` for quick filtering, then counts errors and warnings. The pattern extractor scans parsed entries to identify recurring log signatures, ranking them by frequency to surface the top patterns. The metric aggregator combines parsed entries and extracted patterns into a summary: total entries, error and warning counts, and the most common patterns. Conductor executes them in sequence, passes the growing result set between stages, retries if ingestion fails due to a log source timeout, and tracks entry counts at every stage so you can see how many raw logs became structured entries became actionable patterns.

### What You Write: Workers

Four workers form the log analysis pipeline: ingesting raw entries from a source, parsing them into structured fields with severity flags, extracting recurring patterns to surface top log signatures, and aggregating error counts and per-service metrics.

| Worker | Task | What It Does |
|---|---|---|
| **AggregateMetricsWorker** | `lp_aggregate_metrics` | Aggregates metrics from parsed log entries. |
| **ExtractPatternsWorker** | `lp_extract_patterns` | Extracts recurring patterns from parsed log entries. |
| **IngestLogsWorker** | `lp_ingest_logs` | Ingests raw log entries from a specified source. |
| **ParseEntriesWorker** | `lp_parse_entries` | Parses raw log entries into structured format. |

Workers implement data processing stages with representative outputs so the pipeline runs end-to-end without external data stores. Swap in real data sources and sinks. the pipeline structure and error handling stay the same.

### The Workflow

```
lp_ingest_logs
 │
 ▼
lp_parse_entries
 │
 ▼
lp_extract_patterns
 │
 ▼
lp_aggregate_metrics

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
