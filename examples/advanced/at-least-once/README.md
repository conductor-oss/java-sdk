# At-Least-Once Message Delivery in Java Using Conductor : Receive, Process, Acknowledge, Verify

## Guaranteeing Every Message Gets Processed

Message queues like SQS and Kafka deliver messages with a receipt handle and a visibility timeout. If your consumer crashes after processing but before acknowledging, the message reappears on the queue and gets delivered again. If it crashes after acknowledging but before recording delivery, you lose the audit trail. The gap between "processed" and "acknowledged" is where messages get lost or silently duplicated.

Manually coordinating receive, process, acknowledge, and verify steps means tracking receipt handles, managing visibility timeout windows, retrying failed acknowledgments before the message becomes visible again, and logging every outcome for auditability. One missed edge case and you either lose a message or process it without anyone knowing.

## The Solution

**You write the receive and acknowledge logic. Conductor handles retries, delivery verification, and execution history.**

Each stage of the delivery pipeline is a separate worker. `AloReceiveWorker` ingests the message and produces a receipt handle with a visibility timeout. `AloProcessWorker` handles the business logic for the payload. `AloAcknowledgeWorker` deletes the message from the queue using the receipt handle, confirming successful processing. `AloVerifyDeliveryWorker` checks that the acknowledgment was recorded, closing the loop. If any step fails. the process call times out, the acknowledge call gets a transient error. Conductor retries it automatically, and the full execution history shows exactly where things stand.

### What You Write: Workers

Four workers own the delivery lifecycle: receive, process, acknowledge, and verify, each responsible for one step of the at-least-once guarantee.### The Workflow

```
alo_receive
 │
 ▼
alo_process
 │
 ▼
alo_acknowledge
 │
 ▼
alo_verify_delivery

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
