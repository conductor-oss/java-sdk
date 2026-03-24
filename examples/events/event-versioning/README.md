# Event Versioning

A platform evolves its event schema over time, but consumers at different versions coexist. Version 1 events have a flat structure; version 2 adds nested metadata. The versioning pipeline needs to detect the event version, apply version-specific transformations to normalize to the latest schema, and ensure backward compatibility.

## Pipeline

```
[vr_detect_version]
     |
     v
     <SWITCH>
       |-- v1 -> [vr_transform_v1]
       |-- v2 -> [vr_transform_v2]
       +-- default -> [vr_pass_through]
     |
     v
[vr_process_event]
```

**Workflow inputs:** `event`

## Workers

**DetectVersionWorker** (task: `vr_detect_version`)

Detects the schema version of an incoming event.

- Reads `event`. Writes `version`

**PassThroughWorker** (task: `vr_pass_through`)

Passes through an event that is already at the latest version.

- Reads `event`. Writes `transformed`

**ProcessEventWorker** (task: `vr_process_event`)

Processes a versioned event after transformation.

- Reads `originalVersion`. Writes `processed`, `originalVersion`

**TransformV1Worker** (task: `vr_transform_v1`)

Transforms a v1 event to the latest (v3) schema format.

- Reads `event`. Writes `transformed`

**TransformV2Worker** (task: `vr_transform_v2`)

Transforms a v2 event to the latest (v3) schema format.

- Reads `event`. Writes `transformed`

---

**41 tests** | Workflow: `event_versioning` | Timeout: 60s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
