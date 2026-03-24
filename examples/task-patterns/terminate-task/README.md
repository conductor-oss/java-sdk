# Terminate Task in Java with Conductor

Early exit with TERMINATE based on validation.

## The Problem

You need to validate an order before processing it. If the amount is invalid, the currency is unsupported, or the order exceeds limits, the workflow should stop immediately with a FAILED status and a clear error message. The TERMINATE task provides a clean early exit: the validation worker checks constraints, a SWITCH routes invalid orders to TERMINATE (which ends the workflow), and valid orders continue to the processing worker.

Without orchestration, you'd use exceptions or return codes to short-circuit processing, but there's no standard way to mark the workflow as failed with a specific reason. TERMINATE gives you a declarative early exit with proper status tracking, every terminated workflow shows exactly why it stopped.

## The Solution

**You just write the order validation and processing workers. Conductor handles the conditional routing and TERMINATE-based early exit when validation fails.**

This example validates an order before processing it, using a TERMINATE task to short-circuit the workflow when validation fails. ValidateWorker checks the order's amount (must be positive and under $1,000,000) and currency (must be USD, EUR, or GBP), returning `{ valid: true/false, reason: "..." }`. A SWITCH task inspects the validation result. If `valid` is false, the workflow routes to a TERMINATE task that ends the execution with FAILED status and the validation error message. If valid, the workflow continues to ProcessWorker, which processes the order and returns `{ processedAmount }`. Every terminated workflow shows exactly why it was rejected in Conductor's execution history.

### What You Write: Workers

Two workers support the validate-then-process pattern: ValidateWorker checks order constraints (amount range, supported currency) and returns a valid/invalid verdict, while ProcessWorker handles the order only if validation passed, the TERMINATE early exit for invalid orders is handled entirely by Conductor.

| Worker | Task | What It Does |
|---|---|---|
| **ProcessWorker** | `term_process` | Processes a validated order. Only reached if validation passed. Takes orderId and amount, returns processedAmount. |
| **ValidateWorker** | `term_validate` | Validates an order by checking amount and currency constraints. Validation rules: - Amount must be positive (greater ... |

Workers implement their processing steps so you can see the pattern in action without external services. Replace the simulation with real processing logic. the task pattern and Conductor orchestration remain unchanged.

### The Workflow

```
term_validate
 │
 ▼
SWITCH (check_ref)
 ├── reject: terminate_invalid
 │
 ▼
term_process

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
