# Media Rights Management in Java Using Conductor :  License Validation, Usage Verification, Royalty Tracking, and Compliance Reporting

A Java Conductor workflow example that orchestrates media rights management .  checking license validity (expiration dates, allowed usages, license types, royalty rates), verifying territorial usage restrictions against the content's distribution region, calculating royalty payments due to rights holders, and generating compliance reports for rights audits. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers ,  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## Why Rights Management Needs Orchestration

Using licensed media content requires a compliance pipeline where each step validates a different aspect of the rights agreement. You check the license .  verifying it has not expired, the intended usage (streaming, download, broadcast) is permitted, and noting the royalty rate. You verify that the content distribution region complies with territorial restrictions in the license agreement. You calculate royalty payments owed to the rights holder based on usage volume and the contractual rate. Finally, you generate a compliance report documenting every license check, usage verification, and royalty calculation for legal audit.

If the license check reveals an expired license, distribution must stop .  not proceed to royalty calculation on an invalid license. If territorial verification fails, you need to block distribution in the restricted region while allowing it elsewhere. Without orchestration, you'd build a monolithic rights checker that mixes license database queries, geographic compliance logic, payment calculations, and audit logging ,  making it impossible to add new license types, update royalty formulas, or demonstrate compliance to rights holders during audits.

## How This Workflow Solves It

**You just write the rights workers. License validation, usage verification, royalty calculation, and compliance reporting. Conductor handles license-gated sequencing, royalty calculation retries, and complete audit trails for rights holder accountability.**

Each rights management concern is an independent worker .  check license, verify usage, track royalties, generate report. Conductor sequences them, passes license details and territorial flags between stages, stops the pipeline if a license is invalid, and maintains a complete audit trail of every rights check for legal compliance.

### What You Write: Workers

Four workers enforce the rights pipeline: CheckLicenseWorker validates expiration and usage terms, VerifyUsageWorker confirms territorial compliance, TrackRoyaltiesWorker calculates payments owed, and GenerateReportWorker produces the compliance audit record.

| Worker | Task | What It Does |
|---|---|---|
| **CheckLicenseWorker** | `rts_check_license` | Checks the license |
| **GenerateReportWorker** | `rts_generate_report` | Generates the report |
| **TrackRoyaltiesWorker** | `rts_track_royalties` | Tracks royalties |
| **VerifyUsageWorker** | `rts_verify_usage` | Verifies the usage |

Workers simulate media processing stages .  transcoding, thumbnail generation, metadata extraction ,  with realistic output artifacts. Replace with real media tools (FFmpeg, ImageMagick) and the pipeline stays the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
rts_check_license
    │
    ▼
rts_verify_usage
    │
    ▼
rts_track_royalties
    │
    ▼
rts_generate_report
```

## Example Output

```
=== Example 529: Rights Management ===

Step 1: Registering task definitions...
  Registered: rts_check_license, rts_verify_usage, rts_track_royalties, rts_generate_report

Step 2: Registering workflow 'rights_management_workflow'...
  Workflow registered.

Step 3: Starting workers...
  4 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [license] Processing
  [report] Processing
  [royalties] Processing
  [verify] Processing

  Status: COMPLETED
  Output: {valid=..., expirationDate=..., allowedUsages=..., royaltyRate=...}

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
java -jar target/rights-management-1.0.0.jar
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
java -jar target/rights-management-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow rights_management_workflow \
  --version 1 \
  --input '{"assetId": "SONG-529-001", "SONG-529-001": "licenseId", "licenseId": "LIC-MUS-2026-450", "LIC-MUS-2026-450": "usageType", "usageType": "streaming", "streaming": "territory", "territory": "US", "US": "sample-US"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w rights_management_workflow -s COMPLETED -c 5
```

## How to Extend

Connect CheckLicenseWorker to your rights database, VerifyUsageWorker to your distribution tracking system, and TrackRoyaltiesWorker to your royalty accounting platform. The workflow definition stays exactly the same.

- **CheckLicenseWorker** (`rts_check_license`): query your rights management system or license database to validate expiration, allowed usages, license type, and royalty rate for the content
- **VerifyUsageWorker** (`rts_verify_usage`): check territorial restrictions against the distribution region using your geo-rights database or a rights clearinghouse API
- **TrackRoyaltiesWorker** (`rts_track_royalties`): calculate royalty payments using your billing system, applying the contractual rate to usage volume, and queue payments to rights holders
- **GenerateReportWorker** (`rts_generate_report`): produce compliance reports in PDF or JSON format documenting every license check, territorial verification, and royalty calculation for audit purposes

Replace any worker with your rights database or royalty system while maintaining the same output fields, and the compliance pipeline operates unmodified.

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
rights-management/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/rightsmanagement/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── RightsManagementExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CheckLicenseWorker.java
│       ├── GenerateReportWorker.java
│       ├── TrackRoyaltiesWorker.java
│       └── VerifyUsageWorker.java
└── src/test/java/rightsmanagement/workers/
    ├── CheckLicenseWorkerTest.java        # 2 tests
    └── VerifyUsageWorkerTest.java        # 2 tests
```
