# Payment Reconciliation in Java with Conductor

Reconcile payments: match transactions, identify discrepancies, resolve mismatches, and generate report. ## The Problem

You need to reconcile payments between your internal records and bank/processor statements. The workflow matches transactions from both sides, identifies discrepancies (missing transactions, amount mismatches, duplicate entries), resolves mismatches through investigation or adjustment, and generates a reconciliation report. Unreconciled payments mean your books are inaccurate; unresolved discrepancies accumulate and become harder to fix over time.

Without orchestration, you'd run a batch reconciliation script that pulls transactions from your database and bank feeds, runs matching algorithms, generates exception reports, and manually investigates discrepancies. handling format differences between payment processors, retrying failed bank API calls, and maintaining reconciliation state across daily/weekly cycles.

## The Solution

**You just write the reconciliation workers. Transaction matching, discrepancy identification, and mismatch resolution. Conductor handles step ordering, automatic retries when the bank feed API is unavailable, and complete reconciliation cycle tracking.**

Each reconciliation concern is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of executing them in order (match, identify discrepancies, resolve, report), retrying if the bank feed API is unavailable, tracking every reconciliation cycle with full detail, and resuming from the last step if the process crashes. ### What You Write: Workers

Three workers handle the reconciliation process: MatchTransactionsWorker compares internal records against bank statements, IdentifyDiscrepanciesWorker flags mismatches and missing entries, and ResolveMismatchesWorker investigates and adjusts discrepancies.

| Worker | Task | What It Does |
|---|---|---|
| **IdentifyDiscrepanciesWorker** | `prc_identify_discrepancies` | Identifies discrepancies from unmatched transactions. |
| **MatchTransactionsWorker** | `prc_match_transactions` | Matches transactions against records for reconciliation. |
| **ResolveMismatchesWorker** | `prc_resolve_mismatches` | Resolves identified mismatches/discrepancies. |

Replace with real financial system integrations and the workflow, audit trail, and compliance logic stay the same.

### The Workflow

```
prc_match_transactions
 │
 ▼
prc_identify_discrepancies
 │
 ▼
prc_resolve_mismatches
 │
 ▼
prc_generate_report

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
