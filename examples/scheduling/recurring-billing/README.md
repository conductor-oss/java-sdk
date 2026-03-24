# Recurring Billing

A subscription billing cycle runs monthly. The pipeline fetches the subscription details, calculates charges for the billing period, generates an invoice, processes payment, and sends a billing notification to the customer.

## Workflow

```
billing_fetch_subscription ──> billing_calculate_charges ──> billing_generate_invoice ──> billing_process_payment ──> billing_send_notification
```

Workflow `recurring_billing` accepts `customerId`, `planId`, `billingCycleStart`, `billingCycleEnd`, and `subscriptionStart`. Times out after `300` seconds.

## Workers

**FetchSubscription** (`billing_fetch_subscription`) -- fetches subscription details for the customer and plan.

**CalculateCharges** (`billing_calculate_charges`) -- calculates charges for the billing period.

**GenerateInvoice** (`billing_generate_invoice`) -- generates the invoice from calculated charges.

**ProcessPayment** (`billing_process_payment`) -- processes the payment against the invoice.

**SendBillingNotification** (`billing_send_notification`) -- sends the billing notification to the customer.

## Workflow Output

The workflow produces `invoice`, `payment`, `charges`, `notification` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Task Configuration

- `billing_fetch_subscription`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30
- `billing_calculate_charges`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30
- `billing_process_payment`: retryCount=3, retryLogic=FIXED, retryDelaySeconds=5, timeoutSeconds=60, responseTimeoutSeconds=30
- `billing_generate_invoice`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30
- `billing_send_notification`: retryCount=3, retryLogic=FIXED, retryDelaySeconds=2, timeoutSeconds=60, responseTimeoutSeconds=30

These settings are declared in `task-defs.json` and apply independently to each task, controlling retry behavior, timeout detection, and backoff strategy without any changes to worker code.

## Project Structure

This example contains 5 worker implementations in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `recurring_billing` defines 5 tasks with input parameters `customerId`, `planId`, `billingCycleStart`, `billingCycleEnd`, `subscriptionStart` and a timeout of `300` seconds.

## Tests

13 tests verify subscription fetching, charge calculation, invoice generation, payment processing, and notification delivery.

## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
