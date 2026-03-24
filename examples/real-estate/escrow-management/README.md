# Real Estate Escrow Management in Java with Conductor : Open, Deposit, Verify, Release, and Close

## The Problem

You need to manage escrow for property transactions. When a buyer and seller agree on a sale, earnest money must be deposited into a neutral escrow account. Before funds can be released, contingencies must be verified. title is clear, inspection passed, financing is approved. Only after verification should funds be released to the seller, and then the escrow must be formally closed with all parties notified. If any step executes out of order, funds released before verification, escrow closed before release, you face legal liability and financial loss.

Without orchestration, escrow management is tracked manually with phone calls, emails, and checklists. The title company calls to confirm the deposit, the agent emails to confirm verification, and a paralegal manually triggers the release. A missed step means funds are stuck or released prematurely, and reconstructing what happened requires digging through email threads.

## The Solution

**You just write the escrow opening, deposit handling, contingency verification, fund release, and closing logic. Conductor handles deposit retries, condition tracking, and escrow audit trails.**

Each escrow step is a simple, independent worker. one opens the account, one accepts the deposit, one verifies contingencies, one releases funds, one closes the escrow. Conductor ensures strict sequential execution so funds are never released before verification, retries if the banking API is temporarily down, and maintains a tamper-proof record of every step for regulatory compliance.

### What You Write: Workers

Deposit collection, document verification, condition tracking, and fund disbursement workers each manage one phase of the escrow process.

| Worker | Task | What It Does |
|---|---|---|
| **OpenEscrowWorker** | `esc_open` | Creates a new escrow account for the buyer/seller pair and returns the escrow ID |
| **DepositWorker** | `esc_deposit` | Records the earnest money deposit into the escrow account and confirms receipt |
| **VerifyWorker** | `esc_verify` | Checks that all closing contingencies are satisfied (title clear, inspection, financing) |
| **ReleaseWorker** | `esc_release` | Releases escrowed funds to the seller after verification is complete |
| **CloseEscrowWorker** | `esc_close` | Formally closes the escrow account and generates the closing statement |

Workers implement property transaction steps. listing, inspection, escrow, closing, with realistic outputs.

### The Workflow

```
esc_open
 │
 ▼
esc_deposit
 │
 ▼
esc_verify
 │
 ▼
esc_release
 │
 ▼
esc_close

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
