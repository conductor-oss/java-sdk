# Collecting 15,000 Logs, Parsing to JSON, and Storing in Elasticsearch

Your services emit logs in mixed formats -- some JSON, some plaintext, some syslog. Without
a pipeline, searching across them means grepping through raw files on each box. This workflow
collects 15,000 raw logs from multiple sources, parses them into structured JSON (with 200
parse errors for malformed entries), enriches them with geo/service/traceId/userId fields,
and stores 14,800 documents in Elasticsearch.

## Workflow

```
sources, timeRange, logLevel
           |
           v
+-------------------+     +------------------+     +------------------+     +------------------+
| la_collect_logs   | --> | la_parse_logs    | --> | la_enrich_logs   | --> | la_store_logs    |
+-------------------+     +------------------+     +------------------+     +------------------+
  15000 raw logs           14800 parsed             14800 enriched          logs-2026.03.08
  format: "mixed"          200 parse errors          45MB, 4 fields added   14800 docs written
                           avg 0.2ms/log             (geo,service,          3200ms storage
                                                      traceId,userId)
```

## Workers

**CollectLogs** -- Takes `sources` (list), `timeRange`, and `logLevel`. Returns
`rawLogCount: 15000`, `format: "mixed"`, `collectedAt: "2026-03-08T06:00:00Z"`.

**ParseLogs** -- Parses raw logs into structured JSON. Returns `parsedCount: 14800`,
`parseErrors: 200`, `structuredLogs: "json"`, `avgParseTimeMs: 0.2`.

**EnrichLogs** -- Adds 4 metadata fields: `geo`, `service`, `traceId`, `userId`. Returns
`enrichedCount: 14800`, `sizeBytes: 45000000`.

**StoreLogs** -- Writes enriched logs to Elasticsearch index `"logs-2026.03.08"`. Returns
`documentsWritten: 14800`, `storageMs: 3200`.

## Tests

16 unit tests cover log collection and parsing.

## Running

See [../../RUNNING.md](../../RUNNING.md) for setup and execution instructions.
