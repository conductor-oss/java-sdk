# Event Driven Workflow

A platform receives heterogeneous events (orders, payments, user registrations) through a single ingest endpoint. Each event needs classification by type, routing to the correct handler, and type-specific processing. An `order` event routes differently than a `payment` event, and unknown types must not be silently dropped.

## Pipeline

```
[ed_receive_event]
     |
     v
[ed_classify_event]
     |
     v
     <SWITCH>
       |-- order -> [ed_handle_order]
       |-- payment -> [ed_handle_payment]
       +-- default -> [ed_handle_generic]
```

**Workflow inputs:** `eventId`, `eventType`, `eventData`

## Workers

**ClassifyEventWorker** (task: `ed_classify_event`)

Classifies an event by its type into a category and priority.

- Reads `eventType`. Writes `category`, `priority`

**HandleGenericWorker** (task: `ed_handle_generic`)

Handles generic (unclassified) events.

- Sets `handler` = `"generic"`
- Reads `category`. Writes `handler`, `processed`, `category`

**HandleOrderWorker** (task: `ed_handle_order`)

Handles order-related events.

- Sets `handler` = `"order"`
- Reads `eventData`, `priority`. Writes `handler`, `processed`, `orderId`

**HandlePaymentWorker** (task: `ed_handle_payment`)

Handles payment-related events.

- Sets `handler` = `"payment"`
- Reads `eventData`, `priority`. Writes `handler`, `processed`, `paymentId`

**ReceiveEventWorker** (task: `ed_receive_event`)

Receives an incoming event and enriches it with metadata.

- Reads `eventType`, `eventData`. Writes `eventType`, `eventData`, `metadata`

---

**44 tests** | Workflow: `event_driven_wf` | Timeout: 120s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
