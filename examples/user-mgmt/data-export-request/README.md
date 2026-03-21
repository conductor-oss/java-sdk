# Data Export Request in Java Using Conductor

## The Problem

You need to fulfill a user's data export request. Validating the request parameters and user identity, collecting their data across multiple categories (profile, activity, preferences, uploaded content), packaging everything into the requested format (JSON, CSV, ZIP), and delivering a secure download link to the user. Each step depends on the previous one's output.

If the collection step misses a data category, the export is incomplete and you fail GDPR Article 20 portability requirements. If packaging succeeds but delivery fails, the user's data sits in a temporary location with no notification that it's ready. Without orchestration, you'd build a monolithic export handler that mixes request validation, cross-service data queries, file packaging, and email delivery. Making it impossible to add new data sources, support additional export formats, or track which categories were included in which export for compliance auditing.

## The Solution

**You just write the request-validation, data-collection, packaging, and delivery workers. Conductor handles the export pipeline and cross-service data aggregation.**

ValidateExportWorker verifies the user's identity and checks that the requested export format (JSON, CSV) is supported and the data categories are valid. CollectDataWorker queries the user's data across all requested categories: profile information, activity logs, stored files, preferences, aggregating records from multiple services and databases. PackageDataWorker assembles the collected data into the requested format, compresses it into a downloadable archive, and uploads it to secure storage with a time-limited access URL. DeliverExportWorker sends the user a notification with the download link, recording that the export was successfully delivered. Each worker is a standalone Java class. Conductor handles the sequencing, retries, and crash recovery.

### What You Write: Workers

ValidateExportWorker verifies identity and format, CollectDataWorker aggregates profile and activity records, PackageDataWorker creates a downloadable archive, and DeliverExportWorker sends the expiring download link.

| Worker | Task | What It Does |
|---|---|---|
| **CollectDataWorker** | `der_collect` | Collects user data across requested categories (profile, activity, purchases), returning the total record count |
| **DeliverExportWorker** | `der_deliver` | Sends the export download link to the user with a 7-day expiration |
| **PackageDataWorker** | `der_package` | Packages the collected data into the requested format (JSON, CSV) and uploads it to a downloadable URL |
| **ValidateExportWorker** | `der_validate` | Validates the export request by verifying the user's identity and checking the requested format |

Replace with real identity provider and database calls and ### The Workflow

```
der_validate
 │
 ▼
der_collect
 │
 ▼
der_package
 │
 ▼
der_deliver

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
