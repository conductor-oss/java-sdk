# Leave Management in Java with Conductor :  Request, Balance Check, Manager Approval, Accrual Update, and Notification

A Java Conductor workflow example for leave management .  submitting a leave request with type and dates, checking the employee's available PTO balance, routing for manager approval, updating accrual balances upon approval, and notifying the employee and team. Uses [Conductor](https://github.## The Problem

You need to manage employee leave requests from submission through approval. An employee submits a leave request specifying the type (vacation, sick, personal, FMLA), start date, and number of days. The system must check the employee's available balance for that leave type to ensure they have sufficient hours. The request is then routed to the manager for approval, taking team coverage into account. Once approved, the leave balance must be debited and the payroll calendar updated. Finally, the employee receives confirmation, the team calendar is updated, and the manager is notified. If the balance check or approval is skipped, employees may overdraw their PTO or take leave without coverage arranged.

Without orchestration, you'd build a monolithic leave system that queries the accrual database, sends the manager an approval email, waits for a response, updates the balance, and notifies everyone. If the payroll system is temporarily unavailable for the balance update, you'd need retry logic. If the system crashes after approval but before updating the balance, the employee has approved leave but incorrect accruals. HR needs a complete audit trail of every leave request for labor law compliance.

## The Solution

**You just write the leave request, balance checking, manager approval, accrual update, and notification logic. Conductor handles approval routing, balance updates, and leave request audit trails.**

Each stage of the leave request is a simple, independent worker .  a plain Java class that does one thing. Conductor takes care of checking balances before approval, updating accruals only after approval, sending notifications as the final step, retrying if the payroll or HRIS system is temporarily unavailable, and maintaining a complete audit trail for labor law compliance. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Request intake, balance checking, approval routing, and calendar update workers handle leave requests through independent validation steps.

| Worker | Task | What It Does |
|---|---|---|
| **RequestWorker** | `lvm_request` | Registers the leave request with employee ID, leave type, start date, and duration |
| **CheckBalanceWorker** | `lvm_check_balance` | Queries the employee's available leave balance for the requested type and verifies sufficient hours |
| **ApproveWorker** | `lvm_approve` | Routes the request to the manager for approval, considering team coverage and blackout dates |
| **UpdateWorker** | `lvm_update` | Debits the leave balance, updates the payroll calendar, and records the approved absence |
| **NotifyWorker** | `lvm_notify` | Sends confirmation to the employee and updates the team calendar with the approved absence |

Workers simulate HR operations .  onboarding tasks, approvals, provisioning ,  with realistic outputs. Replace with real HRIS and identity provider integrations and the workflow stays the same.

### The Workflow

```
lvm_request
    │
    ▼
lvm_check_balance
    │
    ▼
lvm_approve
    │
    ▼
lvm_update
    │
    ▼
lvm_notify
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
java -jar target/leave-management-1.0.0.jar
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
java -jar target/leave-management-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow lvm_leave_management \
  --version 1 \
  --input '{"employeeId": "TEST-001", "leaveType": "test-value", "startDate": "2026-01-01T00:00:00Z", "days": "test-value"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w lvm_leave_management -s COMPLETED -c 5
```

## How to Extend

Wire each worker to your real leave systems .  your HRIS for balance lookups, your approval platform for manager routing, your payroll system for accrual updates, and the workflow runs identically in production.

- **RequestWorker** → create leave requests in your HRIS (Workday, BambooHR, ADP) with attachment support for medical documentation
- **CheckBalanceWorker** → query real-time accrual balances from your payroll system, including carryover and FMLA entitlements
- **ApproveWorker** → integrate with Slack or Teams for one-click manager approval, with automatic escalation if approval is delayed
- **UpdateWorker** → debit balances in your payroll system and update scheduling/coverage calendars in real time
- **NotifyWorker** → send confirmation via email/Slack and block the dates on shared team calendars (Google Calendar, Outlook)
- Add a **FMLACheckWorker** with a SWITCH on leave type to verify FMLA eligibility and track intermittent leave hours against the 12-week entitlement

Swap your HRIS or calendar system and the leave pipeline processes requests identically.

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
leave-management-leave-management/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/leavemanagement/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── LeaveManagementExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ApproveWorker.java
│       ├── CheckBalanceWorker.java
│       ├── NotifyWorker.java
│       ├── RequestWorker.java
│       └── UpdateWorker.java
└── src/test/java/leavemanagement/workers/
    ├── ApproveWorkerTest.java        # 2 tests
    ├── CheckBalanceWorkerTest.java        # 2 tests
    ├── NotifyWorkerTest.java        # 2 tests
    ├── RequestWorkerTest.java        # 2 tests
    └── UpdateWorkerTest.java        # 2 tests
```
