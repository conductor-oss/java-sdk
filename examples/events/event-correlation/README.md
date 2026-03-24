# Event Correlation

A monitoring system receives events from multiple subsystems (load balancer, application server, database) that all relate to the same user request but carry different correlation IDs. The system needs to match events by correlation key, merge their data into a unified view, and detect when expected events are missing.

## Pipeline

```
[ec_init_correlation]
     |
     v
     +───────────────────────────────────────────────────────────────────+
     | [ec_receive_order] | [ec_receive_payment] | [ec_receive_shipping] |
     +───────────────────────────────────────────────────────────────────+
     [join]
     |
     v
[ec_correlate_events]
     |
     v
[ec_process_correlated]
```

**Workflow inputs:** `correlationId`, `expectedEvents`

## Workers

**CorrelateEventsWorker** (task: `ec_correlate_events`)

Correlates order, payment, and shipping events into a unified data set. Returns deterministic output with fixed matchScore of 1.0.

- Reads `correlationId`, `orderEvent`, `paymentEvent`, `shippingEvent`. Writes `eventsCorrelated`, `matchScore`, `correlatedData`

**InitCorrelationWorker** (task: `ec_init_correlation`)

Initializes a correlation session with a fixed correlation ID and timestamp. Returns deterministic output with no randomness.

- Reads `correlationId`, `expectedEvents`. Writes `correlationId`, `sessionStarted`

**ProcessCorrelatedWorker** (task: `ec_process_correlated`)

Processes correlated event data and determines the action to take. Returns deterministic output with fixed timestamps.

- Sets `action` = `"fulfill_order"`
- Reads `correlationId`, `correlatedData`, `matchScore`. Writes `action`, `processedAt`, `summary`

**ReceiveOrderWorker** (task: `ec_receive_order`)

perform  receiving an order event for event correlation. Returns deterministic, fixed order data.

- Sets `type` = `"order"`
- Reads `correlationId`, `eventType`. Writes `event`

**ReceivePaymentWorker** (task: `ec_receive_payment`)

perform  receiving a payment event for event correlation. Returns deterministic, fixed payment data.

- Sets `type` = `"payment"`, `method` = `"credit_card"`
- Reads `correlationId`, `eventType`. Writes `event`

**ReceiveShippingWorker** (task: `ec_receive_shipping`)

perform  receiving a shipping event for event correlation. Returns deterministic, fixed shipping data.

- Sets `type` = `"shipping"`
- Reads `correlationId`, `eventType`. Writes `event`

---

**51 tests** | Workflow: `event_correlation_wf` | Timeout: 60s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
