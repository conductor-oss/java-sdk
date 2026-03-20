# Project Kickoff Automation in Java with Conductor :  Scope Definition, Team Assignment, Plan Creation, and Kickoff

A Java Conductor workflow example that automates project kickoff .  defining project scope with objectives, deliverables, and timeline, assigning a team with a lead and members based on the scope requirements, creating a phased project plan with milestones, and formally kicking off the project with sponsor notification and status activation. Uses [Conductor](https://github.## Why Project Kickoff Needs Orchestration

Starting a project requires a sequence of decisions where each step depends on the one before it. You define the scope .  listing objectives ("Deliver MVP", "User testing"), counting deliverables, and setting a timeline (12 weeks). You assign a team based on that scope ,  selecting a project lead, adding team members with the right skills, and confirming headcount fits the budget. You create a project plan that maps the scope to the team ,  breaking work into phases (Discovery, Design, Build, Launch) with milestones gating each transition. Finally, you formally kick off the project ,  setting the status to ACTIVE, recording the kickoff date, and notifying the sponsor.

Each step gates the next .  you cannot assign a team without knowing the scope's skill requirements, you cannot create a plan without knowing both the scope and the team's capacity, and you cannot kick off without a finalized plan. If team assignment fails because a key role is unavailable, the plan should not be created with an incomplete team. Without orchestration, you'd build a monolithic kickoff script that mixes scope documents, HR lookups, project planning, and status updates ,  making it impossible to retry a failed team assignment without re-defining the scope, audit which step delayed the kickoff, or reuse the scope definition for a different team configuration.

## How This Workflow Solves It

**You just write the scope definition, team assignment, plan creation, and kickoff notification logic. Conductor handles team assignment retries, plan creation sequencing, and kickoff audit trails.**

Each kickoff stage is an independent worker .  define scope, assign team, create plan, kick off. Conductor sequences them, passes the scope into team assignment, feeds both scope and team into plan creation, hands the finalized plan to kickoff, retries if an HR system is temporarily unavailable during team assignment, and records every decision from scope definition through project activation.

### What You Write: Workers

Scope definition, team assignment, plan creation, and kickoff workers each automate one preparatory phase of launching a new project.

| Worker | Task | What It Does |
|---|---|---|
| **AssignTeamWorker** | `pkf_assign_team` | Assigns the team |
| **CreatePlanWorker** | `pkf_create_plan` | Creating project plan |
| **DefineScopeWorker** | `pkf_define_scope` | Defines the scope |
| **KickOffWorker** | `pkf_kick_off` | Kick Off. Computes and returns project |

Workers simulate project management operations .  task creation, status updates, notifications ,  with realistic outputs. Replace with real Jira/Asana/Linear integrations and the workflow stays the same.

### The Workflow

```
pkf_define_scope
    │
    ▼
pkf_assign_team
    │
    ▼
pkf_create_plan
    │
    ▼
pkf_kick_off
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
java -jar target/project-kickoff-1.0.0.jar
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
java -jar target/project-kickoff-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow project_kickoff_project-kickoff \
  --version 1 \
  --input '{"projectName": "sample-name", "Project Alpha": "sample-Project Alpha", "sponsor": "sample-sponsor", "VP Engineering": "sample-VP Engineering", "budget": "sample-budget"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w project_kickoff_project-kickoff -s COMPLETED -c 5
```

## How to Extend

Connect each worker to your real PM tools. Jira for scope tracking, your HR system for team assignment, Confluence for plan documentation, and the workflow runs identically in production.

- **DefineScopeWorker** (`pkf_define_scope`): pull scope from your project intake system (Jira, Asana, or a custom intake form), extract objectives, count deliverables, and compute timeline estimates based on historical project data
- **AssignTeamWorker** (`pkf_assign_team`): query your HR or resource management system to find available team members with the required skills, check capacity against existing commitments, and assign a project lead
- **CreatePlanWorker** (`pkf_create_plan`): generate a project plan in your PM tool (Jira, Monday.com, MS Project) with phases, milestone dates computed from the scope timeline, and task assignments mapped to team members
- **KickOffWorker** (`pkf_kick_off`): set the project status to ACTIVE in your PM system, send kickoff notifications to the sponsor and team via Slack or email, and create the initial project status page in Confluence or Notion

Integrate your actual PM tools and the kickoff pipeline runs without structural modifications.

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
project-kickoff-project-kickoff/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/projectkickoff/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ProjectKickoffExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AssignTeamWorker.java
│       ├── CreatePlanWorker.java
│       ├── DefineScopeWorker.java
│       └── KickOffWorker.java
└── src/test/java/projectkickoff/workers/
    ├── AssignTeamWorkerTest.java        # 2 tests
    ├── CreatePlanWorkerTest.java        # 2 tests
    ├── DefineScopeWorkerTest.java        # 2 tests
    └── KickOffWorkerTest.java        # 2 tests
```
