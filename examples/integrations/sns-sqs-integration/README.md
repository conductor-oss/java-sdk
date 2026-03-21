# SNS SQS Integration in Java Using Conductor

## Publishing Messages Through SNS to SQS

Fan-out messaging with SNS and SQS involves a strict sequence: publish a message to an SNS topic, ensure the target SQS queue is subscribed to that topic, poll the queue for the delivered message, and process it (which typically includes parsing the SNS envelope and deleting the message from the queue). Each step depends on the previous one. you cannot receive a message that has not been published, and you need the subscription in place before messages will flow to the queue.

Without orchestration, you would chain AWS SDK calls manually, manage message IDs, subscription ARNs, and receipt handles between steps, and handle partial failures like a message published but not yet delivered. Conductor sequences the pipeline and routes topic ARNs, message IDs, and receipt handles between workers automatically.

## The Solution

**You just write the messaging workers. SNS publishing, SQS subscription, message receiving, and message processing. Conductor handles publish-to-process sequencing, SQS polling retries, and message ID and receipt handle routing between stages.**

Each worker integrates with one external system. Conductor manages the integration sequence, retry logic, timeout handling, and data transformation between systems.

### What You Write: Workers

Four workers run the messaging pipeline: SnsPublishWorker sends to an SNS topic, SubscribeQueueWorker links the SQS queue, ReceiveMessageWorker polls for delivered messages, and ProcessMessageWorker parses and deletes them.

| Worker | Task | What It Does |
|---|---|---|
| **ProcessMessageWorker** | `sns_process_message` | Processes a received SQS message. |
| **ReceiveMessageWorker** | `sns_receive_message` | Receives a message from an SQS queue. |
| **SnsPublishWorker** | `sns_publish` | Publishes a message to an SNS topic. |
| **SubscribeQueueWorker** | `sns_subscribe_queue` | Subscribes an SQS queue to an SNS topic. |

the workflow orchestration and error handling stay the same.

### The Workflow

```
sns_publish
 │
 ▼
sns_subscribe_queue
 │
 ▼
sns_receive_message
 │
 ▼
sns_process_message

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
