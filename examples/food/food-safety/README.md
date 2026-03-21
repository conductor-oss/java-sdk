# Food Safety in Java with Conductor

Conducts a food safety inspection: visiting the facility, checking temperatures, verifying hygiene, issuing certification, and recording results. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## The Problem

You need to conduct a food safety inspection at a restaurant. An inspector visits the facility, checks temperature logs for food storage and cooking equipment, verifies hygiene practices (handwashing, cross-contamination prevention, pest control), issues a safety certification if standards are met, and records the inspection results. Certifying without thorough temperature and hygiene checks puts public health at risk.

Without orchestration, you'd manage inspections with paper checklists and spreadsheets. manually tracking which restaurants are due for inspection, recording findings in documents, issuing certificates through a separate system, and maintaining records for health department audits.

## The Solution

**You just write the facility inspection, temperature checks, hygiene verification, and certification issuance logic. Conductor handles inspection scheduling retries, compliance routing, and safety audit trails.**

Each food safety concern is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of executing them in order (inspect, check temps, verify hygiene, certify, record), tracking every inspection with timestamped evidence, and resuming from the last step if the process crashes. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Inspection scheduling, temperature logging, compliance checking, and reporting workers each enforce one layer of food safety protocols.

| Worker | Task | What It Does |
|---|---|---|
| **CertifyWorker** | `fsf_certify` | Issues a safety certification with grade, score, and validity date when all checks pass |
| **CheckTempsWorker** | `fsf_check_temps` | Checks fridge, freezer, and hot-holding temperatures at the facility and returns pass/fail for each |
| **InspectWorker** | `fsf_inspect` | Dispatches the inspector to the restaurant and records initial findings for cleanliness, equipment, and violations |
| **RecordWorker** | `fsf_record` | Records inspection results and compliance status in the food safety database |
| **VerifyHygieneWorker** | `fsf_verify_hygiene` | Verifies hygiene practices (handwashing, surface sanitation, cross-contamination prevention) and returns a score |

Workers implement food service operations. order processing, kitchen routing, delivery coordination,  with realistic outputs. Replace with real POS and delivery integrations and the workflow stays the same.

### The Workflow

```
fsf_inspect
    │
    ▼
fsf_check_temps
    │
    ▼
fsf_verify_hygiene
    │
    ▼
fsf_certify
    │
    ▼
fsf_record

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
java -jar target/food-safety-1.0.0.jar

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
java -jar target/food-safety-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow food_safety_738 \
  --version 1 \
  --input '{"restaurantId": "TEST-001", "inspectorId": "TEST-001"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w food_safety_738 -s COMPLETED -c 5

```

## How to Extend

Swap each worker for your real food safety tools. your inspection scheduling system, IoT temperature sensors for monitoring, your health department portal for certification filing, and the workflow runs identically in production.

- **Facility inspector**: pull inspection schedules from your health department system and record findings with photo evidence
- **Temperature checker**: read from IoT temperature sensors (Thermoworks, ComplianceMate) for real-time cold/hot holding verification
- **Hygiene verifier**: score against FDA Food Code standards with configurable checklists for different facility types
- **Certification issuer**: generate health inspection certificates with scores and post to public disclosure databases
- **Records keeper**: archive inspection reports in your regulatory compliance database with retention per local health codes

Update inspection checklists or compliance rules and the safety pipeline handles them without restructuring.

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
food-safety/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/foodsafety/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── FoodSafetyExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CertifyWorker.java
│       ├── CheckTempsWorker.java
│       ├── InspectWorker.java
│       ├── RecordWorker.java
│       └── VerifyHygieneWorker.java
└── src/test/java/foodsafety/workers/
    ├── InspectWorkerTest.java
    └── RecordWorkerTest.java

```
