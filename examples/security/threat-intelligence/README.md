# Implementing Threat Intelligence in Java with Conductor : Feed Ingestion, Context Enrichment, IOC Correlation, and Intel Distribution

## The Problem

You subscribe to threat intelligence feeds (VirusTotal, AlienVault OTX, government CERTs) that provide indicators of compromise (IOCs). malicious IPs, domains, file hashes. Each IOC must be ingested, enriched with context (who reported it, what malware family, confidence level), correlated against your environment (have we seen this IP in our logs?), and distributed to security tools (firewall blocklists, SIEM rules, EDR policies).

Without orchestration, threat intelligence is consumed manually. a security analyst reads feed emails, searches indicators in the SIEM, and manually adds block rules. By the time a malicious IP is blocked, it's been active in the environment for hours.

## The Solution

**You just write the feed parsers and IOC correlation queries. Conductor handles feed polling, retries when threat APIs are rate-limited, and tracking of every IOC ingested, correlated, and distributed.**

Each intelligence step is an independent worker. feed ingestion, enrichment, correlation, and distribution. Conductor runs them in sequence: ingest IOCs from feeds, enrich with context, correlate against your logs, then distribute to security tools. Every intelligence cycle is tracked with IOC counts, correlation hits, and distribution status. ### What You Write: Workers

Four workers process threat data: IngestFeedsWorker pulls IOCs from external feeds, CorrelateIocsWorker matches them against your infrastructure, EnrichContextWorker adds MITRE ATT&CK attribution, and DistributeIntelWorker pushes actionable intel to firewalls and SIEMs.

| Worker | Task | What It Does |
|---|---|---|
| **CorrelateIocsWorker** | `ti_correlate_iocs` | Matches ingested IOCs against internal infrastructure to find potential compromises |
| **DistributeIntelWorker** | `ti_distribute_intel` | Distributes actionable intelligence to security tools (SIEM, firewall, EDR) |
| **EnrichContextWorker** | `ti_enrich_context` | Adds MITRE ATT&CK mapping and threat actor attribution to correlated indicators |
| **IngestFeedsWorker** | `ti_ingest_feeds` | Ingests indicators of compromise from multiple threat intelligence feeds |

the workflow logic stays the same.

### The Workflow

```
ti_ingest_feeds
 │
 ▼
ti_correlate_iocs
 │
 ▼
ti_enrich_context
 │
 ▼
ti_distribute_intel

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
