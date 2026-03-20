# Employee Onboarding in Java with Conductor :  Profile Creation, System Provisioning, Mentor Assignment, and Training Plan

A Java Conductor workflow example for employee onboarding .  creating the new hire's profile in the HRIS, provisioning IT systems (laptop, email, Slack, Jira), assigning a department mentor, and generating a personalized training plan. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers ,  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## The Problem

You need to onboard a new employee across multiple departments and systems. When a hire is confirmed, the HR team must create the employee's profile with their name, department, and start date. IT must provision a laptop, create email and Slack accounts, and grant access to Jira and other department-specific tools. A mentor from the same department must be assigned to guide the new hire through their first weeks. Finally, a training plan must be generated with required compliance courses, department-specific skills training, and scheduled check-ins. Each step depends on the previous one. IT cannot provision accounts without the employee profile, and the training plan needs to know who the mentor is.

Without orchestration, you'd coordinate all of this through emails, tickets, and spreadsheets. HR creates the profile, emails IT for provisioning, messages a team lead to assign a mentor, and creates a training document. If IT provisioning is delayed, the new hire shows up on day one with no laptop or email. If a step is forgotten, the employee misses mandatory compliance training or never gets a mentor. HR has no single view of where each onboarding stands.

## The Solution

**You just write the profile creation, system provisioning, mentor assignment, and training plan generation logic. Conductor handles provisioning retries, onboarding step sequencing, and new-hire audit trails.**

Each stage of onboarding is a simple, independent worker .  a plain Java class that does one thing. Conductor takes care of creating the profile before provisioning, provisioning before mentor assignment, building the training plan with all prior context, retrying if any system (Active Directory, Slack API) is temporarily unavailable, and giving HR complete visibility into every onboarding's status. You get all of that for free, without writing a single line of orchestration code.

### What You Write: Workers

Profile creation, system provisioning, mentor assignment, and training plan workers each automate one phase of bringing a new employee into the organization.

| Worker | Task | What It Does |
|---|---|---|
| **CreateProfileWorker** | `hro_create_profile` | Creates the employee profile in the HRIS with name, department, role, and start date |
| **ProvisionWorker** | `hro_provision` | Provisions IT systems .  laptop order, email account, Slack workspace, Jira access, and VPN credentials |
| **AssignMentorWorker** | `hro_assign_mentor` | Selects and assigns a mentor from the same department based on availability and experience |
| **TrainingWorker** | `hro_training` | Generates a personalized training plan with required compliance courses and department-specific skills |

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
hro_create_profile
    │
    ▼
hro_provision
    │
    ▼
hro_assign_mentor
    │
    ▼
hro_training
```

## Example Output

```
=== Example 605: HR Onboarding ===

Step 1: Registering task definitions...
  Registered: hro_create_profile, hro_provision, hro_assign_mentor, hro_training

Step 2: Registering workflow 'hro_hr_onboarding'...
  Workflow registered.

Step 3: Starting workers...
  4 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [mentor] Assigned mentor from
  [profile] Created profile for
  [provision] Provisioned laptop, email, Slack, Jira for
  [training] Training plan created for

  Status: COMPLETED
  Output: {mentorId=..., mentorName=..., employeeId=..., email=...}

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
java -jar target/hr-onboarding-1.0.0.jar
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
java -jar target/hr-onboarding-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow hro_hr_onboarding \
  --version 1 \
  --input '{"employeeName": "sample-name", "John Doe": "sample-John Doe", "department": "sample-department", "Engineering": "sample-Engineering", "startDate": "2025-01-15T10:00:00Z"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w hro_hr_onboarding -s COMPLETED -c 5
```

## How to Extend

Point each worker at your real HR systems. BambooHR or Workday for profile creation, Okta for IT provisioning, your LMS for training plans, and the workflow runs identically in production.

- **CreateProfileWorker** → create real employee records in Workday, BambooHR, or ADP with payroll setup and tax form generation
- **ProvisionWorker** → call Okta for SSO provisioning, Google Workspace Admin for email, Slack API for workspace access, and your IT asset management system for laptop ordering
- **AssignMentorWorker** → query your org chart and mentor availability in your people platform, then send introduction emails to both parties
- **TrainingWorker** → enroll the new hire in required courses in your LMS (Cornerstone, Docebo) and schedule onboarding check-ins
- Add a **BackgroundCheckWorker** before profile creation to verify the hire's background check status
- Add a **WelcomePackageWorker** to ship company swag and welcome materials to the new hire's address

Integrate your actual HRIS, IT provisioning, and LMS systems and the onboarding pipeline runs without structural changes.

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
hr-onboarding-hr-onboarding/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/hronboarding/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── HrOnboardingExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AssignMentorWorker.java
│       ├── CreateProfileWorker.java
│       ├── ProvisionWorker.java
│       └── TrainingWorker.java
└── src/test/java/hronboarding/workers/
    ├── AssignMentorWorkerTest.java        # 2 tests
    ├── CreateProfileWorkerTest.java        # 3 tests
    ├── ProvisionWorkerTest.java        # 2 tests
    └── TrainingWorkerTest.java        # 2 tests
```
