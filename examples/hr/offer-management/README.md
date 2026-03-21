# Offer Management in Java with Conductor :  Generation, Approval, Delivery, and Accept/Decline Routing

A Java Conductor workflow example for job offer management. generating an offer letter with position and salary details, routing for compensation committee approval, delivering the offer to the candidate, and using SWITCH to route the candidate's response to either the acceptance path (trigger onboarding) or the decline path (reopen the requisition). Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## The Problem

You need to manage job offers from generation through candidate response. After the hiring team selects a candidate, an offer letter must be generated with the position title, base salary, equity package, benefits summary, and start date. The offer must be approved by the compensation committee or hiring VP to ensure it falls within the approved salary band and headcount budget. Once approved, the offer is sent to the candidate with an expiration deadline. The candidate either accepts. triggering the onboarding pipeline, background check, and equipment provisioning, or declines,  reopening the requisition and notifying the recruiter to extend the offer to the next candidate in the pipeline. If the system sends an offer before approval, the company may commit to an unauthorized salary. If a decline is not handled, the requisition sits idle and the role goes unfilled.

Without orchestration, you'd manage offers through email threads and spreadsheets. the recruiter drafts the letter, emails the VP for approval, manually sends the offer, and watches for a reply. If the candidate doesn't respond, the recruiter must remember to follow up before the deadline. If the system crashes after approval but before the offer is sent, the approved offer never reaches the candidate. There is no audit trail showing who approved what amount or when the offer was extended.

## The Solution

**You just write the offer generation, compensation approval, candidate delivery, and accept/decline routing logic. Conductor handles approval routing, offer generation retries, and compensation audit trails.**

Each stage of offer management is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of generating the offer before seeking approval, sending only after approval is granted, routing the candidate's response via SWITCH to the correct accept or decline path, retrying if the HRIS or e-signature platform is temporarily unavailable, and maintaining a complete audit trail of every offer's lifecycle. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Compensation calculation, offer letter generation, approval routing, and candidate notification workers each handle one step of extending a job offer.

| Worker | Task | What It Does |
|---|---|---|
| **GenerateWorker** | `ofm_generate` | Creates the offer letter with candidate name, position title, salary, equity, benefits, and start date, returning an offer ID |
| **ApproveWorker** | `ofm_approve` | Routes the offer for compensation committee or VP approval, verifying the salary falls within the approved band for the role and level |
| **SendWorker** | `ofm_send` | Delivers the approved offer letter to the candidate via email or e-signature platform with an acceptance deadline |
| **AcceptWorker** | `ofm_accept` | Processes the candidate's acceptance. marks the requisition filled, triggers onboarding, and notifies the hiring team |
| **DeclineWorker** | `ofm_decline` | Processes the candidate's decline. reopens the requisition, notifies the recruiter, and flags the next candidate in the pipeline |

Workers implement HR operations. onboarding tasks, approvals, provisioning,  with realistic outputs. Replace with real HRIS and identity provider integrations and the workflow stays the same.

### The Workflow

```
ofm_generate
    │
    ▼
ofm_approve
    │
    ▼
ofm_send
    │
    ▼
SWITCH (ofm_switch_ref)
    ├── accept: ofm_accept
    ├── decline: ofm_decline

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
java -jar target/offer-management-1.0.0.jar

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
java -jar target/offer-management-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow ofm_offer_management \
  --version 1 \
  --input '{"candidateName": "test", "position": "sample-position", "salary": "sample-salary", "response": "sample-response"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w ofm_offer_management -s COMPLETED -c 5

```

## How to Extend

Swap each worker for your real offer systems. your HRIS for offer letter generation, your compensation approval platform, DocuSign for candidate signature and response tracking, and the workflow runs identically in production.

- **GenerateWorker** → generate offer letters from templates in your ATS with dynamic salary, equity, sign-on bonus, and relocation package details pulled from the approved compensation plan
- **ApproveWorker** → route approvals through your HRIS with salary band validation, headcount budget checks, and multi-level approval chains for offers exceeding thresholds
- **SendWorker** → deliver offers via DocuSign or Adobe Sign with embedded e-signature, track when the candidate opens the document, and enforce expiration deadlines
- **AcceptWorker** → on acceptance, automatically close the requisition in your ATS, trigger the onboarding workflow, initiate background check, and notify IT for equipment provisioning
- **DeclineWorker** → on decline, reopen the req, move the next-ranked candidate to "offer pending" in your ATS, and send the recruiter a notification with decline reason data
- Add a **NegotiateWorker** as a SWITCH case for "counter" responses where the candidate proposes different terms, routing back through approval with the revised package
- Add a **ExpirationWorker** with a Conductor WAIT task to automatically withdraw the offer and notify the recruiter if the candidate does not respond within the deadline

Change compensation models or approval chains and the offer pipeline retains its structure.

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
offer-management-offer-management/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/offermanagement/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── OfferManagementExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AcceptWorker.java
│       ├── ApproveWorker.java
│       ├── DeclineWorker.java
│       ├── GenerateWorker.java
│       └── SendWorker.java
└── src/test/java/offermanagement/workers/
    ├── AcceptWorkerTest.java        # 2 tests
    ├── ApproveWorkerTest.java        # 2 tests
    ├── DeclineWorkerTest.java        # 2 tests
    ├── GenerateWorkerTest.java        # 2 tests
    └── SendWorkerTest.java        # 2 tests

```
