# Hubspot Integration in Java Using Conductor

A Java Conductor workflow that onboards a new contact in HubSpot. creating the contact record, enriching it with company data (industry, size, revenue), assigning a sales rep owner based on the enriched profile, and enrolling the contact in a nurture sequence. Given contact details (email, name, company), the pipeline produces a contact ID, enriched profile, owner assignment, and sequence enrollment. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate the create-enrich-assign-nurture pipeline.

## Onboarding Contacts with Enrichment and Assignment

When a new lead arrives, they need to be created as a contact in HubSpot, their profile needs to be enriched with company intelligence (industry, company size, revenue segment), a sales rep needs to be assigned based on the enriched profile, and the contact should be enrolled in an appropriate nurture sequence. Each step depends on the previous one. you cannot enrich without a contact ID, and you cannot assign an owner without knowing the industry and company size.

Without orchestration, you would chain HubSpot API calls manually, pass contact IDs between steps, and manage the enrichment-to-assignment logic yourself. Conductor sequences the pipeline and routes contact IDs, enrichment data, and owner assignments between workers automatically.

## The Solution

**You just write the HubSpot workers. Contact creation, profile enrichment, owner assignment, and nurture sequence enrollment. Conductor handles contact-to-nurture sequencing, CRM API retries, and contact ID routing across enrichment and assignment stages.**

Each worker integrates with one external system. Conductor manages the integration sequence, retry logic, timeout handling, and data transformation between systems.

### What You Write: Workers

Four workers onboard CRM contacts: CreateContactWorker adds the lead, EnrichDataWorker augments with company intelligence, AssignOwnerWorker routes to the right sales rep, and NurtureSequenceWorker enrolls in drip campaigns.

| Worker | Task | What It Does |
|---|---|---|
| **CreateContactWorker** | `hs_create_contact` | Creates a contact in HubSpot. takes the email, name, and company and returns the contact ID |
| **EnrichDataWorker** | `hs_enrich_data` | Enriches the contact profile. looks up company intelligence (industry, company size, revenue segment) using the contact ID and company name |
| **AssignOwnerWorker** | `hs_assign_owner` | Assigns a sales rep owner. routes the contact to the appropriate rep based on the enriched profile (industry, company size, territory) |
| **NurtureSequenceWorker** | `hs_nurture_sequence` | Enrolls the contact in a nurture sequence. selects the appropriate email drip campaign based on the contact's profile and stage |

Workers implement external API calls with realistic response shapes so you can see the integration flow end-to-end. Replace with real API clients. the workflow orchestration and error handling stay the same.

### The Workflow

```
hs_create_contact
    │
    ▼
hs_enrich_data
    │
    ▼
hs_assign_owner
    │
    ▼
hs_nurture_sequence

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
java -jar target/hubspot-integration-1.0.0.jar

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
| `HUBSPOT_API_KEY` | _(none)_ | HubSpot private app access token. When set, CreateContactWorker calls the HubSpot Contacts API. When unset, all workers run in demo mode with `[DEMO]` output prefix. |

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/hubspot-integration-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow hubspot_integration_439 \
  --version 1 \
  --input '{"email": "user@example.com", "firstName": "test", "lastName": "test", "company": "sample-company"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w hubspot_integration_439 -s COMPLETED -c 5

```

## How to Extend

Swap in HubSpot CRM API calls for contact creation, Clearbit or ZoomInfo for enrichment, and HubSpot Sequences API for nurture enrollment. The workflow definition stays exactly the same.

- **CreateContactWorker** (`hs_create_contact`): use the HubSpot CRM API to create real contacts with properties
- **EnrichDataWorker** (`hs_enrich_data`): integrate with a data enrichment provider (Clearbit, ZoomInfo) for real company intelligence
- **NurtureSequenceWorker** (`hs_nurture_sequence`): use the HubSpot Sequences API to enroll contacts in real nurture sequences

Connect each worker to the HubSpot API or enrichment provider while keeping the same output fields, and the onboarding pipeline needs no reconfiguration.

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
hubspot-integration/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/hubspotintegration/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── HubspotIntegrationExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AssignOwnerWorker.java
│       ├── CreateContactWorker.java
│       ├── EnrichDataWorker.java
│       └── NurtureSequenceWorker.java
└── src/test/java/hubspotintegration/workers/
    ├── AssignOwnerWorkerTest.java        # 2 tests
    ├── CreateContactWorkerTest.java        # 2 tests
    ├── EnrichDataWorkerTest.java        # 2 tests
    └── NurtureSequenceWorkerTest.java        # 2 tests

```
