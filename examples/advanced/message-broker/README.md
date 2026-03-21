# Message Broker Pipeline in Java Using Conductor : Receive, Route, Deliver, Acknowledge, Log

## Messages Need Reliable Routing, Not Just Transport

A message arrives on the `orders` topic with `high` priority. It needs to be routed to the order processing service, not the analytics pipeline. Another message arrives on `notifications` with `low` priority. it should go to the batch notification queue, not the real-time push service. Topic-based routing, priority handling, delivery confirmation, and audit logging are the core responsibilities of a message broker.

Building this manually means writing routing tables, implementing delivery retries with backoff for each subscriber, tracking which messages were acknowledged and which need redelivery, and maintaining an audit log of every message's journey. When a delivery fails, you need to know the message ID, topic, priority, routing decision, and delivery attempt count. not just "something went wrong."

## The Solution

**You write the routing and delivery logic. Conductor handles the message lifecycle, retries, and audit logging.**

`MbrReceiveWorker` ingests the message and extracts its topic and priority metadata. `MbrRouteWorker` determines the delivery target based on topic routing rules and priority level. `MbrDeliverWorker` sends the payload to the routed destination. `MbrAcknowledgeWorker` confirms successful delivery and records the acknowledgment. `MbrLogWorker` writes the complete message lifecycle. receive, route decision, delivery, acknowledgment, to the audit log. Conductor ensures this five-step pipeline runs in sequence, retries failed deliveries, and gives you full traceability for every message.

### What You Write: Workers

Five workers manage the brokering lifecycle: message reception, topic-based routing, payload delivery, acknowledgment, and audit logging, each owning one phase of reliable message transit.### The Workflow

```
mbr_receive
 │
 ▼
mbr_route
 │
 ▼
mbr_deliver
 │
 ▼
mbr_acknowledge
 │
 ▼
mbr_log

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
