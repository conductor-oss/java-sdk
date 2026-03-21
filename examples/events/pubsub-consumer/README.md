# Pubsub Consumer in Java Using Conductor

Pub/Sub Consumer. receive a Pub/Sub message, decode the base64 payload, process sensor data with threshold checks, and acknowledge the message. ## The Problem

You need to process messages from a Google Cloud Pub/Sub subscription. Each message arrives base64-encoded with attributes metadata, must be decoded, processed (e.g., sensor data with threshold checks), and acknowledged so Pub/Sub stops redelivering it. Failing to acknowledge means the message is redelivered indefinitely; acknowledging before processing means you lose it on failure.

Without orchestration, you'd build a Pub/Sub subscriber with manual message decoding, threshold logic, and acknowledge/nack calls. handling message lease extensions for slow processing, managing subscriber flow control, and debugging why messages are being redelivered.

## The Solution

**You just write the message-receive, payload-decode, sensor-processing, and acknowledgment workers. Conductor handles receive-to-ack sequencing, guaranteed acknowledgment only after processing, and full message lifecycle visibility.**

Each Pub/Sub consumption concern is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of receiving the message, decoding the base64 payload, processing the sensor data with threshold checks, and acknowledging the message, retrying on transient failures, tracking every message's lifecycle, and resuming if the process crashes. ### What You Write: Workers

Four workers process Pub/Sub messages: PsReceiveMessageWorker extracts the raw message, PsDecodePayloadWorker converts the base64 payload into structured sensor data, PsProcessDataWorker evaluates threshold alerts, and PsAckMessageWorker confirms delivery.

| Worker | Task | What It Does |
|---|---|---|
| **PsAckMessageWorker** | `ps_ack_message` | Acknowledges a Pub/Sub message after successful processing. Records the subscription, message ID, and acknowledgement... |
| **PsDecodePayloadWorker** | `ps_decode_payload` | Decodes a base64-encoded Pub/Sub message payload into structured sensor data. Determines event type from message attr... |
| **PsProcessDataWorker** | `ps_process_data` | Processes decoded sensor data by checking thresholds and generating alerts. Thresholds: temperature high=85, temperat... |
| **PsReceiveMessageWorker** | `ps_receive_message` | Receives a Pub/Sub message and extracts its data, encoding, and attributes. The raw data is passed through as encoded... |

Workers implement event processing with realistic payloads so you can trace the full event flow without external message brokers. Replace the simulation with real event sources. the workflow and routing logic stay the same.

### The Workflow

```
ps_receive_message
 │
 ▼
ps_decode_payload
 │
 ▼
ps_process_data
 │
 ▼
ps_ack_message

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
