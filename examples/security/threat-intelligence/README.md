# Implementing Threat Intelligence in Java with Conductor :  Feed Ingestion, Context Enrichment, IOC Correlation, and Intel Distribution

A Java Conductor workflow example for threat intelligence .  ingesting threat feeds, enriching indicators with context, correlating IOCs against your environment, and distributing actionable intelligence to security tools.

## The Problem

You subscribe to threat intelligence feeds (VirusTotal, AlienVault OTX, government CERTs) that provide indicators of compromise (IOCs) .  malicious IPs, domains, file hashes. Each IOC must be ingested, enriched with context (who reported it, what malware family, confidence level), correlated against your environment (have we seen this IP in our logs?), and distributed to security tools (firewall blocklists, SIEM rules, EDR policies).

Without orchestration, threat intelligence is consumed manually .  a security analyst reads feed emails, searches indicators in the SIEM, and manually adds block rules. By the time a malicious IP is blocked, it's been active in the environment for hours.

## The Solution

**You just write the feed parsers and IOC correlation queries. Conductor handles feed polling, retries when threat APIs are rate-limited, and tracking of every IOC ingested, correlated, and distributed.**

Each intelligence step is an independent worker .  feed ingestion, enrichment, correlation, and distribution. Conductor runs them in sequence: ingest IOCs from feeds, enrich with context, correlate against your logs, then distribute to security tools. Every intelligence cycle is tracked with IOC counts, correlation hits, and distribution status. You get all of that for free, without writing a single line of orchestration code.

### What You Write: Workers

Four workers process threat data: IngestFeedsWorker pulls IOCs from external feeds, CorrelateIocsWorker matches them against your infrastructure, EnrichContextWorker adds MITRE ATT&CK attribution, and DistributeIntelWorker pushes actionable intel to firewalls and SIEMs.

| Worker | Task | What It Does |
|---|---|---|
| **CorrelateIocsWorker** | `ti_correlate_iocs` | Matches ingested IOCs against internal infrastructure to find potential compromises |
| **DistributeIntelWorker** | `ti_distribute_intel` | Distributes actionable intelligence to security tools (SIEM, firewall, EDR) |
| **EnrichContextWorker** | `ti_enrich_context` | Adds MITRE ATT&CK mapping and threat actor attribution to correlated indicators |
| **IngestFeedsWorker** | `ti_ingest_feeds` | Ingests indicators of compromise from multiple threat intelligence feeds |

Workers simulate security checks and remediation actions with realistic findings so you can see the response flow without live security tools. Replace with real scanner and SIEM integrations .  the workflow logic stays the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

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

## Example Output

```
=== Example 351: Threat Intelligence ===

Step 1: Registering task definitions...
  Registered: ti_ingest_feeds, ti_correlate_iocs, ti_enrich_context, ti_distribute_intel

Step 2: Registering workflow 'threat_intelligence_workflow'...
  Workflow registered.

Step 3: Starting workers...
  4 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [correlate] 18 IOCs matched against internal infrastructure
  [distribute] Intel distributed to SIEM, firewall, and EDR
  [enrich] Added MITRE ATT&CK mapping and actor attribution
  [ingest] Ingested 2,400 indicators from 5 threat feeds

  Status: COMPLETED

Result: PASSED
```

## Running It

### Prerequisites

- **Java 21+**: verify with `java -version`
- **Maven 3.8+**: verify with `mvn -version`
- **Docker**: to run Conductor

### Option 1: Docker Compose (everything included)

```bash
docker compose up --build
```

Starts Conductor on port 8080 and runs the example automatically.

If port 8080 is already taken:

```bash
CONDUCTOR_PORT=9090 docker compose up --build
```

### Option 2: Run locally

```bash
# Start Conductor
docker run -d -p 8080:8080 -p 1234:5000 orkesio/orkes-conductor-standalone:latest

# Wait for Conductor to be ready
until curl -sf http://localhost:8080/health > /dev/null; do sleep 2; done

# Build and run
mvn package -DskipTests
java -jar target/threat-intelligence-1.0.0.jar
```

### Option 3: Use the run script

```bash
./run.sh

# Or on a custom port:
CONDUCTOR_PORT=9090 ./run.sh

# Or pointing at an existing Conductor:
CONDUCTOR_BASE_URL=http://localhost:9090/api ./run.sh
```

## Configuration

| Environment Variable | Default | Description |
|---|---|---|
| `CONDUCTOR_BASE_URL` | `http://localhost:8080/api` | Conductor server URL |
| `CONDUCTOR_PORT` | `8080` | Host port for Conductor (Docker Compose only) |

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/threat-intelligence-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow threat_intelligence_workflow \
  --version 1 \
  --input '{"feedSources": "sample-feedSources", "OSINT": "sample-OSINT", "lookbackHours": "sample-lookbackHours"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w threat_intelligence_workflow -s COMPLETED -c 5
```

## How to Extend

Each worker handles one intel stage .  connect IngestFeedsWorker to AlienVault OTX or MISP, DistributeIntelWorker to your firewall and SIEM APIs, and the ingest-correlate-distribute workflow stays the same.

- **CorrelateIocsWorker** (`ti_correlate_iocs`): search your SIEM (Splunk, Elastic) for IOC matches in firewall logs, DNS queries, and endpoint telemetry
- **DistributeIntelWorker** (`ti_distribute_intel`): push blocklists to firewalls, create SIEM correlation rules, update EDR detection policies, and notify SOC analysts
- **EnrichContextWorker** (`ti_enrich_context`): enrich IOCs with VirusTotal reputation, WHOIS data, passive DNS, and MITRE ATT&CK technique mapping

Integrate with real threat feeds and firewall APIs, and the ingest-correlate-distribute pipeline continues operating without modification.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):

```xml
<dependency>
    <groupId>org.conductoross</groupId>
    <artifactId>conductor-client</artifactId>
    <version>5.0.1</version>
</dependency>
```

## Project Structure

```
threat-intelligence-threat-intelligence/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/threatintelligence/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── MainExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CorrelateIocsWorker.java
│       ├── DistributeIntelWorker.java
│       ├── EnrichContextWorker.java
│       └── IngestFeedsWorker.java
└── src/test/java/threatintelligence/
    └── MainExampleTest.java        # 2 tests .  workflow resource loading, worker instantiation
```
