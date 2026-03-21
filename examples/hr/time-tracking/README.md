# Time Tracking in Java with Conductor :  Timesheet Submission, Validation, Manager Approval, and Payroll Processing

A Java Conductor workflow example for employee time tracking. submitting a weekly timesheet with hours by project code, validating entries against overtime rules and project allocations, routing for manager approval, and processing approved hours into payroll. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.
## The Problem

You need to process employee timesheets from submission through payroll every week. An employee submits their timesheet for the week ending date, logging hours against project codes and cost centers. The entries must be validated. checking that total hours are within policy limits, overtime is flagged for FLSA compliance, and hours are charged to active project codes. The validated timesheet routes to the employee's manager for approval, where the manager reviews hours against expected allocations and overtime justifications. Once approved, the timesheet feeds into payroll processing,  converting hours into gross pay calculations, applying overtime rates (1.5x for hours over 40), and posting to the general ledger by cost center. If validation is skipped, employees can charge hours to closed projects or exceed overtime limits without authorization.

Without orchestration, you'd build a monolithic timesheet system that collects entries, runs validation rules, sends the manager an approval email, polls for a response, and pushes approved hours to payroll. If the payroll system is down on Friday night when timesheets are due, you'd need retry logic. If the system crashes after approval but before payroll processing, the employee has an approved timesheet but no paycheck. Finance and labor law auditors need a complete trail showing every timesheet's path from submission through payment.

## The Solution

**You just write the timesheet submission, overtime validation, manager approval, and payroll processing logic. Conductor handles clock-in retries, overtime calculations, and timesheet audit trails.**

Each stage of timesheet processing is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of validating entries before they reach the manager, processing payroll only after manager approval, retrying if the payroll or ERP system is temporarily unavailable, and maintaining a complete audit trail from submission through payment for FLSA and labor law compliance. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Clock-in recording, break management, overtime calculation, and payroll export workers each handle one aspect of workforce time management.

| Worker | Task | What It Does |
|---|---|---|
| **SubmitWorker** | `ttk_submit` | Records the employee's timesheet submission for the week ending date, assigns a timesheet ID, and locks the entries for validation |
| **ValidateWorker** | `ttk_validate` | Checks entries against project codes, calculates total hours and overtime, and flags policy violations (over 40 hours, inactive projects) |
| **ApproveWorker** | `ttk_approve` | Routes the validated timesheet to the employee's manager for approval with overtime justification if applicable |
| **ProcessWorker** | `ttk_process` | Converts approved hours into payroll. applies regular and overtime rates, posts labor costs to cost centers in the general ledger |

Workers implement HR operations. onboarding tasks, approvals, provisioning,  with realistic outputs. Replace with real HRIS and identity provider integrations and the workflow stays the same.

### The Workflow

```
ttk_submit
    │
    ▼
ttk_validate
    │
    ▼
ttk_approve
    │
    ▼
ttk_process

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
java -jar target/time-tracking-1.0.0.jar

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
java -jar target/time-tracking-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow ttk_time_tracking \
  --version 1 \
  --input '{"employeeId": "TEST-001", "weekEnding": "sample-weekEnding", "entries": "sample-entries"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w ttk_time_tracking -s COMPLETED -c 5

```

## How to Extend

Swap each worker for your real time systems. your timesheet platform for submission, your labor rules engine for validation, your payroll system for processing, and the workflow runs identically in production.

- **SubmitWorker** → create timesheet records in your time tracking system (Kronos, ADP Time, Replicon) with project code breakdowns and GPS-verified clock-in/out for field workers
- **ValidateWorker** → validate hours against your ERP's active project list, check FLSA overtime thresholds, and enforce union contract rules (guaranteed minimums, shift differentials, double-time triggers)
- **ApproveWorker** → route to the manager via Slack or Teams with an hours-by-project summary, auto-approve timesheets under 40 hours, and escalate overtime requests to the department head
- **ProcessWorker** → post approved hours to your payroll system (ADP, Paychex, Ceridian) with regular/overtime rate splits and GL cost center allocations
- Add a **ProjectAllocationWorker** before validation to verify the employee is authorized to charge time to each project code and check remaining budget
- Add a **ComplianceWorker** after processing to generate FLSA overtime reports and flag employees approaching weekly hour limits for DOL compliance

Replace your time clock or payroll integration and the tracking pipeline continues unchanged.

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
time-tracking-time-tracking/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/timetracking/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── TimeTrackingExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ApproveWorker.java
│       ├── ProcessWorker.java
│       ├── SubmitWorker.java
│       └── ValidateWorker.java
└── src/test/java/timetracking/workers/
    ├── ApproveWorkerTest.java        # 2 tests
    ├── ProcessWorkerTest.java        # 2 tests
    ├── SubmitWorkerTest.java        # 2 tests
    └── ValidateWorkerTest.java        # 2 tests

```
