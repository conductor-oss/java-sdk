# Procurement Workflow

Procurement workflow: requisition, approve, purchase, receive goods, and process payment.

**Input:** `item`, `quantity`, `budget`, `requester` | **Timeout:** 60s

## Pipeline

```
prw_requisition
    │
prw_approve
    │
prw_purchase
    │
prw_receive
    │
prw_pay
```

## Workers

**ApproveWorker** (`prw_approve`): Approves or rejects a requisition based on budget thresholds.

- `estimatedCost > 50000` &rarr; `"CFO"`
- `estimatedCost > 10000` &rarr; `"VP-Operations"`

```java
boolean withinBudget = estimatedCost <= budget;
```

Reads `budget`, `estimatedCost`. Outputs `approved`, `approver`, `approvedAmount`.

**PayWorker** (`prw_pay`): Processes payment for a purchase order. Real payment ID generation.

```java
boolean paid = amount > 0 && poNumber != null && !poNumber.equals("NONE");
```

Reads `amount`, `poNumber`. Outputs `paymentId`, `paid`, `paidAt`.

**PurchaseWorker** (`prw_purchase`): Creates a purchase order. Real PO generation with unique numbers.

```java
boolean approved = Boolean.TRUE.equals(approvedObj);
```

Reads `approved`, `approvedAmount`, `requisitionId`. Outputs `poNumber`, `totalCost`, `createdAt`.

**ReceiveWorker** (`prw_receive`): Confirms goods receipt with quality inspection.

```java
String condition = received ? "good" : "not_received";
```

Reads `poNumber`. Outputs `received`, `condition`, `receivedAt`.

**RequisitionWorker** (`prw_requisition`): Creates a purchase requisition. Real cost estimation based on item and quantity.

```java
double unitPrice = 10.0 + Math.abs(item.hashCode() % 990);
double estimatedCost = quantity * unitPrice;
```

Reads `item`, `quantity`, `requester`. Outputs `requisitionId`, `estimatedCost`, `unitPrice`, `quantity`.

## Tests

**5 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
