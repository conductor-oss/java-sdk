# Agency Management in Java with Conductor :  Onboard, License, Assign Territory, Track Performance, Review

A Java Conductor workflow example demonstrating agency-management Agency Management. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## Insurance Agent Management Has Regulatory Requirements

Bringing on a new insurance agent requires onboarding (background check, E&O insurance verification, appointment paperwork), licensing verification (state license active, lines of authority match, continuing education current), territory assignment (geographic boundaries, product lines, no overlap with existing agents), performance tracking (premium volume, policy count, loss ratio, retention rate), and periodic review (compensation adjustments, territory changes, license renewals).

Licensing is the critical compliance step .  an agent selling without a valid license exposes the insurer to regulatory penalties and coverage disputes. License status must be verified at onboarding and monitored continuously. Territory assignment must avoid conflicts with existing agents while ensuring market coverage.

## The Solution

**You just write the agent onboarding, license verification, territory assignment, performance tracking, and review logic. Conductor handles licensing retries, territory assignment, and agency lifecycle audit trails.**

`OnboardWorker` processes the new agent application .  background checks, E&O insurance verification, and carrier appointment paperwork. `LicenseWorker` verifies state insurance licenses ,  active status, correct lines of authority, continuing education compliance. `AssignTerritoryWorker` assigns the agent's sales territory ,  geographic boundaries, eligible product lines, and commission schedules. `TrackWorker` monitors ongoing performance ,  premium volume, policy count, loss ratio, customer retention, and complaint rate. `ReviewWorker` conducts periodic performance reviews with compensation adjustments and territory modifications. Conductor tracks the complete agent lifecycle for regulatory compliance.

### What You Write: Workers

Agent onboarding, licensing verification, territory assignment, and performance tracking workers each manage one aspect of the agency relationship.

| Worker | Task | What It Does |
|---|---|---|
| **OnboardWorker** | `agm_onboard` | Onboards the new agent .  processes the application, verifies E&O insurance, runs background checks, and returns the onboarded status and start date |
| **LicenseWorker** | `agm_license` | Verifies the agent's insurance license .  checks the state license status, lines of authority, and continuing education compliance, returning the license number and expiration date |
| **AssignTerritoryWorker** | `agm_assign_territory` | Assigns the agent's sales territory .  determines geographic boundaries, eligible product lines, and commission schedules based on the agent's state and qualifications |
| **TrackWorker** | `agm_track` | Monitors agent performance .  tracks premium volume, policy count (12 policies written), loss ratio, and retention rate within the assigned territory |
| **ReviewWorker** | `agm_review` | Conducts the performance review .  evaluates the agent's tracked performance metrics against territory benchmarks and assigns a rating (exceeds expectations) |

Workers simulate insurance operations .  claim intake, assessment, settlement ,  with realistic outputs. Replace with real claims management and underwriting integrations and the workflow stays the same.

### The Workflow

```
agm_onboard
    │
    ▼
agm_license
    │
    ▼
agm_assign_territory
    │
    ▼
agm_track
    │
    ▼
agm_review

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
docker run -d -p 8080:8080 -p 1234:5000 orkesio/orkes-conductor-standalone:1.2.3

# Wait for Conductor to be ready
until curl -sf http://localhost:8080/health > /dev/null; do sleep 2; done

# Build and run
mvn package -DskipTests
java -jar target/agency-management-1.0.0.jar

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
java -jar target/agency-management-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow agm_agency_management \
  --version 1 \
  --input '{"agentId": "TEST-001", "agentName": "test", "state": "sample-state"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w agm_agency_management -s COMPLETED -c 5

```

## How to Extend

Point each worker at your real agency systems .  your licensing database for credential checks, your territory management platform, your performance analytics for production reviews, and the workflow runs identically in production.

- **LicenseWorker** (`agm_license`): query NIPR (National Insurance Producer Registry) for real-time license verification, or state DOI databases for license status and continuing education compliance
- **TrackWorker** (`agm_track`): pull production data from the policy administration system, calculate commission earned and loss ratios, and compare against territory benchmarks
- **OnboardWorker** (`agm_onboard`): integrate with background check providers (Checkr, Sterling), verify E&O insurance certificates, and submit carrier appointment forms electronically

Change licensing databases or territory maps and the agency pipeline adapts without modification.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):

## Project Structure

```
agency-management-agency-management/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/agencymanagement/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── AgencyManagementExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AssignTerritoryWorker.java
│       ├── LicenseWorker.java
│       ├── OnboardWorker.java
│       ├── ReviewWorker.java
│       └── TrackWorker.java
└── src/test/java/agencymanagement/workers/
    ├── OnboardWorkerTest.java        # 1 tests
    └── ReviewWorkerTest.java        # 1 tests

```
