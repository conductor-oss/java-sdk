# Data Anonymization

A healthcare organization must share patient records with a research partner, but HIPAA requires removing direct identifiers and generalizing quasi-identifiers before export. Names must be truncated, ages bucketed into ranges, zip codes masked, and fields like `email`, `ssn`, and `phone` replaced with `[REDACTED]`. The final dataset needs a k-anonymity verification pass.

## Pipeline

```
[an_identify_pii]
     |
     v
[an_generalize_data]
     |
     v
[an_suppress_fields]
     |
     v
[an_verify_anonymization]
```

**Workflow inputs:** `dataset`, `anonymizationLevel`

## Workers

**GeneralizeDataWorker** (task: `an_generalize_data`)

Generalizes quasi-identifier and selected direct-identifier fields.

- Truncates strings to first 1 character(s), formats output strings, uses java streams, filters with predicates
- Reads `dataset`, `piiFields`. Writes `generalized`, `generalizedCount`

**IdentifyPiiWorker** (task: `an_identify_pii`)

Identifies PII fields in the dataset.

- Uses java streams
- Writes `piiFields`, `piiFieldCount`

**SuppressFieldsWorker** (task: `an_suppress_fields`)

Suppresses direct-identifier fields by replacing values with [REDACTED].

- `SUPPRESSED_FIELDS` = List.of("email", "ssn", "phone")
- Replaces values with `[redacted]`, formats output strings
- Reads `generalizedData`. Writes `suppressed`, `suppressedCount`

**VerifyAnonymizationWorker** (task: `an_verify_anonymization`)

Verifies that anonymization was applied correctly.

- Uses java streams, filters with predicates
- Reads `anonymizedData`, `originalPiiFields`. Writes `verified`, `kAnonymity`, `recordCount`

---

**16 tests** | Workflow: `data_anonymization` | Timeout: 60s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
