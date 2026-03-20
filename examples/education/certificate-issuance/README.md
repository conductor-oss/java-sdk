# Certificate Issuance in Java with Conductor :  Completion Verification, Generation, Signing, Delivery, and Record-Keeping

A Java Conductor workflow example for issuing educational certificates .  verifying that a student completed all course requirements, generating a personalized certificate document, digitally signing it for authenticity, delivering it to the student, and recording the credential in the institution's permanent records. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers ,  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## The Problem

You need to issue certificates when students complete a course. Before generating anything, you must verify the student actually finished all required coursework, assignments, and exams. Then you generate a certificate with the student's name, course title, and completion date, digitally sign it so it cannot be forged, deliver it to the student, and record the issued credential in the registrar's system. Issuing a certificate without verified completion is an institutional liability; losing the record makes the credential unverifiable.

Without orchestration, you'd embed completion checks, PDF generation, digital signing, email delivery, and database writes in a single service .  manually ensuring a certificate is never generated without verified completion, retrying failed email sends, and logging every step to prove the audit trail when an employer verifies a credential years later.

## The Solution

**You just write the completion verification, certificate generation, digital signing, delivery, and record-keeping logic. Conductor handles signing retries, delivery tracking, and credential issuance audit trails.**

Each credential concern is a simple, independent worker .  a plain Java class that does one thing. Conductor takes care of executing them in order (verify, generate, sign, issue, record), retrying if the signing service or email delivery times out, maintaining a complete audit trail of every certificate's lifecycle, and resuming from the last step if the process crashes after signing but before delivery. You get all of that for free, without writing a single line of orchestration code.

### What You Write: Workers

Eligibility verification, certificate generation, digital signing, and delivery workers each handle one step of credential issuance.

| Worker | Task | What It Does |
|---|---|---|
| **VerifyCompletionWorker** | `cer_verify_completion` | Checks that the student has completed all course requirements (assignments, exams, attendance) |
| **GenerateCertificateWorker** | `cer_generate` | Creates the certificate document with the student's name, course title, and completion date |
| **SignCertificateWorker** | `cer_sign` | Digitally signs the certificate to ensure authenticity and prevent forgery |
| **IssueCertificateWorker** | `cer_issue` | Delivers the signed certificate to the student (email, portal download) |
| **RecordCertificateWorker** | `cer_record` | Records the issued credential in the institution's registrar system |

Workers simulate educational operations .  enrollment, grading, notifications ,  with realistic outputs. Replace with real LMS and SIS integrations and the workflow stays the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
cer_verify_completion
    │
    ▼
cer_generate
    │
    ▼
cer_sign
    │
    ▼
cer_issue
    │
    ▼
cer_record
```

## Example Output

```
=== Example 674: Certificate Issuance ===

Step 1: Registering task definitions...
  Registered: cer_verify_completion, cer_generate, cer_sign, cer_issue, cer_record

Step 2: Registering workflow 'cer_certificate_issuance'...
  Workflow registered.

Step 3: Starting workers...
  5 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [generate] Certificate for
  [issue]
  [record]
  [sign]
  [verify] Student

  Status: COMPLETED
  Output: {certificateId=..., format=..., issued=..., deliveryMethod=...}

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
java -jar target/certificate-issuance-1.0.0.jar
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
java -jar target/certificate-issuance-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow cer_certificate_issuance \
  --version 1 \
  --input '{"studentId": "STU-2024-674", "STU-2024-674": "courseId", "courseId": "CS-301", "CS-301": "courseName", "courseName": "Machine Learning Fundamentals", "Machine Learning Fundamentals": "sample-Machine Learning Fundamentals"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w cer_certificate_issuance -s COMPLETED -c 5
```

## How to Extend

Wire each worker to your real credentialing stack .  your LMS for completion verification, a PDF renderer (iText, Accredible) for certificate generation, your institution's PKI for digital signing, and the workflow runs identically in production.

- **VerifyCompletionWorker** (`cer_verify_completion`): query your LMS (Canvas, Blackboard, Moodle) or student information system to verify all assignments, exams, and attendance requirements are met
- **GenerateCertificateWorker** (`cer_generate`): render a PDF certificate using a template engine (Apache PDFBox, iText, or a service like Accredible/Certifier)
- **SignCertificateWorker** (`cer_sign`): apply a digital signature using your institution's PKI infrastructure or a signing service (DocuSign, Adobe Sign)
- **IssueCertificateWorker** (`cer_issue`): email the certificate to the student via SendGrid/SES, or make it available for download in the student portal
- **RecordCertificateWorker** (`cer_record`): write the credential to your registrar database and optionally publish to a blockchain-based verification ledger (Blockcerts, Credly)

Change your certificate template or signing authority and the issuance pipeline keeps its structure.

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
certificate-issuance/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/certificateissuance/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── CertificateIssuanceExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── GenerateCertificateWorker.java
│       ├── IssueCertificateWorker.java
│       ├── RecordCertificateWorker.java
│       ├── SignCertificateWorker.java
│       └── VerifyCompletionWorker.java
└── src/test/java/certificateissuance/workers/
    ├── GenerateCertificateWorkerTest.java        # 2 tests
    ├── IssueCertificateWorkerTest.java        # 2 tests
    ├── RecordCertificateWorkerTest.java        # 2 tests
    ├── SignCertificateWorkerTest.java        # 2 tests
    └── VerifyCompletionWorkerTest.java        # 2 tests
```
