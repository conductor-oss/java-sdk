# Prescription Workflow in Java Using Conductor :  Verification, Interaction Checking, Filling, Dispensing, and Adherence Tracking

A Java Conductor workflow example for prescription processing. verifying the prescription and pulling the patient's current medication list, checking for drug-drug interactions, filling the prescription at the pharmacy, dispensing to the patient, and tracking refills and adherence. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## The Problem

You need to process prescriptions from the point a provider writes the order through dispensing and ongoing adherence monitoring. The prescription must first be verified. confirming the prescriber's DEA number, the patient's identity, and the medication's formulary status. The patient's current medication list must be pulled and the new drug checked for interactions, contraindications, and duplicate therapy. Once cleared, the prescription is filled,  the correct medication, strength, and quantity are prepared. The filled prescription is dispensed to the patient with counseling instructions. Finally, the prescription must be tracked for refill timing and adherence (medication possession ratio). A missed interaction check or dispensing error can cause serious patient harm.

Without orchestration, you'd build a monolithic pharmacy system that validates the Rx, queries the interaction database, updates the fill record, prints the label, and schedules refill reminders. If the interaction database is temporarily unavailable, the pharmacist cannot verify safety. If the system crashes after filling but before dispensing, the medication is prepared but the patient record is not updated. State pharmacy boards and DEA require complete dispensing records for every controlled substance.

## The Solution

**You just write the prescription workers. Rx verification, interaction checking, filling, dispensing, and adherence tracking. Conductor handles strict safety sequencing, automatic retries when the interaction database is unavailable, and complete dispensing records for pharmacy board and DEA compliance.**

Each stage of the prescription lifecycle is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of verifying before checking interactions, filling only after safety checks pass, dispensing only after filling is complete, activating adherence tracking as the final step, and maintaining a complete dispensing record for pharmacy board and DEA compliance. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Five workers manage the prescription lifecycle: VerifyWorker validates prescriber credentials and formulary status, CheckInteractionsWorker screens for drug-drug conflicts, FillWorker prepares the medication, DispenseWorker records the dispensing event, and TrackWorker monitors refill adherence.

| Worker | Task | What It Does |
|---|---|---|
| **VerifyWorker** | `prx_verify` | Validates the prescription (prescriber credentials, patient identity, formulary status) and retrieves current medications |
| **CheckInteractionsWorker** | `prx_check_interactions` | Checks the new medication against the patient's current medications for drug-drug interactions and contraindications |
| **FillWorker** | `prx_fill` | Fills the prescription. selects the correct NDC, verifies strength and quantity, generates the label |
| **DispenseWorker** | `prx_dispense` | Records the dispensing event, updates the patient's medication profile, and documents counseling |
| **TrackWorker** | `prx_track` | Monitors refill timing, calculates medication possession ratio (MPR), and schedules refill reminders |

Workers implement clinical and administrative operations with realistic outputs so you can see the care workflow end-to-end. Replace with real EHR and system integrations. the workflow and compliance logic stay the same.

### The Workflow

```
Input -> CheckInteractionsWorker -> DispenseWorker -> FillWorker -> TrackWorker -> VerifyWorker -> Output

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
java -jar target/prescription-workflow-1.0.0.jar

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
java -jar target/prescription-workflow-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow prescription_workflow \
  --version 1 \
  --input '{"prescriptionId": "TEST-001", "patientId": "TEST-001", "medication": "sample-medication", "dosage": "sample-dosage"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w prescription_workflow -s COMPLETED -c 5

```

## How to Extend

Connect VerifyWorker to your pharmacy management system and PDMP, CheckInteractionsWorker to a drug database (First Databank), and FillWorker to your dispensing system for label generation. The workflow definition stays exactly the same.

- **VerifyWorker** → query your state PDMP (Prescription Drug Monitoring Program) and validate prescriber DEA credentials in real time
- **CheckInteractionsWorker** → call First Databank or Medi-Span for real drug-drug, drug-allergy, and duplicate therapy checks
- **FillWorker** → integrate with your pharmacy dispensing robot or manual fill workflow with NDC barcode verification
- **DispenseWorker** → record the dispensing event in your pharmacy management system and generate NCPDP D.0 transactions
- **TrackWorker** → calculate real MPR/PDC adherence metrics and trigger automated refill reminders via IVR or SMS
- Add a **PriorAuthWorker** with a SWITCH on formulary status to initiate prior authorization for non-formulary medications

Swap in your pharmacy management system and drug interaction database while maintaining the same output contract, and the prescription workflow requires no modifications.

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
prescription-workflow/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/prescriptionworkflow/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── PrescriptionWorkflowExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CheckInteractionsWorker.java
│       ├── DispenseWorker.java
│       ├── FillWorker.java
│       ├── TrackWorker.java
│       └── VerifyWorker.java

```
