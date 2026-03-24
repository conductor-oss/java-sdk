# Event-Driven Pipeline: Emit, Process, Project, Notify

In an event-driven architecture, an event is emitted, processed to update state, projected
onto a read model, and fanned out to subscribers. This workflow makes that pipeline explicit:
emit the event with a unique ID, process it (e.g. `"order_updated"`), update the read model
projection, and notify subscribers (billing, shipping, analytics).

## Workflow

```
eventType, payload, source
            |
            v
+------------------+     +---------------------+     +--------------------------+     +--------------------------+
| edm_emit_event   | --> | edm_process_event   | --> | edm_update_projection    | --> | edm_notify_subscribers   |
+------------------+     +---------------------+     +--------------------------+     +--------------------------+
  eventId generated        result: "order_updated"    projectionUpdated: true          count: 3
  timestamp set            subscribers: [billing,                                      (billing, shipping,
                           shipping, analytics]                                         analytics)
```

## Workers

**EmitEventWorker** -- Emits an event of `eventType` from `source`. Returns a unique
`eventId` and a timestamp.

**ProcessEventWorker** -- Processes the event. Returns `result: "order_updated"` and the
`subscribers` list: `["billing", "shipping", "analytics"]`.

**UpdateProjectionWorker** -- Updates the read model. Returns `projectionUpdated: true`.

**NotifySubscribersWorker** -- Notifies all 3 subscribers. Returns `count: 3`.

## Tests

8 unit tests cover event emission, processing, projection updates, and subscriber
notification.

## Running

See [../../RUNNING.md](../../RUNNING.md) for setup and execution instructions.
