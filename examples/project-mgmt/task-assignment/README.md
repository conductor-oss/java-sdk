# Task Assignment Automation in Java with Conductor :  Task Analysis, Skill Matching, Assignment, Notification, and Tracking

A Java Conductor workflow example that automates task assignment. analyzing the task to extract required skills and complexity, matching those skills against available team members with compatibility scoring, assigning the task to the best candidate, notifying the assignee, and setting up tracking with status and due date. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.
## Why Task Assignment Needs Orchestration

Assigning tasks to the right person requires a pipeline where each step narrows the decision. You analyze the task. parsing the title and description to extract required skills (e.g., JavaScript, React) and assessing complexity (low, medium, high). You match those skills against your team,  scoring each team member on skill overlap and checking availability, producing a best match with a compatibility score (e.g., Alice at 95% match, availability "open"). You formally assign the task to the selected candidate. You notify the assignee so they know work is waiting. You set up tracking,  recording the assignee, setting the status to IN_PROGRESS, and computing a due date based on complexity.

Each step depends on the previous one. skill matching needs the analyzed skill list, assignment needs the best match candidate, notification needs the confirmed assignee, and tracking needs the assignment confirmation. If the best-match team member becomes unavailable between matching and assignment (they got pulled into an incident), the assignment step should fail and retry after the skill matcher finds the next-best candidate,  not silently assign to an unavailable person. Without orchestration, you'd build a monolithic assignment function that mixes NLP task analysis, team directory lookups, PM tool API calls, Slack notifications, and status updates,  making it impossible to swap your skill matching algorithm, add a new notification channel, or audit why a specific task was assigned to a specific person.

## How This Workflow Solves It

**You just write the task analysis, skill matching, assignment, notification, and tracking logic. Conductor handles skill matching retries, notification delivery, and assignment audit trails.**

Each assignment stage is an independent worker. analyze, match skills, assign, notify, track. Conductor sequences them, passes the extracted skills into matching, feeds the best match candidate into assignment, hands the confirmed assignee to notification and tracking, retries if your team directory API is temporarily unavailable during skill matching, and records every decision from task analysis through tracking setup.

### What You Write: Workers

Workload analysis, skill matching, assignment, and notification workers each own one step of distributing work to team members.

| Worker | Task | What It Does |
|---|---|---|
| **AnalyzeWorker** | `tas_analyze` | Analyzes and returns skills, complexity |
| **AssignWorker** | `tas_assign` | Assigning task to candidate |
| **MatchSkillsWorker** | `tas_match_skills` | Finding best match for skills |
| **NotifyWorker** | `tas_notify` | Notify. Computes and returns notified, channel |
| **TrackWorker** | `tas_track` | Tracks task progress and updates completion status for the assignee |

Workers implement project management operations. task creation, status updates, notifications,  with realistic outputs. Replace with real Jira/Asana/Linear integrations and the workflow stays the same.

### The Workflow

```
tas_analyze
    │
    ▼
tas_match_skills
    │
    ▼
tas_assign
    │
    ▼
tas_notify
    │
    ▼
tas_track

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
java -jar target/task-assignment-1.0.0.jar

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
java -jar target/task-assignment-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow task_assignment_task-assignment \
  --version 1 \
  --input '{"taskTitle": "sample-taskTitle", "Build search feature": "sample-Build search feature", "requiredSkills": "sample-requiredSkills", "JavaScript": "sample-JavaScript", "priority": "sample-priority"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w task_assignment_task-assignment -s COMPLETED -c 5

```

## How to Extend

Point each worker at your real workforce tools. your skills database for matching, your PM tool for task assignment, Slack for notifications, and the workflow runs identically in production.

- **AnalyzeWorker** (`tas_analyze`): parse the task title and description using NLP or keyword extraction to identify required skills, estimate complexity from historical data on similar tasks, and tag with relevant project labels
- **MatchSkillsWorker** (`tas_match_skills`): query your team directory or HR system for members with matching skills, score candidates on skill overlap and current workload, and check calendar availability via Google Calendar or Outlook API
- **AssignWorker** (`tas_assign`): assign the task in your PM tool (Jira, Asana, Linear) via its API, update the assignee field, and set the task status to in-progress
- **NotifyWorker** (`tas_notify`): send assignment notifications via Slack, Microsoft Teams, or email with task details, priority, and a direct link to the task in your PM tool
- **TrackWorker** (`tas_track`): set up tracking by computing a due date from complexity and priority, creating calendar reminders for the assignee, and registering the task in your status dashboard for manager visibility

Change your skill matching algorithm or notification system and the assignment pipeline keeps its shape.

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
task-assignment-task-assignment/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/taskassignment/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── TaskAssignmentExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AnalyzeWorker.java
│       ├── AssignWorker.java
│       ├── MatchSkillsWorker.java
│       ├── NotifyWorker.java
│       └── TrackWorker.java
└── src/test/java/taskassignment/workers/
    ├── AnalyzeWorkerTest.java        # 2 tests
    ├── AssignWorkerTest.java        # 2 tests
    ├── MatchSkillsWorkerTest.java        # 2 tests
    ├── NotifyWorkerTest.java        # 2 tests
    └── TrackWorkerTest.java        # 2 tests

```
