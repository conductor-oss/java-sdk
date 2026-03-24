# Data Anonymization in Java Using Conductor : PII Detection, Generalization, Suppression, and k-Anonymity Verification

## The Problem

You need to share or analyze datasets that contain personal information. Patient records for research, customer data for analytics, employee records for benchmarking. Regulations like GDPR, HIPAA, and CCPA require you to anonymize this data before it leaves controlled environments. That means scanning every field to identify PII (names, emails, SSNs, phone numbers, addresses), generalizing quasi-identifiers so individuals can't be re-identified (replacing exact ages with ranges, truncating zip codes), suppressing direct identifiers entirely (replacing names and SSNs with `[REDACTED]`), and verifying the result meets a k-anonymity threshold so no individual can be singled out from the anonymized dataset.

Without orchestration, you'd write a single anonymization script that scans fields, applies transformations inline, and hopes everything was covered. If the PII scanner misclassifies a field, downstream suppressions silently fail. There's no audit trail showing which fields were identified, which were generalized vs: suppressed, and whether the final dataset actually meets the required anonymization level. Adding a new anonymization technique (differential privacy, pseudonymization) means rewriting tightly coupled code.

## The Solution

**You just write the PII detection, generalization, suppression, and verification workers. Conductor handles strict sequencing from PII detection through verification, audit-grade tracking of every transformation decision, and retries when external classification services are unavailable.**

Each stage of the anonymization pipeline is a simple, independent worker. The PII identifier scans the dataset and classifies fields as direct identifiers, quasi-identifiers, or safe. The generalizer applies range-based transformations to quasi-identifiers (ages become ranges, zip codes become prefixes). The suppressor replaces direct identifiers with `[REDACTED]` at the configured anonymization level. The verifier checks that no PII leaks through and computes the k-anonymity score of the output dataset. Conductor executes them in sequence, passes the evolving dataset between steps, retries if a scan fails, and tracks every transformation decision with full audit visibility.

### What You Write: Workers

Four workers implement the anonymization pipeline: scanning for PII fields, generalizing quasi-identifiers like age and zip code, suppressing direct identifiers with redaction, and verifying k-anonymity compliance.

| Worker | Task | What It Does |
|---|---|---|
| **GeneralizeDataWorker** | `an_generalize_data` | Generalizes quasi-identifier and selected direct-identifier fields. |
| **IdentifyPiiWorker** | `an_identify_pii` | Identifies PII fields in the dataset. |
| **SuppressFieldsWorker** | `an_suppress_fields` | Suppresses direct-identifier fields by replacing values with [REDACTED]. |
| **VerifyAnonymizationWorker** | `an_verify_anonymization` | Verifies that anonymization was applied correctly. |

Workers implement data processing stages with representative outputs so the pipeline runs end-to-end without external data stores. Swap in real data sources and sinks. the pipeline structure and error handling stay the same.

### The Workflow

```
an_identify_pii
 │
 ▼
an_generalize_data
 │
 ▼
an_suppress_fields
 │
 ▼
an_verify_anonymization

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
