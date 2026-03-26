# Trade Execution

Trade execution workflow that validates orders, checks compliance, routes to optimal exchange, executes, and confirms.

**Input:** `orderId`, `accountId`, `symbol`, `side`, `quantity`, `orderType` | **Timeout:** 60s

## Pipeline

```
trd_validate_order
    │
trd_check_compliance
    │
trd_route
    │
trd_execute
    │
trd_confirm
```

## Workers

**CheckComplianceWorker** (`trd_check_compliance`): Checks regulatory compliance for the trade.

Reads `symbol`. Outputs `compliant`, `checks`, `allPassed`.

**ConfirmWorker** (`trd_confirm`): Sends trade confirmation to the client.

Reads `fillPrice`, `orderId`. Outputs `confirmed`, `confirmationId`, `settlesOn`.

**ExecuteWorker** (`trd_execute`): Executes the trade on the routed exchange.

```java
double totalValue = Math.round(fillPrice * quantity * 100.0) / 100.0;
```

Reads `exchange`, `quantity`, `side`, `symbol`. Outputs `executionId`, `fillPrice`, `totalValue`, `executedAt`.

**RouteWorker** (`trd_route`): Routes the trade to the optimal exchange for best execution.

Outputs `exchange`, `latencyMs`, `nbboSpread`.

**ValidateOrderWorker** (`trd_validate_order`): Validates a trade order for required fields and buying power.

Reads `orderId`, `quantity`, `side`, `symbol`. Outputs `valid`, `buyingPower`, `sufficientFunds`.

## Tests

**9 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
