# Gdpr Data Deletion

A user submits a GDPR Article 17 erasure request. The platform must locate every record associated with that user's ID across multiple data stores, delete or anonymize each one, verify that no residual PII remains, and generate a compliance certificate with a timestamp and record count -- all within the 30-day regulatory window.

## Pipeline

```
[gr_find_records]
     |
     v
[gr_verify_identity]
     |
     v
[gr_delete_data]
     |
     v
[gr_generate_audit_log]
```

**Workflow inputs:** `userId`, `requestId`

## Workers

**DeleteDataWorker** (task: `gr_delete_data`)

Deletes user data from all systems if identity is verified.

- Sets `status` = `"deleted"`
- Reads `verified`. Writes `deletedCount`, `deletedRecords`, `timestamp`

**FindRecordsWorker** (task: `gr_find_records`)

Finds all records associated with a user across multiple systems.

- Formats output strings
- Writes `records`, `recordCount`, `systems`

**GenerateAuditLogWorker** (task: `gr_generate_audit_log`)

Generates a GDPR-compliant audit log for the deletion request.

- Writes `auditId`, `status`, `retentionDays`, `complianceStandard`

**VerifyIdentityWorker** (task: `gr_verify_identity`)

Verifies the identity of the user requesting data deletion.

- Sets `method` = `"token_verification"`
- Writes `verified`, `method`

---

**27 tests** | Workflow: `gdpr_data_deletion` | Timeout: 60s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
