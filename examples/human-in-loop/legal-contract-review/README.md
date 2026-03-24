# Legal Contract Review

Legal Contract Review -- AI extracts key terms, waits for human legal review, then finalizes.

**Input:** `contractId` | **Timeout:** 300s

**Output:** `keyTerms`, `riskFlags`, `finalized`

## Pipeline

```
lcr_extract_terms
    │
legal_review [WAIT]
    │
lcr_finalize
```

## Workers

**LcrExtractTermsWorker** (`lcr_extract_terms`): Extracts key terms from a contract. Real risk analysis.

Reads `contractText`, `duration`, `liability`. Outputs `keyTerms`, `riskFlags`, `riskLevel`.

**LcrFinalizeWorker** (`lcr_finalize`): Worker for lcr_finalize task -- finalizes the legal contract review.

- finalized: true
- contractId: the contract identifier
- riskFlagCount: number of risk flags from the extraction step

Reads `contractId`, `riskFlags`. Outputs `finalized`, `contractId`, `riskFlagCount`.

## Workflow Output

- `keyTerms`: `${lcr_extract_terms_ref.output.keyTerms}`
- `riskFlags`: `${lcr_extract_terms_ref.output.riskFlags}`
- `finalized`: `${lcr_finalize_ref.output.finalized}`

## Data Flow

**lcr_extract_terms**: `contractId` = `${workflow.input.contractId}`
**legal_review** [WAIT]: `keyTerms` = `${lcr_extract_terms_ref.output.keyTerms}`, `riskFlags` = `${lcr_extract_terms_ref.output.riskFlags}`
**lcr_finalize**: `contractId` = `${workflow.input.contractId}`, `keyTerms` = `${lcr_extract_terms_ref.output.keyTerms}`, `riskFlags` = `${lcr_extract_terms_ref.output.riskFlags}`

## Tests

**4 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
