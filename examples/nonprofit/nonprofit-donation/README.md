# Nonprofit Donation in Java with Conductor

A Java Conductor workflow example demonstrating Nonprofit Donation. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers .## The Problem

A donor submits a donation through your nonprofit's website. The donation processing team needs to receive and validate the donation details, process the payment and obtain a transaction ID, generate a tax-deductible receipt, send a personalized thank-you message tied to the campaign, and record the completed donation in the donor database. Each step depends on the previous one's output.

Without orchestration, you'd wire all of this together in a single monolithic class .  managing execution order manually, writing try/catch blocks around every step, building retry loops with backoff, and adding logging to understand what happened when things go wrong. That code becomes brittle, hard to test, and impossible to observe at scale.

## The Solution

**You just write the donation acceptance, payment processing, receipt generation, and fund allocation logic. Conductor handles payment retries, receipt generation, and donation audit trails.**

Each worker handles one nonprofit operation. Conductor manages the donation pipeline, campaign sequencing, receipt generation, and reporting.

### What You Write: Workers

Gift intake, payment processing, receipt generation, and donor acknowledgment workers each handle one step of the donation acceptance flow.

| Worker | Task | What It Does |
|---|---|---|
| **ProcessPaymentWorker** | `don_process_payment` | Processes the donation payment for the specified amount, returning a transaction ID |
| **ReceiptWorker** | `don_receipt` | Generates and sends a tax-deductible receipt to the donor, returning a receipt ID |
| **ReceiveWorker** | `don_receive` | Validates the incoming donation from the donor, recording the amount and assigning a donation ID |
| **RecordWorker** | `don_record` | Records the completed donation in the donor database with donor name, amount, and transaction ID |
| **ThankYouWorker** | `don_thank_you` | Sends a personalized thank-you message to the donor tied to the specific campaign |

Workers simulate nonprofit operations .  donor processing, campaign management, reporting ,  with realistic outputs. Replace with real CRM and payment integrations and the workflow stays the same.

### The Workflow

```
don_receive
    │
    ▼
don_process_payment
    │
    ▼
don_receipt
    │
    ▼
don_thank_you
    │
    ▼
don_record
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
java -jar target/nonprofit-donation-1.0.0.jar
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
java -jar target/nonprofit-donation-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow nonprofit_donation_751 \
  --version 1 \
  --input '{"donorName": "test", "amount": 100, "campaign": "test-value"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w nonprofit_donation_751 -s COMPLETED -c 5
```

## How to Extend

Point each worker at your real donation stack. Stripe or PayPal for payment processing, your CRM for donor records, your email service for tax receipts, and the workflow runs identically in production.

- **ReceiveWorker** (`don_receive`): validate the donation against your donation form rules and create the initial record in Salesforce NPSP or DonorPerfect
- **ProcessPaymentWorker** (`don_process_payment`): charge the donor's payment method via Stripe, PayPal Giving Fund, or your payment gateway, returning the transaction ID
- **ReceiptWorker** (`don_receipt`): generate the IRS-compliant tax receipt using a template engine and email it to the donor via SendGrid or your CRM's email integration
- **ThankYouWorker** (`don_thank_you`): send a personalized thank-you email using campaign-specific templates from Mailchimp, Bloomerang, or your CRM
- **RecordWorker** (`don_record`): update the donation record in your CRM (Salesforce NPSP, DonorPerfect) with the payment transaction ID and final status

Replace your payment gateway or receipt generator and the donation pipeline stays intact.

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
nonprofit-donation/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/nonprofitdonation/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── NonprofitDonationExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ProcessPaymentWorker.java
│       ├── ReceiptWorker.java
│       ├── ReceiveWorker.java
│       ├── RecordWorker.java
│       └── ThankYouWorker.java
└── src/test/java/nonprofitdonation/workers/
    ├── ReceiveWorkerTest.java        # 1 tests
    └── RecordWorkerTest.java        # 1 tests
```
