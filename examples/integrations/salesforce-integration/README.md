# Salesforce Integration in Java Using Conductor

## Scoring and Syncing Salesforce Leads

Lead scoring in Salesforce involves a strict sequence: query the leads you want to score, run them through a scoring model, write the scores back to Salesforce, and sync the results to downstream systems. Each step depends on the previous one. you cannot score leads you have not queried, and you cannot sync records you have not updated. If the scoring model fails or the update step partially succeeds, you need visibility into exactly what happened.

Without orchestration, you would chain Salesforce REST API calls manually, manage lead lists and score maps between steps, and handle partial update failures yourself. Conductor sequences the pipeline and tracks lead counts, scores, and sync status between workers automatically.

## The Solution

**You just write the Salesforce workers. Lead querying, scoring, record updating, and CRM syncing. Conductor handles query-to-sync sequencing, Salesforce API retries, and lead count tracking across scoring and update stages.**

Each worker integrates with one external system. Conductor manages the integration sequence, retry logic, timeout handling, and data transformation between systems.

### What You Write: Workers

Four workers run the lead scoring pipeline: QueryLeadsWorker fetches leads via SOQL, ScoreLeadsWorker applies the predictive model, UpdateRecordsWorker writes scores back to Salesforce, and SyncCrmWorker propagates results to downstream systems.

| Worker | Task | What It Does |
|---|---|---|
| **QueryLeadsWorker** | `sfc_query_leads` | Queries leads from Salesforce. |
| **ScoreLeadsWorker** | `sfc_score_leads` | Scores leads using a model. |
| **SyncCrmWorker** | `sfc_sync_crm` | Syncs records to a CRM target. |
| **UpdateRecordsWorker** | `sfc_update_records` | Updates lead records in Salesforce. |

the workflow orchestration and error handling stay the same.

### The Workflow

```
sfc_query_leads
 │
 ▼
sfc_score_leads
 │
 ▼
sfc_update_records
 │
 ▼
sfc_sync_crm

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
