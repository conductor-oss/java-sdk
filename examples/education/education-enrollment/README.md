# Education Enrollment in Java with Conductor :  Application, Review, Admission, Registration, and Orientation

A Java Conductor workflow example for student enrollment .  accepting an application, reviewing academic qualifications (GPA-based scoring), making an admission decision, registering the admitted student in their program, and scheduling their orientation session. Uses [Conductor](https://github.## The Problem

You need to process new student enrollments from application to orientation. A prospective student submits an application for a program, the admissions office reviews their academic record and assigns a score based on GPA and other criteria, an admission decision is made, the admitted student is registered in the program, and finally an orientation session is scheduled. Each step depends on the previous .  you cannot admit without a review score, and you cannot enroll without an admission decision.

Without orchestration, you'd build a single enrollment service that receives applications, queries GPA records, runs admission logic, inserts enrollment records, and emails orientation details .  manually handling failures when the student information system is down, retrying rejected database writes, and logging every step to audit why an applicant was enrolled without completing review.

## The Solution

**You just write the application intake, academic review, admission decision, registration, and orientation scheduling logic. Conductor handles eligibility retries, seat assignment sequencing, and enrollment audit trails.**

Each enrollment concern is a simple, independent worker .  a plain Java class that does one thing. Conductor takes care of executing them in order (apply, review, admit, enroll, orient), retrying if the student information system times out, tracking every application's full journey from submission to orientation, and resuming from the last successful step if the process crashes. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Application intake, eligibility check, seat assignment, and confirmation workers process enrollments as a sequence of independent validations.

| Worker | Task | What It Does |
|---|---|---|
| **ApplyWorker** | `edu_apply` | Receives the student's application with name, program choice, and GPA |
| **ReviewWorker** | `edu_review` | Reviews the application and assigns a score based on GPA and program criteria |
| **AdmitWorker** | `edu_admit` | Makes the admission decision based on the review score |
| **EnrollWorker** | `edu_enroll` | Registers the admitted student in their chosen program |
| **OrientWorker** | `edu_orient` | Schedules an orientation session for the newly enrolled student |

Workers simulate educational operations .  enrollment, grading, notifications ,  with realistic outputs. Replace with real LMS and SIS integrations and the workflow stays the same.

### The Workflow

```
edu_apply
    │
    ▼
edu_review
    │
    ▼
edu_admit
    │
    ▼
edu_enroll
    │
    ▼
edu_orient
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
java -jar target/education-enrollment-1.0.0.jar
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
java -jar target/education-enrollment-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow edu_enrollment \
  --version 1 \
  --input '{"studentName": "test", "program": "test-value", "gpa": "test-value"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w edu_enrollment -s COMPLETED -c 5
```

## How to Extend

Connect each worker to your real admissions stack .  your CRM (Slate, Ellucian) for applications, the National Student Clearinghouse for transcript verification, your SIS for registration, and the workflow runs identically in production.

- **ApplyWorker** (`edu_apply`): persist the application to your admissions database or CRM (Slate, Ellucian CRM Recruit) and trigger document collection
- **ReviewWorker** (`edu_review`): pull transcripts from the National Student Clearinghouse, run GPA validation against program thresholds, and flag applications for committee review
- **AdmitWorker** (`edu_admit`): execute admission rules from your policy engine and generate acceptance/rejection letters via your document service
- **EnrollWorker** (`edu_enroll`): register the student in your SIS (Banner, PeopleSoft) with program codes, create their student account, and assign an advisor
- **OrientWorker** (`edu_orient`): schedule orientation via your event management system and send calendar invites with pre-arrival checklists

Update eligibility rules or integrate a new student information system and the enrollment pipeline remains unchanged.

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
education-enrollment/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/educationenrollment/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── EducationEnrollmentExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AdmitWorker.java
│       ├── ApplyWorker.java
│       ├── EnrollWorker.java
│       ├── OrientWorker.java
│       └── ReviewWorker.java
└── src/test/java/educationenrollment/workers/
    ├── AdmitWorkerTest.java        # 5 tests
    ├── ApplyWorkerTest.java        # 4 tests
    ├── EnrollWorkerTest.java        # 4 tests
    ├── OrientWorkerTest.java        # 4 tests
    └── ReviewWorkerTest.java        # 6 tests
```
