# Training Management in Java with Conductor :  Course Assignment, Progress Tracking, Assessment, Certification, and Record Keeping

A Java Conductor workflow example for employee training management .  assigning a course to an employee with a due date, tracking their progress through the learning material, administering the final assessment, issuing a certification if they pass, and recording the completed training in their permanent employee record. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers ,  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## The Problem

You need to manage employee training from assignment through certification. An employee is assigned to a course .  whether mandatory compliance training (OSHA, HIPAA, anti-harassment), role-specific skills training, or professional development. The system must track the employee's progress through the course modules and verify completion. Upon finishing the material, the employee takes an assessment; a passing score (e.g., 80%+) earns a certification with an issue date and expiration. The certification must be recorded in the employee's permanent training record for audit purposes. Each step depends on the previous ,  you cannot assess without tracking completion, and you cannot certify without a passing assessment score. Missed compliance training exposes the organization to regulatory fines and liability.

Without orchestration, you'd manage training through spreadsheets and LMS reports. HR assigns courses, manually checks who completed them, reviews assessment results, and updates certification records one by one. If the LMS is down when recording certifications, the employee completed the training but has no record of it. If someone completes a course but the assessment results are not linked to the certification, they may be counted as non-compliant. Auditors for OSHA, HIPAA, and SOX compliance require proof that every employee completed their required training within the mandated timeframe.

## The Solution

**You just write the course assignment, progress tracking, assessment administration, certification, and record keeping logic. Conductor handles enrollment retries, progress tracking, and certification audit trails.**

Each stage of training management is a simple, independent worker .  a plain Java class that does one thing. Conductor takes care of tracking progress only after assignment, assessing only after course completion, certifying only if the assessment score meets the passing threshold, recording the certification as the final step, retrying if the LMS or HRIS is temporarily unavailable, and maintaining a complete audit trail for compliance reporting. You get all of that for free, without writing a single line of orchestration code.

### What You Write: Workers

Course assignment, enrollment, progress tracking, and certification workers each manage one phase of employee skill development.

| Worker | Task | What It Does |
|---|---|---|
| **AssignWorker** | `trm_assign` | Enrolls the employee in the course, generates an enrollment ID, and sets a completion due date |
| **TrackWorker** | `trm_track` | Monitors the employee's progress through course modules and verifies they have completed all required content |
| **AssessWorker** | `trm_assess` | Administers the final assessment and returns the score, tracking which questions were answered correctly |
| **CertifyWorker** | `trm_certify` | Issues a certification based on the assessment score, with an issue date and expiration date for recertification tracking |
| **RecordWorker** | `trm_record` | Records the completed certification in the employee's permanent training record in the HRIS for compliance audits |

Workers simulate HR operations .  onboarding tasks, approvals, provisioning ,  with realistic outputs. Replace with real HRIS and identity provider integrations and the workflow stays the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
trm_assign
    │
    ▼
trm_track
    │
    ▼
trm_assess
    │
    ▼
trm_certify
    │
    ▼
trm_record
```

## Example Output

```
=== Example 700: Training Management ===

Step 1: Registering task definitions...
  Registered: trm_assign, trm_track, trm_assess, trm_certify, trm_record

Step 2: Registering workflow 'trm_training_management'...
  Workflow registered.

Step 3: Starting workers...
  5 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [assess] Assessment completed: score 92/100
  [assign]
  [certify] Certification issued to
  [record] Certification
  [track] Enrollment

  Status: COMPLETED
  Output: {score=..., passed=..., passingScore=..., enrollmentId=...}

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
java -jar target/training-management-1.0.0.jar
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
java -jar target/training-management-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow trm_training_management \
  --version 1 \
  --input '{"employeeId": "EMP-700", "EMP-700": "courseId", "courseId": "CRS-SEC-101", "CRS-SEC-101": "courseName", "courseName": "Security Awareness", "Security Awareness": "sample-Security Awareness"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w trm_training_management -s COMPLETED -c 5
```

## How to Extend

Point each worker at your real training systems .  your LMS for course assignment and progress tracking, your assessment platform for testing, your HRIS for certification records, and the workflow runs identically in production.

- **AssignWorker** → enroll employees in real LMS courses (Cornerstone, Docebo, Lessonly) with automatic assignment based on role, department, or compliance requirements
- **TrackWorker** → poll your LMS for real-time SCORM/xAPI completion data, send reminder emails for approaching due dates, and escalate overdue courses to the employee's manager
- **AssessWorker** → administer real assessments through your LMS or testing platform, supporting multiple question types, time limits, and proctoring for certification exams
- **CertifyWorker** → issue digital certifications with QR-code verification, track expiration dates, and automatically re-enroll employees before certifications lapse
- **RecordWorker** → write completed training records to your HRIS with SCORM completion data for OSHA, HIPAA, SOX, and industry-specific compliance audits
- Add a **RemedialWorker** with a SWITCH on assessment score to assign remedial training for employees who score below the passing threshold before allowing a retake
- Add a **ExpirationWorker** to monitor certification expiration dates and automatically trigger recertification workflows 30/60/90 days before expiry

Connect a different LMS or certification provider and the training pipeline runs identically.

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
training-management-training-management/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/trainingmanagement/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── TrainingManagementExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AssessWorker.java
│       ├── AssignWorker.java
│       ├── CertifyWorker.java
│       ├── RecordWorker.java
│       └── TrackWorker.java
└── src/test/java/trainingmanagement/workers/
    ├── AssessWorkerTest.java        # 2 tests
    ├── AssignWorkerTest.java        # 2 tests
    ├── CertifyWorkerTest.java        # 2 tests
    ├── RecordWorkerTest.java        # 2 tests
    └── TrackWorkerTest.java        # 2 tests
```
