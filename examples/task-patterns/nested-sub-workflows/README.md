# Nested Sub Workflows in Java with Conductor

Three-level nested order processing. order fulfillment (Level 1) delegates to a payment sub-workflow (Level 2), which delegates fraud checking to its own sub-workflow (Level 3).

## The Problem

You need to process an order through a three-level hierarchy: the order workflow (Level 1) calls a payment sub-workflow (Level 2), which itself calls a fraud check sub-workflow (Level 3). The order workflow takes orderId, amount, email, and items. It delegates payment processing to a separate reusable workflow that handles charging and fraud detection. The payment workflow in turn delegates fraud screening to its own sub-workflow that computes a risk score. After payment completes (with fraud check nested inside), the order workflow fulfills the order. Each level is a self-contained, independently testable, and reusable workflow.

Without orchestration, you'd call payment functions directly from order code, nesting fraud checks inside payment logic, creating tight coupling across all three layers. If the fraud check API changes, you modify code deep inside the order processor. There is no way to reuse the payment workflow from a different order flow, and there is no visibility into which level of the hierarchy failed when something goes wrong.

## The Solution

**You just write the fraud check, charge, and fulfillment workers. Conductor handles the three-level sub-workflow nesting, execution tracking, and independent retry at each level.**

This example demonstrates Conductor's SUB_WORKFLOW tasks nested three levels deep. The root `nested_order` workflow (Level 1) invokes a `nested_payment` sub-workflow (Level 2) via SUB_WORKFLOW task, passing orderId, amount, and email. The payment workflow invokes a `nested_fraud` sub-workflow (Level 3) for risk scoring via CheckFraudWorker, then runs ChargeWorker to process the payment. After the payment sub-workflow returns a transactionId, the root workflow runs FulfillWorker to complete the order. Each level is a standalone workflow definition. you can run the payment workflow independently for refunds, or reuse the fraud workflow from a different payment flow. Conductor tracks the full execution tree across all three levels.

### What You Write: Workers

Three workers handle the three-level order flow: CheckFraudWorker computes a risk score at Level 3, ChargeWorker processes the payment at Level 2, and FulfillWorker completes the order at Level 1, each running within its own reusable sub-workflow.

| Worker | Task | What It Does |
|---|---|---|
| **ChargeWorker** | `nest_charge` | Charges payment for an order. Returns a transaction ID and charged status. Used by Level 2 sub-workflow (nested_payme... |
| **CheckFraudWorker** | `nest_check_fraud` | Checks fraud risk for a transaction. Returns a deterministic risk score (25) and approval status. Used by the deepest... |
| **FulfillWorker** | `nest_fulfill` | Fulfills an order after payment is complete. Returns fulfillment status. Used by the root workflow (Level 1: nested_o... |

Workers implement their processing steps so you can see the pattern in action without external services. Replace the simulation with real processing logic. the task pattern and Conductor orchestration remain unchanged.

### The Workflow

```
Input -> ChargeWorker -> CheckFraudWorker -> FulfillWorker -> Output

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
