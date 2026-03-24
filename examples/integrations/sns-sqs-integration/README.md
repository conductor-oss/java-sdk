# Sns Sqs Integration

Orchestrates sns sqs integration through a multi-stage Conductor workflow.

**Input:** `topicArn`, `queueUrl`, `messageBody` | **Timeout:** 60s

## Pipeline

```
sns_publish
    │
sns_subscribe_queue
    │
sns_receive_message
    │
sns_process_message
```

## Workers

**ProcessMessageWorker** (`sns_process_message`): Processes a received SQS message.

Reads `messageBody`, `receiptHandle`. Outputs `processed`, `deletedFromQueue`, `processedAt`.

**ReceiveMessageWorker** (`sns_receive_message`): Receives a message from an SQS queue.

```java
String receiptHandle = "rh-" + Long.toString(System.currentTimeMillis(), 36);
```

Reads `messageId`, `queueUrl`. Outputs `body`, `receiptHandle`, `approximateReceiveCount`.

**SnsPublishWorker** (`sns_publish`): Publishes a message to an SNS topic.

```java
String messageId = "sns-msg-" + Long.toString(System.currentTimeMillis(), 36);
```

Reads `topicArn`. Outputs `messageId`, `sequenceNumber`.

**SubscribeQueueWorker** (`sns_subscribe_queue`): Subscribes an SQS queue to an SNS topic.

Reads `queueUrl`, `topicArn`. Outputs `subscriptionArn`, `protocol`.

## Tests

**8 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
