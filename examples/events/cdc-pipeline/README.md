# Cdc Pipeline

A microservices architecture uses separate databases per service, but downstream analytics needs a unified view. Change Data Capture (CDC) events fire when rows are inserted, updated, or deleted, but the raw events need parsing, deduplication (the same row can fire multiple events during a transaction), transformation to the analytics schema, and reliable delivery to the data warehouse.

## Pipeline

```
[cd_detect_changes]
     |
     v
[cd_transform_changes]
     |
     v
[cd_publish_downstream]
     |
     v
[cd_confirm_delivery]
```

**Workflow inputs:** `sourceTable`, `sinceTimestamp`, `targetTopic`

## Workers

**ConfirmDeliveryWorker** (task: `cd_confirm_delivery`)

Confirms that all published CDC messages were successfully delivered. Returns a deterministic delivery report.

- Reads `publishResult`, `messageIds`. Writes `allDelivered`, `confirmedAt`, `deliveryReport`

**DetectChangesWorker** (task: `cd_detect_changes`)

Detects CDC changes from a source table since a given timestamp. Returns a fixed set of 4 change records: INSERT, UPDATE, DELETE, INSERT.

- Reads `sourceTable`, `sinceTimestamp`. Writes `changes`, `changeCount`, `lastTimestamp`

**PublishDownstreamWorker** (task: `cd_publish_downstream`)

Publishes transformed CDC changes to a downstream topic. Publishes events downstream and returns message IDs.

- Reads `transformedChanges`, `targetTopic`. Writes `publishResult`, `messagesPublished`, `messageIds`

**TransformChangesWorker** (task: `cd_transform_changes`)

Transforms raw CDC change records into structured event payloads with eventType, entityId, payload, previousPayload, and metadata.

- Lowercases strings
- Reads `changes`, `sourceTable`. Writes `transformedChanges`

---

**4 tests** | Workflow: `cdc_pipeline_wf` | Timeout: 120s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
