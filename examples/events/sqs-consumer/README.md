# Sqs Consumer

A microservice consumes messages from an AWS SQS queue. Each message needs receipt handle management, JSON body parsing, business logic execution, and deletion from the queue only after successful processing. Failed messages must return to the queue for retry via the visibility timeout.

## Pipeline

```
[qs_receive_message]
     |
     v
[qs_validate_message]
     |
     v
[qs_process_message]
     |
     v
[qs_delete_message]
```

**Workflow inputs:** `queueUrl`, `messageId`, `receiptHandle`, `body`

## Workers

**DeleteMessageWorker** (task: `qs_delete_message`)

Deletes an SQS message from the queue after successful processing, confirming deletion with a timestamp.

- Reads `queueUrl`, `receiptHandle`, `processingResult`. Writes `deleted`, `deletedAt`

**ProcessMessageWorker** (task: `qs_process_message`)

Processes a validated SQS message based on its type. For invoice.generated events, records the invoice and returns the processing result.

- Reads `validatedData`, `messageType`. Writes `processed`, `result`, `invoiceId`, `processingTimeMs`

**ReceiveMessageWorker** (task: `qs_receive_message`)

Receives and parses an SQS message body into structured data, extracting the event payload and SQS message attributes.

- Reads `queueUrl`, `messageId`, `receiptHandle`, `body`. Writes `parsedBody`, `attributes`

**ValidateMessageWorker** (task: `qs_validate_message`)

Validates an SQS message payload by checking required fields (eventType, invoiceId, customerId, amount) and confirming it is a first receive.

- Reads `parsedBody`, `attributes`. Writes `valid`, `checks`, `validatedData`, `messageType`

---

**36 tests** | Workflow: `sqs_consumer_wf` | Timeout: 120s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
