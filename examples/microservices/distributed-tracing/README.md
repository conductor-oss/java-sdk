# Distributed Tracing in Java with Conductor

Distributed tracing with end-to-end request tracking.

## The Problem

Tracing a request across multiple microservices requires creating a trace context, propagating span IDs through each service call, recording timing for database operations, and exporting the complete trace to a backend like Jaeger or Zipkin. Each span must reference its parent to form a proper trace tree.

Without orchestration, each service must manually propagate trace headers, and if one service forgets, the trace is broken. There is no centralized way to ensure every service participates in tracing, and correlating spans across services requires manual effort.

## The Solution

**You just write the trace-creation, service-span, db-span, and trace-export workers. Conductor handles span sequencing, durable trace assembly, and automatic retry if the export step fails.**

Each worker represents a service boundary. Conductor manages cross-service orchestration, compensating transactions, timeout enforcement, and distributed tracing. your workers just make the service calls.

### What You Write: Workers

Four workers build a trace: CreateTraceWorker generates the root context, ServiceSpanWorker records a service call, DbSpanWorker captures database latency, and ExportTraceWorker sends the completed trace to a backend.

| Worker | Task | What It Does |
|---|---|---|
| **CreateTraceWorker** | `dt_create_trace` | Creates a new root trace with a unique traceId and root spanId for the operation. |
| **DbSpanWorker** | `dt_db_span` | Records a child span for a database operation, capturing query latency. |
| **ExportTraceWorker** | `dt_export_trace` | Exports the completed trace (all spans) to a tracing backend like Jaeger. |
| **ServiceSpanWorker** | `dt_service_span` | Records a child span for a service-to-service call, linking to the parent span. |

the workflow coordination stays the same.

### The Workflow

```
dt_create_trace
 │
 ▼
dt_service_span
 │
 ▼
dt_db_span
 │
 ▼
dt_export_trace

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
