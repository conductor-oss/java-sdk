# Insurance Renewal in Java with Conductor :  Notify, Review Risk, Reprice, Route Decision

A Java Conductor workflow example for automated insurance policy renewal. sending a renewal notice to the policyholder, reviewing the policy's claims history and computing a risk score, repricing the premium based on that risk score, then using a SWITCH to route the policy to either renewal processing (with the new premium) or cancellation (with the reason). The review step analyzes the claims count and produces a riskScore that feeds into repricing, and the reprice step outputs the decision ("renew" or "cancel") and the new premium amount that determines the SWITCH path. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## Policy Renewals Require Claims Review, Repricing, and Conditional Routing

When an insurance policy approaches its renewal date, the insurer must review the policyholder's claims history, calculate a risk score, reprice the premium accordingly, and decide whether to renew or non-renew the policy. If the risk is acceptable, the policy renews at the adjusted premium. If the claims history is too costly, the policy is cancelled with a stated reason. The repricing step must use the risk score from the review. if repricing fails, you need to retry it without re-reviewing the entire claims history.

## The Solution

**You just write the renewal notification, risk review, premium repricing, and renew-or-cancel routing logic. Conductor handles repricing retries, decision routing, and renewal audit trails for every policy.**

`NotifyWorker` sends the renewal notice to the policyholder with the current policy details and renewal timeline. `ReviewWorker` examines the policy's claims history, loss ratio, risk factor changes, and market conditions for the coverage area. `RepriceWorker` calculates the updated premium based on the risk review. applying experience rating, inflation adjustments, and regulatory rate changes. Conductor's `SWITCH` routes to renewal (issue at the new premium) or non-renewal (send non-renewal notice with reason) based on the repricing results and underwriting guidelines. Conductor records the complete renewal decision chain for regulatory compliance.

### What You Write: Workers

Notification, risk review, repricing, and renewal processing workers each address one stage of the policy renewal decision.

| Worker | Task | What It Does |
|---|---|---|
| **NotifyWorker** | `irn_notify` | Sends a renewal notice to the policyholder. identifies the policy and customer, then dispatches the renewal communication |
| **ReviewWorker** | `irn_review` | Reviews the policy's claims history. analyzes past claims for the policyId and outputs a riskScore (0.35) and claimsCount (1) that determine repricing |
| **RepriceWorker** | `irn_reprice` | Reprices the premium based on the risk score. adjusts the annual premium ($1,200/year) and outputs a decision ("renew" or "cancel") along with the newPremium that the SWITCH uses for routing |
| *SWITCH* | `route_decision` | Routes based on the reprice decision: "renew" advances to renewal processing with the new premium, "cancel" routes to cancellation processing with the reason | Built-in Conductor SWITCH. no worker needed |
| **ProcessRenewWorker** | `irn_process_renew` | Processes the renewal. issues the renewed policy at the new premium amount and updates the policy term |
| **ProcessCancelWorker** | `irn_process_cancel` | Processes the cancellation. records the non-renewal reason and triggers required regulatory notices to the policyholder |

Workers implement insurance operations. claim intake, assessment, settlement,  with realistic outputs. Replace with real claims management and underwriting integrations and the workflow stays the same.

### The Workflow

```
irn_notify
    │
    ▼
irn_review
    │
    ▼
irn_reprice
    │
    ▼
SWITCH (irn_switch_ref)
    ├── renew: irn_process_renew
    ├── cancel: irn_process_cancel

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
java -jar target/insurance-renewal-1.0.0.jar

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
java -jar target/insurance-renewal-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow irn_insurance_renewal \
  --version 1 \
  --input '{"policyId": "TEST-001", "customerId": "TEST-001", "claimHistory": "sample-claimHistory"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w irn_insurance_renewal -s COMPLETED -c 5

```

## How to Extend

Connect each worker to your real insurance systems. your policy admin system for renewal notices, your claims database for risk scoring, your rating engine for premium repricing, and the workflow runs identically in production.

- **ReviewWorker** (`irn_review`): query claims management systems (Guidewire ClaimCenter, Duck Creek) for loss history, and external data sources (LexisNexis, CLUE reports) for undisclosed claims
- **RepriceWorker** (`irn_reprice`): implement actuarial rating algorithms with territory factors, loss experience modification, and regulatory rate caps per state
- **NotifyWorker** (`irn_notify`): send renewal notices via certified mail tracking (USPS API), email (SendGrid), and policyholder portal updates with premium comparison breakdowns

Connect your policy administration and claims systems and the renewal pipeline runs without structural changes.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):

## Project Structure

```
insurance-renewal-insurance-renewal/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/insurancerenewal/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── InsuranceRenewalExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── NotifyWorker.java
│       ├── ProcessCancelWorker.java
│       ├── ProcessRenewWorker.java
│       ├── RepriceWorker.java
│       └── ReviewWorker.java
└── src/test/java/insurancerenewal/workers/
    ├── RepriceWorkerTest.java        # 1 tests
    └── ReviewWorkerTest.java        # 1 tests

```
