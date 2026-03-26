# Event Choreography

An order fulfillment system spans multiple services (inventory, payment, shipping) that must coordinate without a central controller. Each service emits domain events, subscribes to events from other services, and executes its local logic. The choreography must handle the happy path and compensating actions when a downstream service rejects.

## Pipeline

```
[ch_order_service]
     |
     v
[ch_emit_order_event]
     |
     v
[ch_payment_service]
     |
     v
[ch_emit_payment_event]
     |
     v
[ch_inventory_service]
     |
     v
[ch_emit_inventory_event]
     |
     v
[ch_notification_service]
```

**Workflow inputs:** `orderId`, `items`, `customerId`

## Workers

**OrderServiceWorker** (task: `ch_order_service`) -- Processes the incoming order, calculates `totalAmount` from `items`, and sets `status` = `"created"`. Reads `orderId`, `items`.

**EmitOrderEventWorker** (task: `ch_emit_order_event`) -- Publishes the order-created event to the event bus. Reads `eventType`, `orderId`. Writes `emitted`, `eventType`.

**PaymentServiceWorker** (task: `ch_payment_service`) -- Charges the customer. Reads `orderId`, `amount`. Writes `paid`, `transactionId`.

**EmitPaymentEventWorker** (task: `ch_emit_payment_event`) -- Publishes the payment-completed event. Reads `eventType`, `orderId`. Writes `emitted`, `eventType`.

**InventoryServiceWorker** (task: `ch_inventory_service`) -- Reserves stock for each line item. Reads `orderId`, `items`. Writes `reserved`, `itemCount`.

**EmitInventoryEventWorker** (task: `ch_emit_inventory_event`) -- Publishes the inventory-reserved event. Reads `eventType`, `orderId`. Writes `emitted`, `eventType`.

**NotificationServiceWorker** (task: `ch_notification_service`) -- Sends order confirmation to the customer. Reads `orderId`, `customerId`, `event`. Writes `notificationStatus`, `channel`.

---

**56 tests** | Workflow: `event_choreography` | Timeout: 60s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
