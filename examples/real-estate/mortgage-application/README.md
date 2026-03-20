# Mortgage Application in Java with Conductor :  Apply, Credit Check, Underwriting, Approval, and Closing

A Java Conductor workflow example for processing mortgage applications .  accepting the application, running a credit check, performing underwriting analysis against loan-to-value ratios, issuing an approval decision, and closing the loan. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers ,  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## The Problem

You need to process mortgage applications from submission to closing. An applicant requests a loan .  the application must be logged, a credit check must be run to pull their score, underwriting must evaluate the risk by comparing credit score, loan amount, and property value (LTV ratio), an approval or denial decision must be issued, and approved loans must proceed to closing with final documentation. Each step feeds into the next: underwriting can't start without the credit score, approval can't happen without the underwriting assessment.

Without orchestration, mortgage processing is a manual pipeline prone to bottlenecks. Loan officers email underwriters, credit checks are requested via phone, and applications sit in queues for days. A monolithic script that tries to automate this breaks when the credit bureau API times out, and nobody knows whether the underwriting step ran or not. Regulators require an audit trail of every decision, and reconstructing one from logs is a nightmare.

## The Solution

**You just write the application intake, credit check, underwriting analysis, approval decision, and loan closing logic. Conductor handles credit check retries, underwriting sequencing, and application audit trails.**

Each mortgage processing step is a simple, independent worker .  one logs the application, one pulls the credit score, one performs underwriting analysis, one issues the approval, one handles closing. Conductor takes care of executing them in strict order, retrying if the credit bureau API is temporarily unavailable, and maintaining a complete audit trail of every decision point for regulatory compliance. You get all of that for free, without writing a single line of orchestration code.

### What You Write: Workers

Application intake, credit evaluation, underwriting, and closing workers each handle one stage of the mortgage approval process.

| Worker | Task | What It Does |
|---|---|---|
| **ApplyWorker** | `mtg_apply` | Logs the mortgage application with applicant details and requested loan amount |
| **CreditCheckWorker** | `mtg_credit_check` | Pulls the applicant's credit score and credit history from a bureau (Equifax, Experian, TransUnion) |
| **UnderwriteWorker** | `mtg_underwrite` | Evaluates loan risk using credit score, loan-to-value ratio, and debt-to-income analysis |
| **ApproveWorker** | `mtg_approve` | Issues the approval or denial decision based on underwriting results, assigns a loan ID |
| **CloseWorker** | `mtg_close` | Finalizes the loan .  generates closing documents, records the mortgage, and disburses funds |

Workers simulate property transaction steps .  listing, inspection, escrow, closing ,  with realistic outputs. Replace with real MLS and escrow service integrations and the workflow stays the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
mtg_apply
    │
    ▼
mtg_credit_check
    │
    ▼
mtg_underwrite
    │
    ▼
mtg_approve
    │
    ▼
mtg_close
```

## Example Output

```
=== Example 683: Mortgage Applicatio ===

Step 1: Registering task definitions...
  Registered: mtg_apply, mtg_credit_check, mtg_underwrite, mtg_approve, mtg_close

Step 2: Registering workflow 'mtg_mortgage_application'...
  Workflow registered.

Step 3: Starting workers...
  5 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [mtg_apply] Executing
  [mtg_approve] Executing
  [mtg_close] Executing
  [mtg_credit_check] Executing
  [mtg_underwrite] Executing

  Status: COMPLETED
  Output: {applicationId=..., loanId=..., closingStatus=..., creditScore=...}

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
java -jar target/mortgage-application-1.0.0.jar
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
java -jar target/mortgage-application-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow mtg_mortgage_application \
  --version 1 \
  --input '{"applicantId": "APP-300", "APP-300": "loanAmount", "loanAmount": 300000}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w mtg_mortgage_application -s COMPLETED -c 5
```

## How to Extend

Wire each worker to your real lending systems. Equifax or Experian for credit pulls, your underwriting engine for risk assessment, your loan origination system for closing, and the workflow runs identically in production.

- **ApplyWorker** (`mtg_apply`): accept applications from your loan origination system (Encompass, Calyx) or a consumer-facing web portal
- **CreditCheckWorker** (`mtg_credit_check`): integrate with Equifax/Experian/TransUnion APIs for real credit pulls, or use a soft-pull service like Credit Karma for pre-qualification
- **UnderwriteWorker** (`mtg_underwrite`): implement automated underwriting rules (Fannie Mae DU, Freddie Mac LP) or route to a HUMAN task for manual underwriter review
- **ApproveWorker** (`mtg_approve`): generate the approval letter, set the interest rate based on risk tier, and send the decision to the applicant via email
- **CloseWorker** (`mtg_close`): generate closing disclosure documents, integrate with a title company for closing coordination, and initiate fund disbursement

Integrate different credit bureaus or underwriting engines and the application pipeline adapts seamlessly.

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
mortgage-application/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/mortgageapplication/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── MortgageApplicationExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ApplyWorker.java
│       ├── ApproveWorker.java
│       ├── CloseWorker.java
│       ├── CreditCheckWorker.java
│       └── UnderwriteWorker.java
└── src/test/java/mortgageapplication/workers/
    ├── ApplyWorkerTest.java        # 2 tests
    ├── ApproveWorkerTest.java        # 2 tests
    ├── CloseWorkerTest.java        # 2 tests
    ├── CreditCheckWorkerTest.java        # 2 tests
    └── UnderwriteWorkerTest.java        # 2 tests
```
