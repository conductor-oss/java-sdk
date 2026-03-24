# Order Fulfillment: Place, Reserve, Pay, Ship

In a choreography model, each service emits events and hopes the next one picks them up. In
this orchestrated version, the workflow explicitly sequences order placement, inventory
reservation (with a real `INVENTORY` map tracking stock for widget/gadget/gizmo/doohickey/
thingamajig), payment processing (with idempotency key deduplication), and shipping.

## Workflow

```
orderId, items, customerId
           |
           v
+--------------------+     +--------------------------+     +------------------------+     +-------------------+
| cvo_place_order    | --> | cvo_reserve_inventory    | --> | cvo_process_payment    | --> | cvo_ship_order    |
+--------------------+     +--------------------------+     +------------------------+     +-------------------+
  status: "placed"          reserves from INVENTORY map      duplicate detection via       trackingId generated
  itemCount, total          {widget:500, gadget:200,         idempotencyKey                warehouse + carrier
  totalQuantity             gizmo:150, doohickey:75,         transactionId: TXN-...        estimatedDelivery
                            thingamajig:300}
```

## Workers

**PlaceOrderWorker** -- Takes `orderId`, `items` list, and `customerId`. Computes `total`
and `totalQuantity` from items. Returns `status: "placed"` with a timestamp.

**ReserveInventoryWorker** -- Maintains a static `INVENTORY` map with 5 products. Checks
stock for each item and reserves quantities. Returns `reserved` boolean and `reservations`
list. Fails if any item has insufficient stock.

**ProcessPaymentWorker** -- Generates an idempotency key from orderId + amount. Detects
duplicate payments by checking a `PROCESSED_PAYMENTS` set. Returns `transactionId` like
`"TXN-..."` and a `charged` flag.

**ShipOrderWorker** -- Ships the order from a deterministic warehouse with a carrier.
Returns `trackingId`, `shippedAt`, and `estimatedDelivery` (3 days out).

## Tests

18 unit tests cover order placement, inventory reservation, payment idempotency, and
shipping.

## Running

See [../../RUNNING.md](../../RUNNING.md) for setup and execution instructions.
