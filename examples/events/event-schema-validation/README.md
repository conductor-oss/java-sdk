# Event Schema Validation

A partner integration API accepts events from external systems, but partners frequently send payloads that deviate from the agreed schema: missing required fields, wrong types, extra unknown fields. Each event needs validation against the schema definition, with detailed error reports sent back to the partner.

## Pipeline

```
[sv_validate_schema]
     |
     v
     <SWITCH>
       |-- valid -> [sv_process_valid]
       |-- invalid -> [sv_dead_letter]
```

**Workflow inputs:** `event`, `schemaName`

## Workers

**DeadLetterWorker** (task: `sv_dead_letter`)

Sends an invalid event to the dead-letter queue along with its validation errors.

- Reads `event`, `errors`. Writes `sentToDLQ`, `errors`

**ProcessValidWorker** (task: `sv_process_valid`)

Processes a valid event that has passed schema validation.

- Reads `event`, `schema`. Writes `processed`

**ValidateSchemaWorker** (task: `sv_validate_schema`)

Validates that an incoming event contains all required fields defined by the schema.  Required fields are: "type", "source", and "data".

- Reads `event`, `schemaName`. Writes `result`, `errors`, `schemaUsed`

---

**28 tests** | Workflow: `event_schema_validation` | Timeout: 120s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
