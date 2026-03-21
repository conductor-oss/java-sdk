# Project Closure in Java with Conductor :  Deliverable Review, Sign-Off, Archival, and Lessons Learned

A Java Conductor workflow example for closing out a project. reviewing all deliverables against acceptance criteria, obtaining formal stakeholder sign-off, archiving project artifacts, and capturing lessons learned for future projects. Uses [Conductor](https://github.

## The Problem

You need to formally close a project. Every deliverable must be reviewed against its acceptance criteria before anyone signs off. Sign-off must happen before archival. you can't archive incomplete work. After archival, lessons learned need to be captured while the project is still fresh. Skip any step and you end up with unsigned deliverables sitting in limbo, project artifacts scattered across personal drives, and the same mistakes repeated on the next project.

Without orchestration, closure becomes a checklist someone tracks in a spreadsheet. Deliverables get signed off before review is complete, archives are missing key documents because the archival script ran before sign-off, and lessons learned never happen because the process fell apart. Building this as a monolithic script means a failure in the archival step silently skips lessons learned, and nobody knows the project was never properly closed.

## The Solution

**You just write the deliverable review, sign-off collection, artifact archival, and lessons learned capture logic. Conductor handles deliverable retries, archival sequencing, and closure audit trails.**

Each closure step is a simple, independent worker. one reviews deliverables against acceptance criteria, one processes formal sign-off, one archives all project artifacts, one captures lessons learned. Conductor takes care of executing them in strict sequence so nothing gets skipped, retrying if the document management system is temporarily unavailable, and maintaining a permanent record of exactly when each closure step completed. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Deliverable verification, documentation archival, lessons-learned capture, and stakeholder sign-off workers each handle one closure activity.

| Worker | Task | What It Does |
|---|---|---|
| **ReviewDeliverablesWorker** | `pcl_review_deliverables` | Checks each deliverable against its acceptance criteria and flags incomplete items |
| **SignOffWorker** | `pcl_sign_off` | Records formal stakeholder approval of all deliverables, capturing approver and timestamp |
| **ArchiveWorker** | `pcl_archive` | Moves all project artifacts (documents, code, reports) to long-term storage and returns an archive ID |
| **LessonsLearnedWorker** | `pcl_lessons_learned` | Generates a lessons-learned report from project data (what worked, what didn't, recommendations) |

Workers implement project management operations. task creation, status updates, notifications,  with realistic outputs. Replace with real Jira/Asana/Linear integrations and the workflow stays the same.

### The Workflow

```
pcl_review_deliverables
    │
    ▼
pcl_sign_off
    │
    ▼
pcl_archive
    │
    ▼
pcl_lessons_learned

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
java -jar target/project-closure-1.0.0.jar

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
java -jar target/project-closure-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow project_closure_project-closure \
  --version 1 \
  --input '{"projectId": "PRJ-909", "PRJ-909": "projectName", "projectName": "Cloud Migration", "Cloud Migration": "manager", "manager": "Sarah Chen", "Sarah Chen": "sample-Sarah Chen"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w project_closure_project-closure -s COMPLETED -c 5

```

## How to Extend

Swap each worker for your real closure tools. your PM platform for deliverable review, DocuSign for stakeholder sign-off, Confluence or SharePoint for artifact archival, and the workflow runs identically in production.

- **ReviewDeliverablesWorker** (`pcl_review_deliverables`): query Jira for all deliverable tickets, check completion status, and validate against acceptance criteria stored in Confluence
- **SignOffWorker** (`pcl_sign_off`): integrate with DocuSign or a HUMAN task for real e-signatures from project sponsors and stakeholders
- **ArchiveWorker** (`pcl_archive`): upload artifacts to S3/Google Cloud Storage with retention policies, or push to SharePoint/Confluence archive spaces
- **LessonsLearnedWorker** (`pcl_lessons_learned`): pull sprint retrospective data from your PM tool, analyze velocity trends, and generate a structured report in Confluence

Change archival systems or sign-off workflows and the closure pipeline handles them with no restructuring.

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
project-closure-project-closure/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/projectclosure/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ProjectClosureExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ArchiveWorker.java
│       ├── LessonsLearnedWorker.java
│       ├── ReviewDeliverablesWorker.java
│       └── SignOffWorker.java
└── src/test/java/projectclosure/workers/
    ├── ArchiveWorkerTest.java        # 2 tests
    ├── LessonsLearnedWorkerTest.java        # 2 tests
    ├── ReviewDeliverablesWorkerTest.java        # 2 tests
    └── SignOffWorkerTest.java        # 2 tests

```
