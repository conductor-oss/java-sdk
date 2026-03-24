# Order Fulfillment Across Four Microservices

An order flows through four independently deployed services: order validation, inventory
reservation, shipping, and customer notification. This workflow chains them explicitly so
each service receives exactly the data it needs from the previous step.

## Workflow

```
orderId, items, customerId
          |
          v
+---------------------+     +-------------------------+     +-----------------------+     +----------------------------+
| isc_order_service   | --> | isc_inventory_service   | --> | isc_shipping_service  | --> | isc_notification_service   |
+---------------------+     +-------------------------+     +-----------------------+     +----------------------------+
  orderRef: REF-...           reserved: true                  trackingId: TRACK-...        sent: true
  validated: true             warehouse: "WH-EAST-1"          eta: "2 days"                channel: "email"
```

## Workers

**OrderServiceWorker** -- Validates and processes the order. Returns
`orderRef: "REF-{timestamp}"`, `validated: true`.

**InventoryServiceWorker** -- Reserves items for the order reference. Returns
`reserved: true`, `warehouse: "WH-EAST-1"`.

**ShippingServiceWorker** -- Creates a shipment from the warehouse. Returns
`trackingId: "TRACK-{timestamp}"`, `eta: "2 days"`.

**NotificationServiceWorker** -- Sends the tracking ID to the customer via email. Returns
`sent: true`, `channel: "email"`.

## Tests

12 unit tests cover order processing, inventory reservation, shipping, and notification.

## Running

See [../../RUNNING.md](../../RUNNING.md) for setup and execution instructions.
