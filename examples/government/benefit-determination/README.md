# Government Benefit Determination in Java with Conductor :  Eligibility Verification, Calculation, and Applicant Notification

A Java Conductor workflow example for government benefit determination. receiving applications, verifying income-based eligibility, calculating benefit amounts, and routing applicants to approval or denial notifications. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.
## The Problem

You need to process benefit applications for a government assistance program. Each application requires intake validation, income verification against eligibility thresholds, benefit amount calculation based on the applicant's financial profile, and then routing to either an approval notice (with the benefit amount) or a denial notice (with the specific reason). The eligibility decision must branch the workflow. eligible applicants receive a benefit calculation and approval letter, while ineligible applicants receive a denial with an explanation.

Without orchestration, you'd build a monolithic service that queries the applicant database, runs the income check, computes the benefit, and branches with if/else into notification logic. If the eligibility service is temporarily unavailable, you'd need retry logic. If the notification step fails after eligibility is already determined, you'd need to track partial progress. Auditors require a complete trail of every determination for compliance reviews.

## The Solution

**You just write the application intake, eligibility verification, benefit calculation, and approval or denial notification logic. Conductor handles eligibility retries, benefit routing, and determination audit trails.**

Each stage of the benefit determination is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of executing them in the right order, routing eligible and ineligible applicants to different notification paths via SWITCH, retrying on failure, and providing a complete audit trail of every determination. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Application intake, eligibility verification, benefit calculation, and enrollment workers process government benefits through transparent, rule-based stages.

| Worker | Task | What It Does |
|---|---|---|
| **ApplyWorker** | `bnd_apply` | Receives and validates the benefit application for a given applicant and program type |
| **VerifyEligibilityWorker** | `bnd_verify_eligibility` | Checks the applicant's income against the program threshold ($50,000) and returns eligible/ineligible with reason |
| **CalculateWorker** | `bnd_calculate` | Computes the benefit amount based on eligibility status and income level |
| **NotifyEligibleWorker** | `bnd_notify_eligible` | Sends an approval notice to the applicant with their calculated benefit amount |
| **NotifyIneligibleWorker** | `bnd_notify_ineligible` | Sends a denial notice to the applicant with the specific reason for ineligibility |

Workers implement government operations. application processing, compliance checks, notifications,  with realistic outputs. Replace with real agency system integrations and the workflow stays the same.

### The Workflow

```
bnd_apply
    │
    ▼
bnd_verify_eligibility
    │
    ▼
bnd_calculate
    │
    ▼
SWITCH (bnd_switch_ref)
    ├── eligible: bnd_notify_eligible
    ├── ineligible: bnd_notify_ineligible

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
java -jar target/benefit-determination-1.0.0.jar

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
java -jar target/benefit-determination-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow bnd_benefit_determination \
  --version 1 \
  --input '{"applicantId": "TEST-001", "programType": "standard", "income": "sample-income"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w bnd_benefit_determination -s COMPLETED -c 5

```

## How to Extend

Connect each worker to your real benefits systems. your eligibility verification service for income checks, your benefit calculator for amount determination, your case management platform for applicant notifications, and the workflow runs identically in production.

- **ApplyWorker** → integrate with your agency's case management system to create intake records and validate required documentation
- **VerifyEligibilityWorker** → call federal/state income verification APIs (IRS, SSA) instead of using a hardcoded threshold
- **CalculateWorker** → plug in your program's benefit schedule with sliding scales, household size adjustments, and regional cost-of-living factors
- **NotifyEligibleWorker** / **NotifyIneligibleWorker** → send real letters via USPS or emails via SendGrid, with legally required appeal rights language
- Add a **FraudCheckWorker** before eligibility verification to cross-reference applicant data against fraud databases

Update eligibility rules or benefit calculations and the determination pipeline handles them seamlessly.

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
benefit-determination-benefit-determination/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/benefitdetermination/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── BenefitDeterminationExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ApplyWorker.java
│       ├── CalculateWorker.java
│       ├── NotifyEligibleWorker.java
│       ├── NotifyIneligibleWorker.java
│       └── VerifyEligibilityWorker.java
└── src/test/java/benefitdetermination/workers/
    ├── CalculateWorkerTest.java
    └── VerifyEligibilityWorkerTest.java

```
