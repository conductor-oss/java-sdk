# Switch Default Case in Java with Conductor

Fallback routing for unmatched payment methods using SWITCH with defaultCase. ## The Problem

You need to route payment processing based on the payment method. credit card goes to Stripe, bank transfer goes to Plaid, crypto goes to Coinbase. But customers sometimes submit unrecognized payment methods (PayPal, Apple Pay, "cash") that don't match any configured processor. Those unmatched methods need a fallback path that flags them for manual review rather than silently failing or throwing an exception. After processing (or flagging), every payment attempt must be logged regardless of which branch was taken.

Without orchestration, you'd write an if/else chain or switch statement with a catch-all else clause, but there is no record of which branch executed for a given payment. If you add a new payment method (e.g., "digital_wallet"), you modify the routing code and hope the default case still works. When a customer complains that their payment failed, debugging whether it hit the crypto branch or the default case requires searching through application logs.

## The Solution

**You just write the per-method payment processing and logging workers. Conductor handles the routing, default-case fallback, and branch tracking.**

This example demonstrates Conductor's SWITCH task with a `defaultCase` for handling unmatched values. The SWITCH routes on `paymentMethod`: `credit_card` goes to ProcessCardWorker (Stripe), `bank_transfer` goes to ProcessBankWorker (Plaid), `crypto` goes to ProcessCryptoWorker (Coinbase). Any unrecognized method falls through to the `defaultCase`, where UnknownMethodWorker flags it for manual review. After the SWITCH resolves. regardless of which branch ran. LogWorker records the payment attempt. Conductor records exactly which branch executed, so you can see that `paymentMethod=paypal` hit the default case and was flagged for review.

### What You Write: Workers

Five workers cover payment routing: ProcessCardWorker handles credit cards via Stripe, ProcessBankWorker handles bank transfers via Plaid, ProcessCryptoWorker handles crypto via Coinbase, UnknownMethodWorker flags unrecognized methods for review, and LogWorker records every payment attempt regardless of branch.

| Worker | Task | What It Does |
|---|---|---|
| **LogWorker** | `dc_log` | Logs the payment processing action. Runs after the SWITCH for all cases. |
| **ProcessBankWorker** | `dc_process_bank` | Processes bank transfer payments via Plaid. |
| **ProcessCardWorker** | `dc_process_card` | Processes credit card payments via Stripe. |
| **ProcessCryptoWorker** | `dc_process_crypto` | Processes crypto payments via Coinbase. |
| **UnknownMethodWorker** | `dc_unknown_method` | Handles unrecognized payment methods (default case). Takes the method name and flags it for manual review. |

Workers implement their processing steps so you can see the pattern in action without external services. Replace the simulation with real processing logic. the task pattern and Conductor orchestration remain unchanged.

### The Workflow

```
SWITCH (route_ref)
 ├── credit_card: dc_process_card
 ├── bank_transfer: dc_process_bank
 ├── crypto: dc_process_crypto
 └── default: dc_unknown_method
 │
 ▼
dc_log

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
