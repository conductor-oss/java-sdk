# Customer Churn in Java Using Conductor

A Java Conductor workflow example that orchestrates customer churn prevention .  detecting at-risk subscribers based on usage trend decline, analyzing the reasons behind the churn risk (pricing, coverage, service quality), creating a personalized retention offer based on the identified reasons and account tenure, delivering the offer to the customer, and tracking whether the customer accepts and stays. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers ,  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## Why Churn Prevention Needs Orchestration

Retaining at-risk customers requires a time-sensitive pipeline from detection through outcome tracking. You detect churn risk by analyzing usage trends .  declining call minutes, reduced data consumption, or increased complaint frequency signal a customer is likely to leave. You analyze the reasons behind the risk ,  is it pricing, network quality in their area, a recent bad support experience, or a competitor offer? You create a personalized retention offer based on the identified reasons and the customer's account age ,  a discount, a plan upgrade, bonus data, or a device credit. You deliver the offer via the most effective channel for that customer. Finally, you track whether the offer was accepted and the customer was retained.

If the offer is created but delivery fails, a perfectly good retention offer never reaches the customer and they churn. If detection flags a customer but reason analysis stalls, the retention team misses the intervention window. Without orchestration, you'd build a batch churn-scoring script that dumps results into a spreadsheet for manual follow-up .  making it impossible to personalize offers at scale, track which offers actually prevent churn, or close the loop between the churn model and retention outcomes.

## The Solution

**You just write the churn detection, reason analysis, retention offer creation, offer delivery, and outcome tracking logic. Conductor handles scoring retries, offer routing, and retention campaign audit trails.**

Each worker handles one telecom operation. Conductor manages the provisioning pipeline, activation sequencing, billing triggers, and service state tracking.

### What You Write: Workers

Risk scoring, retention offer generation, outreach execution, and outcome tracking workers each tackle one stage of reducing customer attrition.

| Worker | Task | What It Does |
|---|---|---|
| **AnalyzeReasonsWorker** | `ccn_analyze_reasons` | Analyzes the reasons behind the churn risk .  pricing, coverage, service quality, or competitor offers. |
| **CreateOfferWorker** | `ccn_create_offer` | Creates a personalized retention offer based on the identified churn reasons and account tenure. |
| **DeliverWorker** | `ccn_deliver` | Delivers the retention offer to the customer via their preferred channel (SMS, email, in-app, call center). |
| **DetectRiskWorker** | `ccn_detect_risk` | Detects churn risk by scoring the customer based on usage trend decline and behavioral signals. |
| **TrackWorker** | `ccn_track` | Tracks whether the customer accepted the offer and was retained or still churned. |

Workers simulate telecom operations .  provisioning, activation, billing ,  with realistic outputs. Replace with real OSS/BSS integrations and the workflow stays the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
ccn_detect_risk
    │
    ▼
ccn_analyze_reasons
    │
    ▼
ccn_create_offer
    │
    ▼
ccn_deliver
    │
    ▼
ccn_track
```

## Example Output

```
=== Example 812: Customer Chur ===

Step 1: Registering task definitions...
  Registered: ccn_detect_risk, ccn_analyze_reasons, ccn_create_offer, ccn_deliver, ccn_track

Step 2: Registering workflow 'ccn_customer_churn'...
  Workflow registered.

Step 3: Starting workers...
  5 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [analyze] Reasons: price sensitivity, competitor offers, declining usage
  [offer] Retention offer: 20%% discount for 6 months
  [deliver] Processing
  [detect_risk] Processing
  [track] Processing

  Status: COMPLETED
  Output: {reasons=..., primaryReason=..., offerId=..., offer=...}

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
java -jar target/customer-churn-1.0.0.jar
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
java -jar target/customer-churn-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow ccn_customer_churn \
  --version 1 \
  --input '{"customerId": "CUST-812", "CUST-812": "accountAge", "accountAge": 36, "declining": "sample-declining"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w ccn_customer_churn -s COMPLETED -c 5
```

## How to Extend

Connect each worker to your real retention systems .  your analytics platform for churn scoring, your CRM for account analysis, your marketing automation for offer delivery and tracking, and the workflow runs identically in production.

- **DetectRiskWorker** (`ccn_detect_risk`): run your churn prediction model (scikit-learn, TensorFlow Serving, or SAS) against the customer's usage data from your data warehouse or CRM analytics platform
- **AnalyzeReasonsWorker** (`ccn_analyze_reasons`): correlate the risk score with data from your CRM (Salesforce, Amdocs), NPS survey results, network quality KPIs for the customer's area, and competitor pricing intelligence
- **CreateOfferWorker** (`ccn_create_offer`): generate the retention offer using your campaign management system (Adobe Campaign, Pega, Salesforce Marketing Cloud) with offer rules tuned to the churn reason and customer segment
- **DeliverWorker** (`ccn_deliver`): deliver the offer via your omnichannel engagement platform. SMS gateway, email (SendGrid), push notification, or queue it for the next call center interaction via your CTI system
- **TrackWorker** (`ccn_track`): monitor offer acceptance by tracking the customer's subsequent behavior in your CRM and billing system .  checking for plan changes, renewed usage, or port-out requests

Swap scoring models or retention offer engines and the churn pipeline adapts without restructuring.

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
customer-churn-customer-churn/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/customerchurn/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── CustomerChurnExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AnalyzeReasonsWorker.java
│       ├── CreateOfferWorker.java
│       ├── DeliverWorker.java
│       ├── DetectRiskWorker.java
│       └── TrackWorker.java
└── src/test/java/customerchurn/workers/
    ├── DetectRiskWorkerTest.java        # 1 tests
    └── TrackWorkerTest.java        # 1 tests
```
