# Government Permit in Java with Conductor

Processes a government permit application: receiving the application, validating documents, routing to a zoning board review, and issuing or denying the permit via a SWITCH task. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers ,  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## The Problem

You need to process a government permit application. A citizen submits an application for a permit (building, business, event), the application is validated for completeness and jurisdiction, a reviewer assesses it against regulations and zoning rules, and the permit is either issued or denied with explanation. Issuing a permit without proper review creates legal liability for the government; denying without explanation violates due process.

Without orchestration, you'd manage permits through a legacy system with paper forms, manual reviews, and status tracking in spreadsheets .  losing applications in the queue, missing review deadlines mandated by statute, and struggling to produce audit trails when a permit decision is challenged.

## The Solution

**You just write the application intake, document validation, zoning review, and permit issuance or denial logic. Conductor handles review retries, approval routing, and permit application audit trails.**

Each permit concern is a simple, independent worker .  a plain Java class that does one thing. Conductor takes care of executing the application flow (apply, validate, review, issue/deny), routing via a SWITCH task to the correct outcome, tracking every application with timestamps and reviewer notes, and resuming from the last step if the process crashes. You get all of that for free, without writing a single line of orchestration code.

### What You Write: Workers

Application intake, document validation, zoning review, and permit issuance workers each handle one stage of the government permitting process.

| Worker | Task | What It Does |
|---|---|---|
| **ApplyWorker** | `gvp_apply` | Receives and validates the permit application with applicant details and permit type |
| **DenyWorker** | `gvp_deny` | Denies the permit application with the specified reason and records the denial |
| **IssueWorker** | `gvp_issue` | Issues the approved permit to the applicant with a unique permit number |
| **ReviewWorker** | `gvp_review` | Conducts a zoning board review of the application and returns an approve/deny decision |
| **ValidateWorker** | `gvp_validate` | Validates the application for completeness and verifies all required documents |

Workers simulate government operations .  application processing, compliance checks, notifications ,  with realistic outputs. Replace with real agency system integrations and the workflow stays the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |
| **Conditional routing** | SWITCH tasks route execution to different paths based on worker output |

### The Workflow

```
gvp_apply
    │
    ▼
gvp_validate
    │
    ▼
gvp_review
    │
    ▼
SWITCH (gvp_switch_ref)
    ├── approve: gvp_issue
    ├── deny: gvp_deny
```

## Example Output

```
=== Example 521: Government Permit ===

Step 1: Registering task definitions...
  Registered: gvp_apply, gvp_validate, gvp_review, gvp_issue, gvp_deny

Step 2: Registering workflow 'gvp_government_permit'...
  Workflow registered.

Step 3: Starting workers...
  5 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [apply] Processing
  [deny] Processing
  [issue] Processing
  [review] Zoning board review complete .  approved
  [validate] Application documents verified

  Status: COMPLETED
  Output: {application=..., denied=..., permitNumber=..., issued=...}

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
java -jar target/government-permit-1.0.0.jar
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
java -jar target/government-permit-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow gvp_government_permit \
  --version 1 \
  --input '{"applicantId": "CIT-100", "CIT-100": "permitType", "permitType": "building", "building": "details", "details": "New garage construction", "New garage construction": "sample-New garage construction"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w gvp_government_permit -s COMPLETED -c 5
```

## How to Extend

Connect each worker to your real permitting systems .  your citizen portal for application intake, your GIS system for zoning validation, your permitting database for issuance decisions, and the workflow runs identically in production.

- **Application handler**: integrate with your government e-services portal for online permit applications with document uploads
- **Validator**: check application completeness, verify jurisdiction, and validate against zoning maps and GIS data
- **Reviewer**: route to the appropriate department reviewer based on permit type; use a WAIT task for human review with statutory deadline tracking
- **Permit issuer**: generate the official permit document with unique permit number, conditions, and expiration date
- **Denial handler**: generate denial notice with specific regulatory citations and appeal instructions per administrative procedure requirements

Change zoning rules or approval workflows and the permitting pipeline adapts without restructuring.

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
government-permit-government-permit/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/governmentpermit/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── GovernmentPermitExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ApplyWorker.java
│       ├── DenyWorker.java
│       ├── IssueWorker.java
│       ├── ReviewWorker.java
│       └── ValidateWorker.java
└── src/test/java/governmentpermit/workers/
    ├── ApplyWorkerTest.java
    ├── DenyWorkerTest.java
    ├── IssueWorkerTest.java
    ├── ReviewWorkerTest.java
    └── ValidateWorkerTest.java
```
