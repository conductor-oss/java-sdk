# Implementing E-Commerce Order Saga in Java with Conductor : Inventory, Payment, and Shipping with Compensation

## The Problem

You process an e-commerce order: reserve the inventory so it's not sold to someone else, charge the customer's payment method, and ship the order. If shipping fails (item damaged, carrier unavailable), the payment must be refunded and the inventory must be released back to stock. Each step depends on the previous one. you can't charge without reserved inventory, and you can't ship without payment.

Without orchestration, order processing is a monolithic transaction attempt. A shipping failure after successful payment leaves the customer charged with no shipment. A payment failure after inventory reservation leaves stock locked. Compensating each combination of partial success requires tangled error-handling code.

## The Solution

**You just write the order processing and compensation logic. Conductor handles the reserve-charge-ship sequence, SWITCH-based failure detection, reverse-order compensation (refund then release), retries on each step, and a complete audit trail of every order showing which steps completed and which compensations ran.**

Each forward step (reserve inventory, charge payment, ship order) and its compensation (release inventory, refund payment) are independent workers. Conductor runs the forward steps in sequence. When shipping fails, the failure workflow triggers compensation. refund payment, then release inventory, in the correct reverse order. Every step is tracked, so you can see exactly where the order failed and which compensations ran.

### What You Write: Workers

ReserveInventoryWorker locks stock, ChargePaymentWorker processes the charge, and ShipOrderWorker dispatches the package, with ReleaseInventoryWorker and RefundPaymentWorker as compensating transactions that execute in reverse order when shipping fails.

| Worker | Task | What It Does |
|---|---|---|
| **ChargePaymentWorker** | `spi_charge_payment` | Worker for spi_charge_payment. Charges payment for an order. Produces a deterministic payment ID "PAY-001" and mark.. |
| **RefundPaymentWorker** | `spi_refund_payment` | Worker for spi_refund_payment. Compensation worker that refunds a payment. This is called as part of the saga compe.. |
| **ReleaseInventoryWorker** | `spi_release_inventory` | Worker for spi_release_inventory. Compensation worker that releases reserved inventory. This is called as part of t.. |
| **ReserveInventoryWorker** | `spi_reserve_inventory` | Worker for spi_reserve_inventory. Reserves inventory for an order. Produces a deterministic reservation ID "INV-001.. |
| **ShipOrderWorker** | `spi_ship_order` | Worker for spi_ship_order. Ships an order. If the input parameter "shouldFail" is true, the worker returns shipStat.. |

Workers implement success and failure scenarios so you can observe the resilience pattern end-to-end. Swap in real service calls and the retry, compensation, and recovery behavior works identically.

### The Workflow

```
spi_reserve_inventory
 │
 ▼
spi_charge_payment
 │
 ▼
spi_ship_order
 │
 ▼
SWITCH (check_ship_ref)
 ├── failed: spi_refund_payment -> spi_release_inventory

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
