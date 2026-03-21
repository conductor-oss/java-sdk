# Sprint Retrospective in Java with Conductor :  Feedback Collection, Categorization, Prioritization, and Action Items

A Java Conductor workflow example for automating sprint retrospectives. collecting team feedback (what went well, what didn't, what to improve), categorizing it into themes, prioritizing by impact, and generating actionable improvement items for the next sprint. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.
## The Problem

You need to run retrospectives consistently across sprints and teams. After each sprint, the team provides feedback. blockers they hit, processes that worked, tools that slowed them down. That feedback needs to be collected from multiple sources (survey forms, Slack threads, meeting notes), categorized into themes (process, tooling, communication, technical debt), prioritized by how many people raised the issue and its impact on velocity, and turned into concrete action items assigned to owners with deadlines.

Without orchestration, retrospectives become ad-hoc meetings where feedback is captured in a Google Doc, never categorized, and the same issues surface sprint after sprint. Building this as a script means a failure pulling feedback from Slack silently skips categorization, and you lose the ability to track which action items were actually generated and whether they carried over from previous sprints.

## The Solution

**You just write the feedback collection, theme categorization, impact prioritization, and action item generation logic. Conductor handles feedback collection retries, theme analysis, and action item tracking.**

Each retrospective step is a simple, independent worker. one collects raw feedback, one categorizes it into themes, one prioritizes by frequency and impact, one generates action items with owners and deadlines. Conductor takes care of executing them in sequence, retrying if a data source is temporarily unavailable, and maintaining a historical record of every retrospective so you can track improvement trends across sprints. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Feedback collection, theme identification, action item creation, and follow-up tracking workers each own one step of the team retrospective process.

| Worker | Task | What It Does |
|---|---|---|
| **CollectFeedbackWorker** | `rsp_collect_feedback` | Gathers team feedback from surveys, Slack channels, and meeting notes for the given sprint |
| **CategorizeWorker** | `rsp_categorize` | Groups raw feedback into themes. process, tooling, communication, technical debt, team dynamics |
| **PrioritizeWorker** | `rsp_prioritize` | Ranks categorized items by frequency, impact on velocity, and team sentiment scores |
| **ActionItemsWorker** | `rsp_action_items` | Converts top priorities into concrete action items with owners, deadlines, and success criteria |

Workers implement project management operations. task creation, status updates, notifications,  with realistic outputs. Replace with real Jira/Asana/Linear integrations and the workflow stays the same.

### The Workflow

```
rsp_collect_feedback
    │
    ▼
rsp_categorize
    │
    ▼
rsp_prioritize
    │
    ▼
rsp_action_items

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
java -jar target/retrospective-1.0.0.jar

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
java -jar target/retrospective-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow retrospective_retrospective \
  --version 1 \
  --input '{"sprintId": "SPR-42", "SPR-42": "teamName", "teamName": "Platform Engineering", "Platform Engineering": "facilitator", "facilitator": "Scrum Master", "Scrum Master": "sample-Scrum Master"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w retrospective_retrospective -s COMPLETED -c 5

```

## How to Extend

Connect each worker to your real retrospective tools. Slack and survey forms for feedback collection, your analytics platform for categorization, Jira for action item creation, and the workflow runs identically in production.

- **CollectFeedbackWorker** (`rsp_collect_feedback`): pull responses from Google Forms/Typeform surveys, scrape tagged Slack messages, or query a retro tool like Retrium/EasyRetro
- **CategorizeWorker** (`rsp_categorize`): use NLP or an LLM to auto-categorize free-text feedback, or map against a predefined taxonomy stored in your knowledge base
- **PrioritizeWorker** (`rsp_prioritize`): implement dot-voting tallies, weighted scoring based on team size impact, or cross-reference with previous sprint action items to flag recurring issues
- **ActionItemsWorker** (`rsp_action_items`): create Jira tickets for each action item, assign owners based on team roster, and set due dates aligned with the next sprint boundary

Swap feedback tools or action tracking systems and the retrospective pipeline keeps working.

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
retrospective-retrospective/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/retrospective/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── RetrospectiveExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ActionItemsWorker.java
│       ├── CategorizeWorker.java
│       ├── CollectFeedbackWorker.java
│       └── PrioritizeWorker.java
└── src/test/java/retrospective/workers/
    ├── ActionItemsWorkerTest.java        # 2 tests
    ├── CategorizeWorkerTest.java        # 2 tests
    ├── CollectFeedbackWorkerTest.java        # 2 tests
    └── PrioritizeWorkerTest.java        # 2 tests

```
