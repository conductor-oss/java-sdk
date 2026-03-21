# Data Lineage in Java Using Conductor : Source Registration, Transformation Tracking, and Lineage Graph Construction

## The Problem

When a number looks wrong in a report, your first question is "where did this data come from and what happened to it along the way?" You need end-to-end lineage tracking: recording where data originated (which database, API, or file), documenting every transformation applied (name normalization, email lowercasing, field derivations), noting where the data landed (which table, data warehouse, or API), and building a graph that traces any record's journey from source to destination. Without lineage, debugging data quality issues is guesswork, compliance audits are painful, and impact analysis for schema changes is impossible.

Without orchestration, lineage tracking is an afterthought bolted onto transformation scripts, a log line here, a metadata table update there. Transformations happen inline with no structured record of what changed. If a transformation step fails and gets manually restarted, the lineage becomes inconsistent. There's no unified graph showing the full journey, and adding a new transformation step means remembering to update the lineage tracking in a completely separate place.

## The Solution

**You just write the source registration, transformation, destination recording, and lineage graph workers. Conductor handles sequential execution with built-in observability at both the orchestration and application level, giving you lineage tracking alongside retries and crash recovery.**

Each stage of the pipeline is a simple, independent worker that both transforms data and appends to the lineage chain. The source registrar records the origin system and initializes the lineage metadata. Each transformation worker applies its logic (uppercase names, lowercase emails) and appends a lineage entry documenting what it changed. The destination recorder notes where the final data lands. The graph builder assembles all lineage entries into a structured graph showing the full source-to-destination journey with every transformation step. Conductor executes them in sequence, passes the growing lineage chain between steps, and provides built-in observability for every transformation's inputs and outputs. Giving you lineage tracking both at the application level and the orchestration level. ### What You Write: Workers

Five workers track lineage across the data pipeline: registering the source origin, applying sequential transformations that each append lineage metadata, recording the destination, and building a source-to-destination lineage graph.

| Worker | Task | What It Does |
|---|---|---|
| **ApplyTransform1Worker** | `ln_apply_transform_1` | Applies transform 1: uppercase names and tracks lineage. |
| **ApplyTransform2Worker** | `ln_apply_transform_2` | Applies transform 2: lowercase emails and tracks lineage. |
| **BuildLineageGraphWorker** | `ln_build_lineage_graph` | Builds a lineage graph summary from collected lineage entries. |
| **RecordDestinationWorker** | `ln_record_destination` | Records the destination in the lineage chain. |
| **RegisterSourceWorker** | `ln_register_source` | Registers the data source and initializes lineage tracking. |

Workers implement data processing stages with representative outputs so the pipeline runs end-to-end without external data stores. Swap in real data sources and sinks. the pipeline structure and error handling stay the same.

### The Workflow

```
ln_register_source
 │
 ▼
ln_apply_transform_1
 │
 ▼
ln_apply_transform_2
 │
 ▼
ln_record_destination
 │
 ▼
ln_build_lineage_graph

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
