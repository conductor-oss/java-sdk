# Background Check in Java with Conductor :  Consent, Parallel Criminal/Employment/Education Verification, and Report

A Java Conductor workflow example for pre-employment background checks. collecting candidate consent under FCRA, running criminal record, employment history, and education verification in parallel via FORK_JOIN, and compiling a final eligibility report. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.
## The Problem

You need to verify a candidate's background before their start date. The Fair Credit Reporting Act (FCRA) requires written consent before any checks can begin. Once consent is obtained, three independent verifications must run: a criminal record search across multiple jurisdictions (county, state, federal), an employment history verification confirming titles, dates, and employers from the candidate's resume, and an education verification confirming degrees and institutions. These three checks are independent. each queries different databases and services,  so they should run in parallel to minimize the total turnaround time. Once all three complete, a consolidated report determines the overall result (clear, flagged, or adverse) for the hiring decision. Running checks sequentially when they could run in parallel wastes days of time-to-hire.

Without orchestration, you'd build a monolithic verification system that collects consent, then sequentially calls the criminal database, employment verification service, and education registrar, or manages parallel threads manually with CompletableFuture and custom join logic. If the criminal records database is temporarily unavailable, you'd need retry logic per check. If the system crashes after employment verification completes but before education finishes, you lose the completed results and must restart everything. FCRA requires a complete audit trail documenting when consent was obtained and what was checked.

## The Solution

**You just write the consent collection, criminal check, employment verification, education verification, and eligibility reporting logic. Conductor handles verification retries, parallel check coordination, and background audit trails.**

Each verification step is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of obtaining consent before any checks begin, running criminal, employment, and education checks simultaneously via FORK_JOIN, waiting for all three to complete before generating the report, retrying any individual check if its data source is temporarily unavailable (without re-running the others), and maintaining an FCRA-compliant audit trail of every verification step. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Identity verification, criminal records search, employment history check, and report compilation workers each investigate one dimension of a candidate's background.

| Worker | Task | What It Does |
|---|---|---|
| **ConsentWorker** | `bgc_consent` | Collects the candidate's FCRA-compliant written consent and disclosure acknowledgment before any checks can proceed |
| **CriminalWorker** | `bgc_criminal` | Searches criminal records across county, state, and federal jurisdictions, returning clear/flagged status and the number of jurisdictions checked |
| **EmploymentWorker** | `bgc_employment` | Verifies the candidate's employment history. job titles, dates of employment, and employers,  against what they reported |
| **EducationWorker** | `bgc_education` | Verifies degrees, institutions, and graduation dates against the National Student Clearinghouse or institution registrars |
| **ReportWorker** | `bgc_report` | Compiles all three verification results into a consolidated background check report with an overall clear/flagged/adverse determination |

Workers implement HR operations. onboarding tasks, approvals, provisioning,  with realistic outputs. Replace with real HRIS and identity provider integrations and the workflow stays the same.

### The Workflow

```
bgc_consent
    │
    ▼
FORK_JOIN
    ├── bgc_criminal
    ├── bgc_employment
    └── bgc_education
    │
    ▼
JOIN (wait for all branches)
bgc_report

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
java -jar target/background-check-1.0.0.jar

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
java -jar target/background-check-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow bgc_background_check \
  --version 1 \
  --input '{"candidateName": "test", "candidateId": "TEST-001"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w bgc_background_check -s COMPLETED -c 5

```

## How to Extend

Connect each worker to your real screening services. Checkr or Sterling for criminal and employment verification, the National Student Clearinghouse for education checks, your HRIS for eligibility reporting, and the workflow runs identically in production.

- **ConsentWorker** → collect real FCRA consent via DocuSign or your background check provider's consent API, with state-specific disclosure language (California, New York, and other "ban the box" jurisdictions)
- **CriminalWorker** → search real criminal databases via Checkr, Sterling, or GoodHire across county, state, federal, and sex offender registries with configurable jurisdiction scope
- **EmploymentWorker** → verify employment through The Work Number (Equifax), Truework, or direct employer HR department calls with title, dates, and eligibility for rehire
- **EducationWorker** → verify degrees through the National Student Clearinghouse or direct registrar verification, including professional certifications and licenses
- **ReportWorker** → generate FCRA-compliant reports with pre-adverse and adverse action letter workflows, including the required waiting periods and candidate dispute process
- Add a **CreditWorker** as a FORK_JOIN branch for positions requiring credit checks (finance, government) with consumer credit report from Experian, Equifax, or TransUnion
- Add a **DrugScreenWorker** to coordinate specimen collection scheduling, lab results tracking, and MRO (Medical Review Officer) review

Swap verification providers or add new check types and the pipeline structure stays unchanged.

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
background-check-background-check/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/backgroundcheck/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── BackgroundCheckExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ConsentWorker.java
│       ├── CriminalWorker.java
│       ├── EducationWorker.java
│       ├── EmploymentWorker.java
│       └── ReportWorker.java
└── src/test/java/backgroundcheck/workers/
    ├── ConsentWorkerTest.java        # 2 tests
    ├── CriminalWorkerTest.java        # 2 tests
    ├── EducationWorkerTest.java        # 2 tests
    ├── EmploymentWorkerTest.java        # 2 tests
    └── ReportWorkerTest.java        # 2 tests

```
