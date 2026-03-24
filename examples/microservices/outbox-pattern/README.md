# Transactional Outbox: Write Entity and Event Atomically

Publishing an event after a database write is not atomic -- if the app crashes between the
write and the publish, the event is lost. The outbox pattern writes the entity and an outbox
entry in the same transaction, then a separate process polls the outbox, publishes to the
message broker, and marks the entry as published.

## Workflow

```
entityId, entityData, eventType
              |
              v
+-----------------------+     +-------------------+     +----------------------+     +----------------------+
| ob_write_with_outbox  | --> | ob_poll_outbox    | --> | ob_publish_event     | --> | ob_mark_published    |
+-----------------------+     +-------------------+     +----------------------+     +----------------------+
  outboxId generated           event: ORDER_CREATED     published to                  marked: true
  written: true (atomic)       entityId: ORD-1           orders-topic
                               destination:              messageId: MSG-...
                               orders-topic
```

## Workers

**WriteWithOutboxWorker** -- Writes entity `entityId` and outbox entry atomically. Returns
`outboxId`, `written: true`.

**PollOutboxWorker** -- Finds the unpublished event. Returns `event: {type: "ORDER_CREATED",
entityId: "ORD-1"}`, `destination: "orders-topic"`.

**PublishEventWorker** -- Publishes to the destination topic. Returns `published: true`,
`messageId: "MSG-{timestamp}"`.

**MarkPublishedWorker** -- Marks the outbox entry as published. Returns `marked: true`.

## Tests

8 unit tests cover atomic writes, outbox polling, publishing, and marking.

## Running

See [../../RUNNING.md](../../RUNNING.md) for setup and execution instructions.
