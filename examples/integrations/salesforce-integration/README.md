# Salesforce Integration

Orchestrates salesforce integration through a multi-stage Conductor workflow.

**Input:** `query`, `scoringModel`, `syncTarget` | **Timeout:** 60s

## Pipeline

```
sfc_query_leads
    │
sfc_score_leads
    │
sfc_update_records
    │
sfc_sync_crm
```

## Workers

**QueryLeadsWorker** (`sfc_query_leads`): Queries leads from Salesforce.

Reads `query`. Outputs `leads`, `totalCount`.

**ScoreLeadsWorker** (`sfc_score_leads`): Scores leads using a model.

Reads `leads`, `model`. Outputs `scoredLeads`, `scoredCount`.

**SyncCrmWorker** (`sfc_sync_crm`): Syncs records to a CRM target.

Reads `syncTarget`, `updatedCount`. Outputs `synced`, `syncedAt`.

**UpdateRecordsWorker** (`sfc_update_records`): Updates lead records in Salesforce.

Reads `scoredLeads`. Outputs `updatedCount`, `updatedAt`.

## Tests

**8 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
