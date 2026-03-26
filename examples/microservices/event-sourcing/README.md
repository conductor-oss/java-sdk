# Event Sourcing: Validate, Append, Rebuild State, Publish

Instead of storing the current state, event sourcing stores every change as an immutable
event. This workflow validates the incoming event, appends it to the event store (version 5),
rebuilds the aggregate state from the full event history (balance: 1500, status: "active"),
and publishes the event to subscribers.

## Workflow

```
aggregateId, eventType, eventData
               |
               v
+-----------------------+     +---------------------+     +-----------------------+     +---------------------+
| es_validate_event     | --> | es_append_event     | --> | es_rebuild_state      | --> | es_publish_event    |
+-----------------------+     +---------------------+     +-----------------------+     +---------------------+
  validatedEvent with          eventId generated           state rebuilt from            published: true
  type + data + timestamp      version: 5                  5 events: balance=1500
                               appended: true              status="active"
```

## Workers

**ValidateEventWorker** -- Validates `eventType` for `aggregateId`. Returns
`validatedEvent` containing `type`, `data`, and `timestamp`.

**AppendEventWorker** -- Appends the event to the store. Returns a generated `eventId`,
`version: 5`, `appended: true`.

**RebuildStateWorker** -- Replays events to rebuild state. Returns
`state: {balance: 1500, status: "active"}` from `version` events.

**PublishEventWorker** -- Publishes the event to subscribers. Returns `published: true`.

## Tests

8 unit tests cover event validation, appending, state rebuild, and publishing.

## Running

See [../../RUNNING.md](../../RUNNING.md) for setup and execution instructions.
