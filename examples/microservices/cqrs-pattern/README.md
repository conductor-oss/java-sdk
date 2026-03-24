# CQRS: Separating Command Validation from Read Model Updates

In a traditional CRUD system, reads and writes share the same model, which causes contention
at scale. This workflow implements CQRS by validating a command against the aggregate,
persisting the resulting event, and updating the read model as a separate projection. The
read model can be queried independently.

## Workflow

```
command, aggregateId, data
           |
           v
+--------------------------+     +----------------------+     +---------------------------+
| cqrs_validate_command    | --> | cqrs_persist_event   | --> | cqrs_update_read_model    |
+--------------------------+     +----------------------+     +---------------------------+
  valid: true                     eventId generated            projection updated for
  event: {type:                   version: 5                   aggregateId
  "ITEM_UPDATED", data}          persisted: true
```

## Workers

**ValidateCommandWorker** -- Validates `command` for `aggregateId`. Returns `valid: true`
and an `event` with `type: "ITEM_UPDATED"` and the command data.

**PersistEventWorker** -- Stores the event. Returns a generated `eventId`, `version: 5`,
`persisted: true`.

**UpdateReadModelWorker** -- Updates the read model projection for the aggregate. Returns
`updated: true`.

**QueryReadModelWorker** -- (Available for queries) Returns the current state:
`{id: aggregateId, name: "Widget"}`.

## Tests

8 unit tests cover command validation, event persistence, read model updates, and queries.

## Running

See [../../RUNNING.md](../../RUNNING.md) for setup and execution instructions.
