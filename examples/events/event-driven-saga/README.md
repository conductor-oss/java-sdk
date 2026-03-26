# Event Driven Saga

An order processing system must coordinate payment, inventory reservation, and shipping as an atomic business transaction, but each runs in a different service with its own database. If payment succeeds but inventory is out of stock, the payment must be refunded. The saga pattern coordinates these steps with compensating actions.

## Pipeline

```
[ds_create_order]
     |
     v
[ds_process_payment]
     |
     v
     <SWITCH>
       |-- success -> [ds_ship_order]
       |-- failed -> [ds_compensate_payment]
       |-- failed -> [ds_cancel_order]
```

**Workflow inputs:** `orderId`, `amount`, `shippingAddress`

## Workers

**CancelOrderWorker** (task: `ds_cancel_order`)

Cancels an order as part of saga compensation.

- Reads `orderId`, `reason`. Writes `cancelled`, `orderId`

**CompensatePaymentWorker** (task: `ds_compensate_payment`)

Compensates (refunds) a payment when the saga needs to roll back.

- Reads `orderId`, `transactionId`. Writes `refunded`, `transactionId`

**CreateOrderWorker** (task: `ds_create_order`)

Creates an order in the saga.

- Reads `orderId`, `amount`. Writes `orderId`, `amount`, `orderStatus`

**ProcessPaymentWorker** (task: `ds_process_payment`)

Processes payment for an order.

- Reads `orderId`, `amount`. Writes `paymentStatus`, `transactionId`, `amount`

**ShipOrderWorker** (task: `ds_ship_order`)

Ships an order after successful payment.

- Reads `orderId`, `address`. Writes `shipped`, `trackingNumber`

---

**5 tests** | Workflow: `event_driven_saga` | Timeout: 60s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
