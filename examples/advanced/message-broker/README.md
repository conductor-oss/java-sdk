# Message Broker

A microservices architecture needs to decouple producers from consumers. The message broker pipeline accepts messages, routes them to the correct topic, delivers to subscribed consumers, and handles acknowledgment. If a consumer is offline, messages must be retained until it reconnects.

## Pipeline

```
[mbr_receive]
     |
     v
[mbr_route]
     |
     v
[mbr_deliver]
     |
     v
[mbr_acknowledge]
     |
     v
[mbr_log]
```

**Workflow inputs:** `message`, `topic`, `priority`

## Workers

**MbrAcknowledgeWorker** (task: `mbr_acknowledge`)

- Reads `delivered`. Writes `acknowledged`

**MbrDeliverWorker** (task: `mbr_deliver`)

- Writes `delivered`, `deliveryLatencyMs`

**MbrLogWorker** (task: `mbr_log`)

- Records wall-clock milliseconds
- Writes `logId`, `logged`

**MbrReceiveWorker** (task: `mbr_receive`)

- Captures `instant.now()` timestamps, records wall-clock milliseconds
- Writes `messageId`, `receivedAt`

**MbrRouteWorker** (task: `mbr_route`)

- Captures `instant.now()` timestamps
- Writes `destination`, `routedAt`

---

**20 tests** | Workflow: `mbr_message_broker` | Timeout: 60s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
