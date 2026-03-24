# Implementing Data Masking in Java with Conductor : Field Identification, Strategy Selection, Masking Application, and Output Validation

## The Problem

You need to share production-like data with development teams, analytics pipelines, or third-party vendors; but the data contains Social Security numbers, email addresses, credit card numbers, health records, and other regulated PII that cannot be exposed. Each field type requires a different masking approach: SSNs should be redacted or tokenized, emails need format-preserving pseudonymization so downstream systems still process them correctly, credit card numbers must preserve the BIN prefix for payment testing. After masking, you must verify that no PII leaked through (partial masks, edge cases, NULL handling) and that referential integrity is maintained (the same customer ID maps to the same masked ID across all tables).

Without orchestration, data masking is a brittle script that somebody wrote once and nobody maintains. It hardcodes which columns to mask, uses the same blunt approach (replace everything with `***`) regardless of the field type, and has no validation step. so developers discover that the masked data breaks their tests because foreign keys no longer match. When a new table with sensitive data is added, nobody updates the script. There is no audit trail showing what was masked, when, or by whom, making it impossible to prove compliance to regulators who require evidence that non-production environments contain only masked data.

## The Solution

**You just write the field detection and masking transformations. Conductor handles the strict field-identification-to-validation sequence, retries when data source connections drop, and a complete audit proving exactly which fields were masked, how, and whether validation passed.**

Each masking step is a simple, independent worker. one scans the data source to identify all sensitive fields, one selects the right masking strategy (tokenization for IDs, redaction for SSNs, format-preserving encryption for emails) based on the data's purpose, one applies the masks across all identified fields, one validates that the output contains no residual PII and that referential integrity is preserved across related tables. Conductor takes care of executing them in strict order so no data is released without validation, retrying if the data source connection drops mid-scan, and maintaining a complete audit trail proving exactly which fields were identified, what strategy was applied, and whether the validation passed, essential for demonstrating compliance with GDPR, CCPA, and HIPAA data minimization requirements.

### What You Write: Workers

The masking pipeline chains DmIdentifyFieldsWorker to discover sensitive fields, DmSelectStrategyWorker to choose the right technique (tokenization, redaction, or FPE), DmApplyMaskingWorker to transform the data, and DmValidateOutputWorker to verify no PII leaked and referential integrity is preserved.

| Worker | Task | What It Does |
|---|---|---|
| **DmIdentifyFieldsWorker** | `dm_identify_fields` | Scans the data source and identifies all sensitive fields. email columns, SSN fields, credit card numbers, phone numbers, health record identifiers, returning the field list with detected PII types |
| **DmSelectStrategyWorker** | `dm_select_strategy` | Selects the masking strategy for each field based on the data's intended purpose. tokenization for fields that need reversibility, redaction for SSNs, format-preserving encryption for emails used in testing |
| **DmApplyMaskingWorker** | `dm_apply_masking` | Applies the selected masking technique to every identified sensitive field. tokenizing IDs, redacting SSNs, pseudonymizing email addresses, while preserving data format and referential integrity |
| **DmValidateOutputWorker** | `dm_validate_output` | Verifies that no PII remains in the masked output by re-scanning for sensitive patterns, checks that referential integrity is maintained across related tables, and confirms the masked data is usable for its intended purpose |

the workflow logic stays the same.

### The Workflow

```
dm_identify_fields
 │
 ▼
dm_select_strategy
 │
 ▼
dm_apply_masking
 │
 ▼
dm_validate_output

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
