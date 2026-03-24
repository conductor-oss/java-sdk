# Event Routing

A multi-tenant platform receives events from different tenants that must be routed to tenant-specific processing queues. The router needs to extract the tenant ID, look up the routing rules, direct the event to the correct handler, and handle unknown tenants gracefully.

## Pipeline

```
[eo_receive_event]
     |
     v
[eo_extract_type]
     |
     v
     <SWITCH>
       |-- user -> [eo_user_processor]
       |-- order -> [eo_order_processor]
       +-- default -> [eo_system_processor]
```

**Workflow inputs:** `eventId`, `eventDomain`, `eventData`

## Workers

**ExtractTypeWorker** (task: `eo_extract_type`)

Splits the eventDomain string by "." to extract the domain (first part) and subType (remaining parts joined by "."). Example: "user.profile_update" -> domain="user", subType="profile_update"

- Reads `eventDomain`. Writes `domain`, `subType`

**OrderProcessorWorker** (task: `eo_order_processor`)

Processes order-domain events. Extracts the orderId from eventData and returns a fixed result indicating fulfillment has started.

- Reads `eventId`, `subType`, `eventData`. Writes `processed`, `processor`, `orderId`, `fulfillmentStarted`, `processedAt`

**ReceiveEventWorker** (task: `eo_receive_event`)

Receives an incoming event and passes through its domain and data, stamping a receivedAt timestamp.

- Reads `eventId`, `eventDomain`, `eventData`. Writes `eventDomain`, `eventData`, `receivedAt`

**SystemProcessorWorker** (task: `eo_system_processor`)

Default processor for events that do not match user or order domains. Passes through the domain and marks the event as processed.

- Reads `eventId`, `domain`. Writes `processed`, `processor`, `domain`, `processedAt`

**UserProcessorWorker** (task: `eo_user_processor`)

Processes user-domain events. Returns a fixed result indicating the user event was handled: profile updated, notification sent, and audit logged.

- Reads `eventId`, `subType`. Writes `processed`, `processor`, `actions`, `processedAt`

---

**5 tests** | Workflow: `event_routing_wf` | Timeout: 120s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
