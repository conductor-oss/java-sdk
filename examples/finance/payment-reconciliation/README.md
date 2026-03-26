# Payment Reconciliation

Reconcile payments: match transactions, identify discrepancies, resolve mismatches, and generate report.

**Input:** `batchId`, `accountId`, `periodStart`, `periodEnd` | **Timeout:** 60s

## Pipeline

```
prc_match_transactions
    │
prc_identify_discrepancies
    │
prc_resolve_mismatches
    │
prc_generate_report
```

## Workers

**GenerateReportWorker** (`prc_generate_report`): Generates a reconciliation report summarizing matched, resolved, and unresolved transactions.

Reads `accountId`, `batchId`, `matchedCount`, `resolvedCount`, `unresolvedCount`. Outputs `reportId`, `generatedAt`, `summary`.

**IdentifyDiscrepanciesWorker** (`prc_identify_discrepancies`): Identifies discrepancies from unmatched transactions.

Reads `unmatchedCount`. Outputs `discrepancies`, `totalDiscrepancyAmount`.

**MatchTransactionsWorker** (`prc_match_transactions`): Matches transactions against records for reconciliation.

Reads `batchId`. Outputs `matchedCount`, `unmatchedCount`, `totalAmount`, `unmatchedItems`.

**ResolveMismatchesWorker** (`prc_resolve_mismatches`): Resolves identified mismatches/discrepancies.

Reads `discrepancies`. Outputs `resolvedCount`, `unresolvedCount`, `resolutions`, `pendingReview`.

## Tests

**0 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
