# Threat Intelligence

The security team needs to process threat feeds. The pipeline ingests 2,400 indicators from 5 threat feeds, correlates 18 IOCs against internal infrastructure, enriches context with MITRE ATT&CK mapping and actor attribution, and distributes the intel to SIEM, firewall, and EDR systems.

## Workflow

```
ti_ingest_feeds ──> ti_correlate_iocs ──> ti_enrich_context ──> ti_distribute_intel
```

Workflow `threat_intelligence_workflow` accepts `feedSources` and `lookbackHours`. Times out after `300` seconds.

## Workers

**IngestFeedsWorker** (`ti_ingest_feeds`) -- reports `"Ingested 2,400 indicators from 5 threat feeds"`. Returns `ingest_feedsId` = `"INGEST_FEEDS-1351"`.

**CorrelateIocsWorker** (`ti_correlate_iocs`) -- matches indicators against infrastructure. Reports `"18 IOCs matched against internal infrastructure"`. Returns `correlate_iocs` = `true`.

**EnrichContextWorker** (`ti_enrich_context`) -- adds threat framework context. Reports `"Added MITRE ATT&CK mapping and actor attribution"`. Returns `enrich_context` = `true`.

**DistributeIntelWorker** (`ti_distribute_intel`) -- pushes intel to security tools. Reports `"Intel distributed to SIEM, firewall, and EDR"`. Returns `distribute_intel` = `true`.

## Workflow Output

The workflow produces `ingest_feedsResult`, `distribute_intelResult` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Project Structure

This example contains 4 worker implementations in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `threat_intelligence_workflow` defines 4 tasks with input parameters `feedSources`, `lookbackHours` and a timeout of `300` seconds.

## Workflow Definition Details

Schema version `2`, workflow version `1`. Owner: `examples@orkes.io`.

## Tests

2 tests verify the end-to-end threat intelligence pipeline from feed ingestion through intel distribution.

## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
