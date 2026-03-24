# Kafka Consumer in Java Using Conductor

Kafka consumer pipeline: receives a message, deserializes it, processes the payload, and commits the offset.

## The Problem

You need to process messages from a Kafka topic reliably. Each message must be received from a specific topic/partition/offset, deserialized from its wire format, processed according to your business logic, and have its offset committed only after successful processing. Committing the offset before processing is complete means you lose the message on failure; not committing means you reprocess it on restart.

Without orchestration, you'd write a Kafka consumer loop with manual offset management, deserialization try/catch, processing logic, and commit calls. handling rebalances, serialization errors, and exactly-once semantics with ad-hoc code that becomes increasingly fragile as message formats evolve.

## The Solution

**You just write the message-receive, deserialize, process, and offset-commit workers. Conductor handles receive-to-commit sequencing, guaranteed offset commit only after processing, and per-message lifecycle tracking.**

Each Kafka consumption concern is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of receiving the message, deserializing it, processing the payload, and committing the offset, retrying on transient failures, tracking every message's processing lifecycle, and resuming from the last step if the process crashes.

### What You Write: Workers

Four workers form the Kafka consumption pipeline: ReceiveMessage extracts the raw message, Deserialize converts it to structured data, ProcessPayload applies business logic, and CommitOffset marks the message as consumed only after successful processing.

| Worker | Task | What It Does |
|---|---|---|
| **CommitOffset** | `kc_commit_offset` | Commits the Kafka consumer offset after successful message processing. |
| **Deserialize** | `kc_deserialize` | Deserializes a raw Kafka message into structured data with type and schema information. |
| **ProcessPayload** | `kc_process_payload` | Processes the deserialized Kafka message payload and applies the relevant action. |
| **ReceiveMessage** | `kc_receive_message` | Receives a Kafka message and wraps it in a raw message envelope with headers and timestamp. |

Workers implement event processing with realistic payloads so you can trace the full event flow without external message brokers. Replace the simulation with real event sources. the workflow and routing logic stay the same.

### The Workflow

```
kc_receive_message
 │
 ▼
kc_deserialize
 │
 ▼
kc_process_payload
 │
 ▼
kc_commit_offset

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
