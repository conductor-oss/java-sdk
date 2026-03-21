# Drug Interaction Checking in Java Using Conductor :  Medication List, Pairwise Checks, Conflict Flagging, and Alternative Recommendations

A Java Conductor workflow example for drug interaction checking. pulling a patient's current medication list, checking every drug pair for interactions, flagging clinically significant conflicts by severity, and recommending safer therapeutic alternatives. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## The Problem

You need to check for drug-drug interactions when a new medication is prescribed. The patient's complete active medication list must be retrieved from the EHR. Every pair of the new medication against existing medications must be checked against an interaction database for contraindications, dose adjustments, and monitoring requirements. Clinically significant conflicts must be flagged with severity levels (minor, moderate, major, contraindicated). For flagged interactions, the system must recommend alternative medications in the same therapeutic class that do not conflict. A missed major interaction. like prescribing warfarin with a CYP2C9 inhibitor,  can cause life-threatening adverse events.

Without orchestration, you'd build a monolithic prescribing safety service that queries the medication list, runs pairwise lookups against the interaction database, formats the conflict alerts, and generates alternatives. all in a single request. If the drug interaction database is temporarily unavailable, the prescriber gets no safety check at all. If the system crashes after finding conflicts but before presenting alternatives, the clinician sees a warning with no actionable guidance. Every interaction check must be logged for medication safety audits.

## The Solution

**You just write the drug safety workers. Medication list retrieval, pairwise interaction checks, conflict flagging, and alternative recommendation. Conductor handles sequential evaluation, automatic retries when the interaction database is unavailable, and logged records of every safety check for medication audits.**

Each stage of the interaction check is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of listing medications before checking pairs, flagging conflicts only after all pairs are evaluated, generating alternative recommendations only for flagged conflicts, and logging every check for medication safety audits. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Four workers form the drug safety pipeline: ListMedicationsWorker retrieves active medications, CheckPairsWorker evaluates pairwise interactions, FlagConflictsWorker categorizes severity, and RecommendAlternativesWorker suggests safer therapeutic options.

| Worker | Task | What It Does |
|---|---|---|
| **ListMedicationsWorker** | `drg_list_medications` | Retrieves the patient's complete active medication list (drug name, dose, frequency, route) |
| **CheckPairsWorker** | `drg_check_pairs` | Evaluates every pairwise combination of the new medication against existing medications for interactions |
| **FlagConflictsWorker** | `drg_flag_conflicts` | Categorizes detected interactions by clinical severity (minor, moderate, major, contraindicated) |
| **RecommendAlternativesWorker** | `drg_recommend_alternatives` | Suggests alternative medications in the same therapeutic class that avoid the flagged interactions |

Workers implement clinical and administrative operations with realistic outputs so you can see the care workflow end-to-end. Replace with real EHR and system integrations. the workflow and compliance logic stay the same.

### The Workflow

```
drg_list_medications
    │
    ▼
drg_check_pairs
    │
    ▼
drg_flag_conflicts
    │
    ▼
drg_recommend_alternatives

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
java -jar target/drug-interaction-1.0.0.jar

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
java -jar target/drug-interaction-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow drug_interaction_workflow \
  --version 1 \
  --input '{"patientId": "TEST-001", "newMedication": "sample-newMedication"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w drug_interaction_workflow -s COMPLETED -c 5

```

## How to Extend

Connect ListMedicationsWorker to your EHR's FHIR MedicationRequest API, CheckPairsWorker to a drug interaction database (First Databank, Medi-Span), and RecommendAlternativesWorker to your formulary. The workflow definition stays exactly the same.

- **ListMedicationsWorker** → pull real medication lists via FHIR MedicationRequest or your EHR's medication reconciliation API
- **CheckPairsWorker** → query a commercial drug interaction database (First Databank FDB, Medi-Span, DrugBank API) for real interaction data
- **FlagConflictsWorker** → apply your organization's clinical severity rules, including dose-dependent interactions and patient-specific factors (renal function, age)
- **RecommendAlternativesWorker** → query your formulary for covered therapeutic equivalents using GPI or ATC classification codes
- Add a **AllergyCheckWorker** to cross-reference the new medication against the patient's documented allergies before pair checking
- Add a **PharmacistReviewWorker** as a human-in-the-loop step for contraindicated interactions that require pharmacist override

Replace demo lookups with real drug interaction databases and formulary APIs while returning the same fields, and the safety checking pipeline needs no workflow changes.

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
drug-interaction/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/druginteraction/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── DrugInteractionExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CheckPairsWorker.java
│       ├── FlagConflictsWorker.java
│       ├── ListMedicationsWorker.java
│       └── RecommendAlternativesWorker.java
└── src/test/java/druginteraction/workers/
    ├── CheckPairsWorkerTest.java        # 2 tests
    ├── FlagConflictsWorkerTest.java        # 2 tests
    ├── ListMedicationsWorkerTest.java        # 2 tests
    └── RecommendAlternativesWorkerTest.java        # 2 tests

```
