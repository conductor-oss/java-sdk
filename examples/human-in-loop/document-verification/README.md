# Document Verification

Document Verification -- AI extracts data from document, human verifies, then store verified data.

**Input:** `documentId` | **Timeout:** 300s

**Output:** `extracted`, `confidence`, `verifiedData`, `stored`

## Pipeline

```
dv_ai_extract
    │
dv_human_verify [WAIT]
    │
dv_store_verified
```

## Workers

**AiExtractWorker** (`dv_ai_extract`): Worker for dv_ai_extract task -- performs AI extraction of data from a document.

- name, dateOfBirth, documentNumber, expiryDate, address
- confidence score of 0.92

Outputs `extracted`, `confidence`.

**StoreVerifiedWorker** (`dv_store_verified`): Worker for dv_store_verified task -- stores the verified document data.

Outputs `stored`.

## Workflow Output

- `extracted`: `${dv_ai_extract_ref.output.extracted}`
- `confidence`: `${dv_ai_extract_ref.output.confidence}`
- `verifiedData`: `${dv_human_verify_ref.output.verifiedData}`
- `stored`: `${dv_store_verified_ref.output.stored}`

## Data Flow

**dv_ai_extract**: `documentId` = `${workflow.input.documentId}`
**dv_human_verify** [WAIT]: `extracted` = `${dv_ai_extract_ref.output.extracted}`, `confidence` = `${dv_ai_extract_ref.output.confidence}`
**dv_store_verified**: `verifiedData` = `${dv_human_verify_ref.output.verifiedData}`

## Tests

**14 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
