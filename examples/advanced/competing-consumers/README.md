# Competing Consumers in Java Using Conductor : Publish, Compete, Process, Acknowledge

## Scaling Throughput with Multiple Consumers

When a single consumer can't keep up with the message rate on a queue, you add more consumers. But multiple consumers reading from the same queue introduces coordination problems: two consumers might grab the same message, a consumer might crash after claiming a message but before processing it, and you need to know which consumer actually handled each task for debugging and audit.

The competing consumers pattern formalizes this: publish a message to a queue, let multiple consumers compete for it (only one wins), process the task with the winner, and acknowledge completion so the message is removed. Getting the compete-process-acknowledge lifecycle right. especially with retries and crash recovery, requires careful coordination that's easy to get wrong in hand-rolled code.

## The Solution

**You write the publish and consume logic. Conductor handles the competition coordination, retries, and audit trail.**

`CcsPublishWorker` places the task payload on the queue and returns a message ID. `CcsCompeteWorker` simulates the consumer competition. given the consumer count, it determines which consumer instance wins the race to claim the message. `CcsProcessWorker` executes the actual business logic with the winning consumer's context. `CcsAcknowledgeWorker` confirms the message was processed and removes it from the queue. Conductor ensures these steps run in sequence, retries if the processing step fails, and records which consumer won and what result it produced.

### What You Write: Workers

Four workers handle the compete-and-process lifecycle. Publishing to a shared queue, consumer competition, task execution by the winner, and delivery acknowledgment.### The Workflow

```
ccs_publish
 │
 ▼
ccs_compete
 │
 ▼
ccs_process
 │
 ▼
ccs_acknowledge

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
