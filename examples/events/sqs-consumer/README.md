# SQS Consumer in Java Using Conductor

SQS Consumer. receive an SQS message, validate it, process the event, and delete the message from the queue. ## The Problem

You need to process messages from an AWS SQS queue. Each message must be received, validated for correct structure, processed according to your business logic, and deleted from the queue only after successful processing. If you delete before processing, you lose the message on failure; if you do not delete after success, SQS redelivers it after the visibility timeout expires.

Without orchestration, you'd write an SQS polling loop with ReceiveMessage, validation logic, processing, and DeleteMessage calls. handling visibility timeout extensions for slow processing, managing long polling, and dealing with poison messages that repeatedly fail and block the queue.

## The Solution

**You just write the message-receive, validate, process, and queue-delete workers. Conductor handles receive-to-delete sequencing, guaranteed queue deletion only after processing, and per-message audit trail.**

Each SQS consumption concern is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of receiving the message, validating it, processing the event, and deleting it from the queue, retrying on transient failures, tracking every message's lifecycle, and resuming if the process crashes. ### What You Write: Workers

Four workers process SQS messages: ReceiveMessageWorker parses the SQS message body, ValidateMessageWorker checks required fields, ProcessMessageWorker applies business logic, and DeleteMessageWorker removes the message from the queue after success.

| Worker | Task | What It Does |
|---|---|---|
| **DeleteMessageWorker** | `qs_delete_message` | Deletes an SQS message from the queue after successful processing, confirming deletion with a timestamp. |
| **ProcessMessageWorker** | `qs_process_message` | Processes a validated SQS message based on its type. For invoice.generated events, records the invoice and returns th... |
| **ReceiveMessageWorker** | `qs_receive_message` | Receives and parses an SQS message body into structured data, extracting the event payload and SQS message attributes. |
| **ValidateMessageWorker** | `qs_validate_message` | Validates an SQS message payload by checking required fields (eventType, invoiceId, customerId, amount) and confirmin... |

Workers implement event processing with realistic payloads so you can trace the full event flow without external message brokers. Replace the simulation with real event sources. the workflow and routing logic stay the same.

### The Workflow

```
qs_receive_message
 │
 ▼
qs_validate_message
 │
 ▼
qs_process_message
 │
 ▼
qs_delete_message

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
