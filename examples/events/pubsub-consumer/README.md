# Pubsub Consumer

A notification system subscribes to a Google Cloud Pub/Sub topic. Incoming messages need acknowledgment management, payload deserialization, idempotency checks (Pub/Sub delivers at least once), and routing to the appropriate handler based on message attributes.

## Pipeline

```
[ps_receive_message]
     |
     v
[ps_decode_payload]
     |
     v
[ps_process_data]
     |
     v
[ps_ack_message]
```

**Workflow inputs:** `subscription`, `messageId`, `publishTime`, `data`, `attributes`

## Workers

**PsAckMessageWorker** (task: `ps_ack_message`)

Acknowledges a Pub/Sub message after successful processing. Records the subscription, message ID, and acknowledgement timestamp.

- Reads `subscription`, `messageId`, `processingResult`. Writes `acknowledged`, `ackedAt`

**PsDecodePayloadWorker** (task: `ps_decode_payload`)

Decodes a base64-encoded Pub/Sub message payload into structured sensor data. Determines event type from message attributes or defaults to "iot.sensor.reading".

- Reads `encodedData`, `encoding`, `attributes`. Writes `decodedData`, `eventType`

**PsProcessDataWorker** (task: `ps_process_data`)

Processes decoded sensor data by checking thresholds and generating alerts. Thresholds: temperature high=85, temperature low=55, humidity high=70. With default values (temp=72.4, humidity=45.2) no alerts are produced.

- `TEMP_HIGH` = 85.0; `TEMP_LOW` = 55.0; `HUMIDITY_HIGH` = 70.0
- Parses strings to `double`
- Sets `result` = `"stored"`
- Reads `decodedData`, `eventType`. Writes `processed`, `result`, `sensorId`, `alertCount`, `alerts`

**PsReceiveMessageWorker** (task: `ps_receive_message`)

Receives a Pub/Sub message and extracts its data, encoding, and attributes. The raw data is passed through as encodedData with base64 encoding marker.

- Reads `subscription`, `messageId`, `publishTime`, `data`, `attributes`. Writes `encodedData`, `encoding`, `attributes`, `receivedAt`

---

**37 tests** | Workflow: `pubsub_consumer_wf` | Timeout: 120s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
