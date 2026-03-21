# Benefits Enrollment in Java with Conductor :  Plan Presentation, Selection, Validation, Enrollment, and Confirmation

A Java Conductor workflow example for employee benefits enrollment. presenting available medical, dental, and vision plan options based on the employee's eligibility, capturing their selections, validating choices against plan rules and dependent eligibility, enrolling in the carrier systems, and sending confirmation with effective dates and premium costs. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## The Problem

You need to manage open enrollment for employee benefits. During the enrollment period, each employee must see their eligible plan options. medical (PPO, HMO, HDHP), dental (basic, premium), and vision (standard). The employee makes selections for themselves and their dependents. Those selections must be validated,  checking that chosen plans are available in the employee's region, dependents meet age and relationship eligibility rules, and HSA elections are only paired with HDHP medical plans. Validated selections are enrolled with each insurance carrier, generating a monthly premium total and an effective date. Finally, the employee receives confirmation with their benefit summary, ID card information, and payroll deduction details. If enrollment happens out of order,  selecting before seeing options, or enrolling without validating,  employees end up with ineligible plan combinations or missed coverage.

Without orchestration, you'd build a monolithic enrollment portal that queries plan catalogs, captures selections in a form, runs validation rules, calls each carrier's enrollment API, and emails a confirmation. If a carrier API is down during open enrollment crunch time, the enrollment fails and the employee may miss the deadline. If the system crashes after enrolling with the medical carrier but before dental, the employee has partial coverage. HR needs a complete audit trail of every enrollment for ERISA compliance and benefits reconciliation.

## The Solution

**You just write the plan presentation, selection capture, eligibility validation, carrier enrollment, and confirmation logic. Conductor handles enrollment retries, plan selection routing, and benefits audit trails.**

Each stage of the enrollment process is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of presenting options before selection, validating before enrollment, enrolling with carriers only after validation passes, confirming only after all carriers acknowledge, retrying if a carrier API is temporarily unavailable during peak enrollment periods, and maintaining a complete audit trail for ERISA compliance. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Eligibility check, plan comparison, enrollment processing, and confirmation workers guide employees through benefits selection as independent steps.

| Worker | Task | What It Does |
|---|---|---|
| **PresentWorker** | `ben_present` | Retrieves the employee's eligible benefit options. medical plans (PPO, HMO, HDHP), dental tiers, and vision coverage,  based on their employment class and location |
| **SelectWorker** | `ben_select` | Captures the employee's plan selections for medical, dental, and vision, including dependent coverage elections |
| **ValidateWorker** | `ben_validate` | Validates selections against plan rules. HDHP/HSA pairing, dependent age limits, regional availability, and ACA affordability requirements |
| **EnrollWorker** | `ben_enroll` | Submits validated selections to insurance carriers, calculates the total monthly premium, and sets the coverage effective date |
| **ConfirmWorker** | `ben_confirm` | Sends the employee a benefits confirmation with plan summaries, ID card details, payroll deduction amounts, and carrier contact information |

Workers implement HR operations. onboarding tasks, approvals, provisioning,  with realistic outputs. Replace with real HRIS and identity provider integrations and the workflow stays the same.

### The Workflow

```
ben_present
    │
    ▼
ben_select
    │
    ▼
ben_validate
    │
    ▼
ben_enroll
    │
    ▼
ben_confirm

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
java -jar target/benefits-enrollment-1.0.0.jar

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
java -jar target/benefits-enrollment-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow ben_benefits_enrollment \
  --version 1 \
  --input '{"employeeId": "TEST-001", "enrollmentPeriod": "sample-enrollmentPeriod"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w ben_benefits_enrollment -s COMPLETED -c 5

```

## How to Extend

Connect each worker to your real benefits systems. your benefits admin platform for plan options, your carrier APIs for enrollment, your HRIS for confirmation and premium deductions, and the workflow runs identically in production.

- **PresentWorker** → query your benefits administration platform (Benefitfocus, bswift) for real plan catalogs filtered by the employee's employment class, location, and life event eligibility
- **SelectWorker** → integrate with your enrollment portal to capture real-time selections with dependent information and beneficiary designations
- **ValidateWorker** → enforce carrier-specific enrollment rules, ACA affordability safe harbor calculations, and Section 125 cafeteria plan compliance
- **EnrollWorker** → transmit 834 EDI enrollment files to each carrier and process acknowledgments, calculating actual premiums with employer contribution splits
- **ConfirmWorker** → generate benefits confirmation statements with digital ID cards, FSA/HSA account setup links, and payroll deduction schedules
- Add a **LifeEventWorker** to handle qualifying life events (marriage, birth, job loss) that trigger special enrollment periods outside open enrollment
- Add a **COBRAWorker** after termination to calculate COBRA continuation premiums and send election notices within the 14-day requirement

Update plan options or eligibility rules and the enrollment pipeline handles them without modification.

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
benefits-enrollment-benefits-enrollment/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/benefitsenrollment/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── BenefitsEnrollmentExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ConfirmWorker.java
│       ├── EnrollWorker.java
│       ├── PresentWorker.java
│       ├── SelectWorker.java
│       └── ValidateWorker.java
└── src/test/java/benefitsenrollment/workers/
    ├── ConfirmWorkerTest.java        # 2 tests
    ├── EnrollWorkerTest.java        # 2 tests
    ├── PresentWorkerTest.java        # 2 tests
    ├── SelectWorkerTest.java        # 2 tests
    └── ValidateWorkerTest.java        # 2 tests

```
