# Kafka Consumer

A data pipeline consumes events from a Kafka topic, but the raw Kafka records need deserialization, header extraction, offset tracking, and transformation before they can be used by downstream services. The consumer must also handle rebalancing and commit offsets only after successful processing.

## Pipeline

```
[kc_receive_message]
     |
     v
[kc_deserialize]
     |
     v
[kc_process_payload]
     |
     v
[kc_commit_offset]
```

**Workflow inputs:** `topic`, `partition`, `offset`, `messageKey`, `messageValue`

## Workers

**CommitOffset** (task: `kc_commit_offset`)

Commits the Kafka consumer offset after successful message processing.

- Reads `topic`, `partition`, `offset`, `processingResult`. Writes `committed`, `committedOffset`, `nextOffset`, `committedAt`

**Deserialize** (task: `kc_deserialize`)

Deserializes a raw Kafka message into structured data with type and schema information.

- Sets `action` = `"profile_updated"`
- Reads `rawMessage`, `format`. Writes `deserializedData`, `messageType`, `schemaVersion`

**ProcessPayload** (task: `kc_process_payload`)

Processes the deserialized Kafka message payload and applies the relevant action.

- Sets `result` = `"applied"`
- Reads `deserializedData`, `messageType`. Writes `processed`, `result`, `affectedEntities`, `processingTimeMs`

**ReceiveMessage** (task: `kc_receive_message`)

Receives a Kafka message and wraps it in a raw message envelope with headers and timestamp.

- Sets `format` = `"json"`
- Reads `topic`, `partition`, `offset`, `messageKey`, `messageValue`. Writes `rawMessage`, `format`

---

**35 tests** | Workflow: `kafka_consumer_wf` | Timeout: 300s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
