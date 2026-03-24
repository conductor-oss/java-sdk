# Two-Phase Commit: Order, Inventory, and Payment in Parallel

An order must reserve inventory and authorize payment atomically -- if payment fails after
inventory is reserved, you have phantom stock. This workflow uses FORK_JOIN to prepare all
three participants (order, inventory, payment) in parallel, then either commits all or rolls
back all based on whether every participant voted "prepared."

## Workflow

```
orderId, items, paymentMethod
              |
              v
    FORK_JOIN (prepare phase) --------+
    |              |                  |
    v              v                  v
+-----------------+ +-----------------+ +------------------+
| dtx_prepare_    | | dtx_prepare_    | | dtx_prepare_     |
| order           | | inventory       | | payment          |
+-----------------+ +-----------------+ +------------------+
  txId: OTX-...      txId: ITX-...       txId: PTX-...
  prepared: true     prepared: true      prepared: true
    |              |                  |
    +------ JOIN --+------------------+
              |
              v
     +-------------------+
     | dtx_commit_all    |   committed: true, globalTxId: GTX-...
     +-------------------+
         (or dtx_rollback_all if any prepare fails)
```

## Workers

**PrepareOrderWorker** -- Prepares the order. Returns `txId: "OTX-{timestamp}"`,
`prepared: true`.

**PrepareInventoryWorker** -- Reserves inventory items. Returns `txId: "ITX-{timestamp}"`,
`prepared: true`.

**PreparePaymentWorker** -- Authorizes the payment method. Returns `txId: "PTX-{timestamp}"`,
`prepared: true`.

**CommitAllWorker** -- Commits all 3 transactions. Returns `committed: true`,
`globalTxId: "GTX-{timestamp}"`.

**RollbackAllWorker** -- Rolls back all transactions if any prepare failed. Returns
`rolledBack: true`.

## Tests

10 unit tests cover order/inventory/payment preparation, commit, and rollback.

## Running

See [../../RUNNING.md](../../RUNNING.md) for setup and execution instructions.
