# Event Versioning in Java Using Conductor

Event versioning workflow that detects event schema version, transforms older versions to the latest format via a SWITCH task, and processes the event uniformly.

## The Problem

You need to handle events with different schema versions in the same processing pipeline. As your event schema evolves (adding fields, changing types, renaming properties), older producers may still emit events in v1 format while newer ones emit v2. The workflow must detect the event's schema version, transform older versions to the latest format, and then process all events uniformly regardless of their original version. Without version handling, schema changes break consumers.

Without orchestration, you'd embed version detection and transformation logic in every consumer, maintain transformation functions for every version pair, handle unknown versions with fallback logic, and risk consumers silently processing events in the wrong format when a version check is missed.

## The Solution

**You just write the version-detection, schema-transform, and event-processing workers. Conductor handles version-based SWITCH routing, per-version transformation retries, and full version lineage tracking for every event.**

Each versioning concern is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of detecting the event version, routing via a SWITCH task to the appropriate version transformer, processing the event in its canonical format, and tracking every event's version and transformation.

### What You Write: Workers

Five workers handle schema evolution: DetectVersionWorker identifies the event's schema version, TransformV1Worker and TransformV2Worker upgrade older formats to v3, PassThroughWorker skips transformation for current-version events, and ProcessEventWorker handles the canonical format.

| Worker | Task | What It Does |
|---|---|---|
| **DetectVersionWorker** | `vr_detect_version` | Detects the schema version of an incoming event. |
| **PassThroughWorker** | `vr_pass_through` | Passes through an event that is already at the latest version. |
| **ProcessEventWorker** | `vr_process_event` | Processes a versioned event after transformation. |
| **TransformV1Worker** | `vr_transform_v1` | Transforms a v1 event to the latest (v3) schema format. |
| **TransformV2Worker** | `vr_transform_v2` | Transforms a v2 event to the latest (v3) schema format. |

Workers implement event processing with realistic payloads so you can trace the full event flow without external message brokers. Replace the simulation with real event sources. the workflow and routing logic stay the same.

### The Workflow

```
vr_detect_version
 │
 ▼
SWITCH (switch_ref)
 ├── v1: vr_transform_v1
 ├── v2: vr_transform_v2
 └── default: vr_pass_through
 │
 ▼
vr_process_event

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
