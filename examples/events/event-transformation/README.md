# Event Transformation in Java Using Conductor

Event Transformation Pipeline. parse raw events, enrich with context, map to CloudEvents schema, and deliver to target. ## The Problem

You need to transform raw events from one format into another before delivering them downstream. The pipeline must parse raw events (JSON, XML, CSV), enrich them with contextual data (geo-IP lookup, user profile enrichment), map them to a target schema (CloudEvents, custom schema), and deliver the transformed events to the target system. Without a transformation pipeline, every downstream consumer must implement its own parsing and enrichment logic.

Without orchestration, you'd build a monolithic transformer that parses, enriches, maps, and delivers in a single method. manually handling parse failures for malformed input, retrying enrichment API calls, managing schema version differences, and logging every transformation for debugging.

## The Solution

**You just write the parse, enrich, schema-map, and delivery workers. Conductor handles sequential transformation stages, retry on enrichment API failures, and full before/after tracking for every event.**

Each transformation stage is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of executing the pipeline (parse, enrich, map, deliver), retrying if the enrichment API or target system is unavailable, tracking every event's transformation with full before/after details, and resuming from the last stage if the process crashes. ### What You Write: Workers

Four workers form the transformation pipeline: ParseEventWorker normalizes raw input, EnrichEventWorker adds contextual metadata, MapSchemaWorker converts to CloudEvents format, and OutputEventWorker delivers the transformed event to its destination.

| Worker | Task | What It Does |
|---|---|---|
| **EnrichEventWorker** | `et_enrich_event` | Enriches a parsed event with additional context: department, role, and location for the actor; project and environmen... |
| **MapSchemaWorker** | `et_map_schema` | Maps an enriched event to CloudEvents format (specversion 1.0). Produces: specversion, type, source, id, time, dataco... |
| **OutputEventWorker** | `et_output_event` | Delivers the mapped event to its target destination and returns delivery metadata: outputEventId, delivered flag, out... |
| **ParseEventWorker** | `et_parse_event` | Parses a raw event from a legacy format into a normalized structure. Extracts and renames fields: event_id->id, event... |

Workers implement event processing with realistic payloads so you can trace the full event flow without external message brokers. Replace the simulation with real event sources. the workflow and routing logic stay the same.

### The Workflow

```
et_parse_event
 │
 ▼
et_enrich_event
 │
 ▼
et_map_schema
 │
 ▼
et_output_event

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
