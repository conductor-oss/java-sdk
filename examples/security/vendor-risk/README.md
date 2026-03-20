# Implementing Vendor Risk Assessment in Java with Conductor :  Questionnaire, SOC2 Review, Risk Scoring, and Decision

A Java Conductor workflow example for vendor risk assessment .  collecting security questionnaires from vendors, reviewing their SOC2/ISO27001 certifications, scoring their overall risk, and making an approve/conditional/deny decision on vendor onboarding.

## The Problem

Before giving a vendor access to your data, you need to assess their security posture. This requires collecting their security questionnaire responses, reviewing their compliance certifications (SOC2 Type II, ISO 27001), scoring the overall risk based on data access level and security maturity, and making a go/no-go decision. If the vendor handles PII, the bar is higher.

Without orchestration, vendor risk assessment lives in email threads. Someone sends the questionnaire, waits weeks for a response, forwards the SOC2 report to security, and the approval decision happens in a meeting with no audit trail. Three months later, nobody can tell why a vendor was approved.

## The Solution

**You just write the questionnaire collection and risk scoring logic. Conductor handles the assessment sequence, retries when vendor portals are slow to respond, and a complete audit trail of every risk score, SOC2 review, and onboarding decision.**

Each assessment step is an independent worker .  questionnaire collection, SOC2 review, risk scoring, and decision. Conductor runs them in sequence: collect the questionnaire, review certifications, score the risk, then make the decision. Every assessment is tracked with responses, review findings, risk score, and decision rationale for audit. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

The assessment pipeline chains CollectQuestionnaireWorker to gather vendor responses, AssessRiskWorker to calculate a risk score, ReviewSoc2Worker to verify compliance certifications, and MakeDecisionWorker to produce an approve/conditional/deny ruling.

| Worker | Task | What It Does |
|---|---|---|
| **AssessRiskWorker** | `vr_assess_risk` | Calculates a risk score (0-100) and identifies specific security gaps (e.g., data encryption) |
| **CollectQuestionnaireWorker** | `vr_collect_questionnaire` | Collects the completed security questionnaire from the vendor |
| **MakeDecisionWorker** | `vr_make_decision` | Makes an approve/deny/conditional decision based on risk score and SOC 2 review |
| **ReviewSoc2Worker** | `vr_review_soc2` | Reviews the vendor's SOC 2 Type II report for validity and qualifications |

Workers simulate security checks and remediation actions with realistic findings so you can see the response flow without live security tools. Replace with real scanner and SIEM integrations .  the workflow logic stays the same.

### The Workflow

```
vr_collect_questionnaire
    │
    ▼
vr_assess_risk
    │
    ▼
vr_review_soc2
    │
    ▼
vr_make_decision
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
java -jar target/vendor-risk-1.0.0.jar
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
java -jar target/vendor-risk-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow vendor_risk_workflow \
  --version 1 \
  --input '{"vendorName": "test", "dataAccess": "test-value"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w vendor_risk_workflow -s COMPLETED -c 5
```

## How to Extend

Each worker covers one assessment phase .  connect CollectQuestionnaireWorker to OneTrust or SecurityScorecard, ReviewSoc2Worker to parse real audit reports, and the questionnaire-review-score-decision workflow stays the same.

- **AssessRiskWorker** (`vr_assess_risk`): compute a risk score based on data access level, security maturity, vendor size, and compliance status
- **CollectQuestionnaireWorker** (`vr_collect_questionnaire`): send security questionnaires via OneTrust, SecurityScorecard, or custom forms and collect vendor responses
- **MakeDecisionWorker** (`vr_make_decision`): apply risk acceptance criteria .  auto-approve low risk, route medium risk to security review, deny high risk

Integrate with your GRC platform for real questionnaire data, and the risk assessment orchestration transfers with no changes needed.

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
vendor-risk-vendor-risk/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/vendorrisk/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── MainExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AssessRiskWorker.java
│       ├── CollectQuestionnaireWorker.java
│       ├── MakeDecisionWorker.java
│       └── ReviewSoc2Worker.java
└── src/test/java/vendorrisk/
    └── MainExampleTest.java        # 2 tests .  workflow resource loading, worker instantiation
```
