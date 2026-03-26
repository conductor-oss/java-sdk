# E-Commerce Order Saga

An e-commerce order must reserve inventory, charge the customer, and ship the package. If shipping fails (item damaged, carrier unavailable), the payment must be refunded and the inventory released. The compensation steps must receive the specific IDs from the forward steps -- `paymentId` = `"PAY-001"` and `reservationId` = `"INV-001"` -- to know exactly what to reverse.

## Workflow

```
spi_reserve_inventory ──> spi_charge_payment ──> spi_ship_order
                                                      │
                                               SWITCH(shipStatus)
                                                 ├── "failed" ──> spi_refund_payment ──> spi_release_inventory
                                                 └── default ──> (done)
```

Workflow `saga_payment_inventory` accepts `orderId`, `amount`, and `shouldFail`. The SWITCH task `check_ship_ref` evaluates `${ship_ref.output.shipStatus}`.

## Workers

**ReserveInventoryWorker** (`spi_reserve_inventory`) -- reads `orderId` from input (defaults to `"unknown"`). Returns `reservationId` = `"INV-001"` and `inventoryStatus` = `"reserved"`.

**ChargePaymentWorker** (`spi_charge_payment`) -- reads `orderId` and `amount` from input. Returns `paymentId` = `"PAY-001"` and `paymentStatus` = `"charged"`.

**ShipOrderWorker** (`spi_ship_order`) -- reads `orderId` and `shouldFail` from input. Always returns `COMPLETED`. When `shouldFail` is `true`, sets `shipStatus` = `"failed"`. Otherwise sets `shipStatus` = `"shipped"`. Both paths return `shipmentId` = `"SHIP-001"`.

**RefundPaymentWorker** (`spi_refund_payment`) -- receives `paymentId` and `orderId` from the forward steps. Returns `refundStatus` = `"refunded"`.

**ReleaseInventoryWorker** (`spi_release_inventory`) -- receives `reservationId` and `orderId` from the forward steps. Returns `inventoryStatus` = `"released"`.

## Workflow Output

The workflow produces `orderId`, `shipStatus`, `shipmentId`, `compensated` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Project Structure

This example contains 5 worker implementations in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `saga_payment_inventory` defines 4 tasks with input parameters `orderId`, `amount`, `shouldFail` and a timeout of `120` seconds.

## Tests

20 tests cover the happy shipping path, the compensation path when shipping fails, correct ID propagation from forward to compensation steps, and the SWITCH routing based on `shipStatus`.

## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
