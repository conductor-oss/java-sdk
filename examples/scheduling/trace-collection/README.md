# Trace Collection

A distributed service needs tracing. The pipeline instruments the service, collects spans at the configured sampling rate, assembles the spans into a complete trace, and stores it.

## Workflow

```
trc_instrument ──> trc_collect_spans ──> trc_assemble_trace ──> trc_store_trace
```

Workflow `trace_collection_416` accepts `serviceName`, `traceId`, and `samplingRate`. Times out after `60` seconds.

## Workers

**InstrumentWorker** (`trc_instrument`) -- instruments the service for trace collection.

**CollectSpansWorker** (`trc_collect_spans`) -- collects spans from the instrumented service.

**AssembleTraceWorker** (`trc_assemble_trace`) -- assembles collected spans into a complete trace.

**StoreTraceWorker** (`trc_store_trace`) -- stores the assembled trace.

## Workflow Output

The workflow produces `instrumentedServices`, `spanCount`, `traceDepth`, `stored` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Project Structure

This example contains 4 worker implementations in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `trace_collection_416` defines 4 tasks with input parameters `serviceName`, `traceId`, `samplingRate` and a timeout of `60` seconds.

## Workflow Definition Details

Schema version `2`, workflow version `1`. Owner: `examples@orkes.io`.

## Tests

2 tests verify instrumentation, span collection, trace assembly, and storage.

## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
