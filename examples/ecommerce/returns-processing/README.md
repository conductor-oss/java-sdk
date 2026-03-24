# Returns Processing

Returns processing workflow with SWITCH: receive, inspect, then refund/exchange/reject

**Input:** `orderId`, `returnReason`, `items`, `customerId` | **Timeout:** 60s

## Pipeline

```
ret_receive
    │
ret_inspect
    │
return_decision [SWITCH]
  ├─ refund: ret_refund
  ├─ exchange: ret_exchange
  └─ reject: ret_reject
```

## Workers

**ExchangeWorker** (`ret_exchange`)

Reads `returnId`. Outputs `exchangeOrderId`, `exchanged`.

**InspectReturnWorker** (`ret_inspect`)

Reads `returnId`, `returnReason`. Outputs `condition`, `decision`, `refundAmount`, `rejectReason`.

**ReceiveReturnWorker** (`ret_receive`)

Reads `items`, `orderId`. Outputs `returnId`, `receivedAt`, `itemCount`.

**RefundWorker** (`ret_refund`)

Reads `amount`, `customerId`, `returnId`. Outputs `refundId`, `refunded`, `amount`.

**RejectWorker** (`ret_reject`)

Reads `reason`, `returnId`. Outputs `rejected`, `reason`.

## Tests

**12 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
