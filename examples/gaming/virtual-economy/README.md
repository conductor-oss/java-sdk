# Virtual Economy in Java Using Conductor

Processes virtual economy transactions: recording the transaction, validating balance and ownership, updating player balances, creating an audit trail for fraud detection, and generating a settlement report.

## The Problem

You need to process a virtual economy transaction in your game. a purchase, sale, trade, or currency conversion. The transaction must be recorded, validated for sufficient balance and item ownership, the sender and receiver balances updated atomically, an audit record created for fraud detection, and a transaction report generated. Processing transactions without validation enables item duplication and currency exploits; missing audit records makes fraud investigation impossible.

Without orchestration, you'd handle transactions in a single database transaction with validation, balance updates, and audit logging. manually ensuring atomicity across player accounts, handling concurrent transactions on the same account, and maintaining economy health metrics.

## The Solution

**You just write the transaction recording, balance validation, currency transfer, audit logging, and settlement reporting logic. Conductor handles transaction validation retries, balance reconciliation, and economy audit trails.**

Each economy concern is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of executing them in order (transaction, validate, update balance, audit, report), retrying if the database is temporarily unavailable, tracking every transaction with full audit trail, and resuming from the last step if the process crashes.

### What You Write: Workers

Currency minting, transaction validation, balance updates, and audit logging workers each govern one aspect of the in-game economy.

| Worker | Task | What It Does |
|---|---|---|
| **AuditWorker** | `vec_audit` | Creates an audit log entry linking the transaction ID and resulting balance for fraud detection |
| **ReportWorker** | `vec_report` | Generates a settlement report with player ID, transaction ID, and final status |
| **TransactionWorker** | `vec_transaction` | Records the transaction request with type (earn, purchase, trade) and amount, and assigns a transaction ID |
| **UpdateBalanceWorker** | `vec_update_balance` | Updates the player's balance by adding the transaction amount and returns old and new balances |
| **ValidateWorker** | `vec_validate` | Validates the transaction by checking rate limits and value limits for the currency |

### The Workflow

```
vec_transaction
 │
 ▼
vec_validate
 │
 ▼
vec_update_balance
 │
 ▼
vec_audit
 │
 ▼
vec_report

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
