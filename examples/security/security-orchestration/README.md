# Implementing Security Orchestration (SOAR) in Java with Conductor : Alert Ingestion, Enrichment, Decision, and Playbook Execution

## The Problem

Your SOC receives hundreds of alerts daily from multiple sources. SIEM, EDR, IDS, cloud security tools. Each alert must be ingested, enriched with context (is this a known-bad IP? what asset is affected? who owns it?), triaged (true positive vs false positive, severity level), and responded to with an automated playbook (isolate host, block IP, reset credentials). Without automation, analysts manually investigate each alert, and most of their time is spent on enrichment rather than response.

Without orchestration, SOAR is a collection of scripts that run independently. Enrichment queries three different tools, the triage decision is in the analyst's head, and playbook execution is a separate runbook. There's no unified pipeline from alert to response, and no tracking of which alerts were handled and how.

## The Solution

**You just write the alert enrichment and response playbooks. Conductor handles the alert-to-playbook pipeline, retries when enrichment APIs are unavailable, and tracks mean time to respond for every incident from ingestion to resolution.**

Each SOAR step is an independent worker. alert ingestion, enrichment, triage decision, and playbook execution. Conductor runs them in sequence: ingest the alert, enrich with context, decide on action, then execute the playbook. Every alert is tracked from ingestion to resolution, you can measure mean time to respond and audit every automated action. ### What You Write: Workers

The SOAR pipeline chains IngestAlertWorker to receive alerts from SIEM/EDR sources, EnrichWorker to add threat intel and asset context, DecideActionWorker to triage and select the response, and ExecutePlaybookWorker to run automated containment actions.

| Worker | Task | What It Does |
|---|---|---|
| **DecideActionWorker** | `soar_decide_action` | Determines the response actions based on enriched alert data (e.g., isolate host, block C2 domain) |
| **EnrichWorker** | `soar_enrich` | Adds threat intelligence, asset context, and user history to the raw alert |
| **ExecutePlaybookWorker** | `soar_execute_playbook` | Executes the decided response playbook. host isolation, domain blocking, forensic collection |
| **IngestAlertWorker** | `soar_ingest_alert` | Ingests a security alert from an external source (e.g., CrowdStrike, Splunk) |

the workflow logic stays the same.

### The Workflow

```
soar_ingest_alert
 │
 ▼
soar_enrich
 │
 ▼
soar_decide_action
 │
 ▼
soar_execute_playbook

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
