# Sub-Workflows in Java with Conductor

SUB_WORKFLOW demo: an order processing workflow that delegates payment handling to a reusable child workflow. The parent calculates the order total, invokes a payment sub-workflow (validate + charge), then confirms the order with the returned transaction ID. You write the business logic, Conductor handles retries, failure routing, durability, and observability.

## The Problem

You need to process an order through three phases: calculate the order total from line items (summing price * quantity for each item), process the payment (validate payment details, then charge the card), and confirm the order. Payment processing is a reusable multi-step process: validate the payment method and amount, then charge it, that should be a self-contained unit you can invoke from any order workflow, refund workflow, or subscription workflow. Embedding payment logic directly in the order workflow makes it impossible to reuse, test, or version independently.

Without orchestration, you'd call payment functions directly from the order code, tightly coupling order processing to payment logic. Changing the payment flow (adding fraud checks, supporting new payment methods) requires modifying the order workflow. Testing payment logic means running the full order flow. If the charge fails after validation succeeds, there is no clean boundary for retrying just the payment portion.

## The Solution

**You just write the order calculation, payment validation, charge, and confirmation workers. Conductor handles the parent-child workflow composition via SUB_WORKFLOW.**

This example demonstrates Conductor's SUB_WORKFLOW task for composable workflow design. The parent `sub_order_workflow` calculates the order total via CalcTotalWorker (summing price * qty for each line item), then delegates payment processing to a `sub_payment_process` child workflow via SUB_WORKFLOW task, passing the orderId, computed total, and payment method. The child workflow runs ValidatePaymentWorker (checks method is present and amount is positive) then ChargePaymentWorker (processes the charge and returns a deterministic transactionId of "TXN-" + orderId). After the sub-workflow completes, the parent runs ConfirmOrderWorker with the transactionId from the payment result, returning the final order confirmation. The payment sub-workflow can be versioned, tested, and invoked independently, from refund flows, subscription renewals, or any other workflow that needs payment processing.

### What You Write: Workers

Four workers span the parent and child workflows: CalcTotalWorker computes the order total in the parent, ValidatePaymentWorker and ChargePaymentWorker run inside the reusable payment sub-workflow, and ConfirmOrderWorker finalizes the order with the transaction ID returned from the child.

| Worker | Task | What It Does |
|---|---|---|
| **CalcTotalWorker** | `sub_calc_total` | Calculates the order total from a list of items. Each item has name, price, and qty. Returns total = sum of (price * qty) and itemCount. Returns 0.0/0 for null or empty items. |
| **ValidatePaymentWorker** | `sub_validate_payment` | Validates payment details: checks that paymentMethod is present and non-blank, and that amount is present and positive. Returns valid=true/false with a descriptive reason string. |
| **ChargePaymentWorker** | `sub_charge_payment` | Charges payment and returns a deterministic transactionId: "TXN-" + orderId. Returns charged=true and the amount. Defaults orderId to "UNKNOWN" if missing/blank. |
| **ConfirmOrderWorker** | `sub_confirm_order` | Confirms the order after payment: returns the orderId, transactionId, and confirmed=true. Defaults orderId to "UNKNOWN" and transactionId to "NONE" if missing/blank. |

Workers implement their processing steps so you can see the pattern in action without external services. Replace the simulation with real processing logic, the task pattern and Conductor orchestration remain unchanged.

### The Workflow

```
Parent: sub_order_workflow
 sub_calc_total
 │
 ▼
 SUB_WORKFLOW (sub_payment_process)
 │ ├── sub_validate_payment
 │ └── sub_charge_payment
 ▼
 sub_confirm_order

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
