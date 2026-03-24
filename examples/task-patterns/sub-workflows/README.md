# Sub-Workflows

An order processing system delegates payment handling to a sub-workflow. The main workflow calculates the total, invokes a `SUB_WORKFLOW` for payment (validate then charge), and confirms the order after payment succeeds.

## Workflow

```
sub_calc_total ──> payment_sub_workflow (SUB_WORKFLOW) ──> sub_confirm_order
```

Workflow `sub_order_workflow` accepts `orderId`, `items`, and `paymentMethod`. Times out after `120` seconds.

## Workers

**CalcTotalWorker** (`sub_calc_total`) -- calculates the order total from the item list.

**ValidatePaymentWorker** (`sub_validate_payment`) -- validates the payment method in the sub-workflow.

**ChargePaymentWorker** (`sub_charge_payment`) -- charges the validated amount.

**ConfirmOrderWorker** (`sub_confirm_order`) -- confirms the order after payment completes.

## Workflow Output

The workflow produces `orderId`, `transactionId`, `confirmed` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Task Configuration

- `sub_calc_total`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30
- `sub_validate_payment`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30
- `sub_charge_payment`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30
- `sub_confirm_order`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30

These settings are declared in `task-defs.json` and apply independently to each task, controlling retry behavior, timeout detection, and backoff strategy without any changes to worker code.

## Project Structure

This example contains 4 worker implementations in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `sub_order_workflow` defines 3 tasks with input parameters `orderId`, `items`, `paymentMethod` and a timeout of `120` seconds.

## Tests

6 tests verify total calculation, sub-workflow invocation, payment validation and charging, and order confirmation.

## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
