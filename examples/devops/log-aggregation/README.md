# Log Aggregation in Java with Conductor : Collect, Parse, Enrich, Store

Aggregate logs: collect raw logs, parse them into structured format, enrich with metadata, and store in the log store. Pattern: collect -> parse -> enrich -> store. ## Raw Logs Are Useless Without Processing

A production system generates gigabytes of logs daily across dozens of services. Raw log lines like `2025-01-15T14:32:01Z INFO [payment-svc] Payment processed for order ORD-12345` are useless in raw form. they need to be collected from all sources (files, stdout, syslog), parsed into structured fields (timestamp, level, service, message, order ID), enriched with context (deployment version, environment, geo-IP of the request), and stored in a searchable system (Elasticsearch, Loki, CloudWatch).

Each step transforms the data: collection handles different log formats and transports. Parsing extracts structured fields using format-specific patterns (JSON, logfmt, regex). Enrichment adds context that isn't in the log line itself. Storage indexes the enriched data for search and analysis. If the storage step fails, parsed and enriched logs should be buffered. not lost.

## The Solution

**You write the parsing and enrichment logic. Conductor handles the collect-parse-enrich-store pipeline, retries on storage failures, and processing throughput tracking.**

`CollectLogsWorker` gathers log entries from the configured sources and time range. application log files, container stdout, syslog streams, and cloud provider log services. `ParseLogsWorker` transforms raw log lines into structured records, extracting timestamp, level, service name, message, and any embedded fields using format-appropriate parsers. `EnrichLogsWorker` adds context metadata, deployment version, environment name, geo-IP lookup for request IPs, and trace correlation IDs. `StoreLogsWorker` indexes the enriched records in the log store for search, alerting, and dashboarding. Conductor pipelines these four steps and records processing throughput and error rates.

### What You Write: Workers

Four workers process the log pipeline. Collecting raw logs, parsing into structured records, enriching with metadata, and storing in a searchable index.

| Worker | Task | What It Does |
|---|---|---|
| **CollectLogs** | `la_collect_logs` | Collects raw logs from the specified sources for the given time range. |
| **EnrichLogs** | `la_enrich_logs` | Enriches parsed logs with additional metadata fields. |
| **ParseLogs** | `la_parse_logs` | Parses raw logs into a structured format. |
| **StoreLogs** | `la_store_logs` | Stores enriched logs in the log store index. |

the workflow and rollback logic stay the same.

### The Workflow

```
la_collect_logs
 │
 ▼
la_parse_logs
 │
 ▼
la_enrich_logs
 │
 ▼
la_store_logs

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
