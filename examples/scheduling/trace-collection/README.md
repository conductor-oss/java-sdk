# Distributed Trace Collection in Java Using Conductor : Instrument, Collect Spans, Assemble Traces, and Store

## The Problem

Your microservices produce distributed traces. spans from each service that must be collected, assembled into complete request traces, and stored for debugging and performance analysis. Instrumentation must be configured, spans collected from each service's collector, assembled by trace ID into a full picture of the request path, and stored in a durable trace backend.

Without orchestration, trace collection relies entirely on sidecar agents and backend infrastructure (Jaeger, Zipkin). When instrumentation fails for one service, traces are incomplete. When the backend is overloaded, spans are dropped. There's no pipeline to verify instrumentation, validate span completeness, or manage trace sampling rates.

## The Solution

**You just write the span collection and trace assembly logic. Conductor handles the instrument-collect-assemble-store pipeline, retries when span collectors or trace backends are temporarily unavailable, and a complete record of every collection run with span counts and trace completeness.**

Each trace collection step is an independent worker. instrumentation, span collection, trace assembly, and storage. Conductor runs them in sequence: configure instrumentation, collect spans, assemble into traces, then store. Every collection run is tracked with span counts, trace completeness, and storage confirmation. ### What You Write: Workers

InstrumentWorker configures tracing on target services, CollectSpansWorker gathers individual spans, AssembleTraceWorker stitches them into complete request traces, and StoreTraceWorker persists the assembled traces to Jaeger or your preferred backend.

| Worker | Task | What It Does |
|---|---|---|
| **AssembleTraceWorker** | `trc_assemble_trace` | Assembles collected spans into a complete trace, computing trace depth and total request duration |
| **CollectSpansWorker** | `trc_collect_spans` | Collects individual spans from instrumented services and returns the total span count |
| **InstrumentWorker** | `trc_instrument` | Configures tracing instrumentation on target services with the specified sampling rate, returning the count of instrumented services |
| **StoreTraceWorker** | `trc_store_trace` | Persists assembled traces to the trace backend (e.g., Jaeger) for querying and analysis |

the schedule triggers, retry behavior, and monitoring stay the same.

### The Workflow

```
trc_instrument
 │
 ▼
trc_collect_spans
 │
 ▼
trc_assemble_trace
 │
 ▼
trc_store_trace

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
