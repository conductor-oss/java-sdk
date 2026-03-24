# Building a Distributed Trace with Service and Database Spans

A request flows through an API service and into a database, but without tracing you cannot
see where the 57ms total latency comes from. This workflow creates a trace, adds a service
span (45ms) and a database span (12ms) under the root, and exports all spans to Jaeger.

## Workflow

```
requestId, operation
         |
         v
+--------------------+     +--------------------+     +----------------+     +---------------------+
| dt_create_trace    | --> | dt_service_span    | --> | dt_db_span     | --> | dt_export_trace     |
+--------------------+     +--------------------+     +----------------+     +---------------------+
  traceId generated          span-svc-...               span-db-...           exported to Jaeger
  spanId: span-root-...      durationMs: 45             durationMs: 12        destination: "jaeger"
```

## Workers

**CreateTraceWorker** -- Creates a new trace for the `operation`. Returns a `traceId` and
root `spanId: "span-root-{timestamp}"`.

**ServiceSpanWorker** -- Adds a service span under the parent. Returns
`spanId: "span-svc-{timestamp}"`, `durationMs: 45`.

**DbSpanWorker** -- Adds a database span under the parent. Returns
`spanId: "span-db-{timestamp}"`, `durationMs: 12`.

**ExportTraceWorker** -- Exports all spans to Jaeger. Returns `exported: true`,
`destination: "jaeger"`.

## Tests

8 unit tests cover trace creation, span recording, and trace export.

## Running

See [../../RUNNING.md](../../RUNNING.md) for setup and execution instructions.
