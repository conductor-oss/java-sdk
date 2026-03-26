# Ehr Integration

EHR Integration: query patient records, merge, validate, update master record

**Input:** `patientId`, `sourceSystem` | **Timeout:** 60s

## Pipeline

```
ehr_query_patient
    │
ehr_merge_records
    │
ehr_validate
    │
ehr_update
```

## Workers

**MergeRecordsWorker** (`ehr_merge_records`)

Reads `records`. Outputs `mergedRecord`, `sourceCount`, `conflictsResolved`.

**QueryPatientWorker** (`ehr_query_patient`)

Reads `patientId`, `sourceSystem`. Outputs `records`.

**UpdateRecordWorker** (`ehr_update`)

Reads `patientId`. Outputs `updated`, `updatedAt`, `version`.

**ValidateRecordWorker** (`ehr_validate`)

Reads `mergedRecord`. Outputs `passed`, `validatedRecord`, `validationErrors`.

## Tests

**8 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
