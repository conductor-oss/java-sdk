# Lesson Planning in Java with Conductor :  Learning Objectives, Content Creation, Review, and Publishing

A Java Conductor workflow example for building lesson plans. defining learning objectives for a course topic, creating instructional content aligned to those objectives, reviewing the lesson plan for quality and curriculum alignment, and publishing it to the course schedule for a given week. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.
## The Problem

You need to prepare lesson plans for each week of a course. This means defining measurable learning objectives for the lesson topic, creating instructional content (lecture materials, activities, readings) that aligns with those objectives, having the plan reviewed for pedagogical quality and curriculum fit, and publishing it to the course so students and co-instructors can see the upcoming schedule. Creating content without clear objectives leads to unfocused lessons; publishing without review risks distributing incomplete or misaligned materials.

Without orchestration, you'd build a single lesson-builder tool that mixes objective definition, content authoring, review workflows, and LMS publishing in one class. manually tracking which lessons have been reviewed, retrying when the content management system is down, and logging every step to figure out why a lesson appeared in the course calendar without review approval.

## The Solution

**You just write the learning objectives definition, content creation, pedagogical review, and course publishing logic. Conductor handles content selection retries, plan assembly sequencing, and lesson version tracking.**

Each lesson planning concern is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of executing them in order (define objectives, create content, review, publish), retrying if the LMS publishing API times out, tracking every lesson plan from initial objectives to published materials, and resuming from the last successful step if the process crashes. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Objective setting, content selection, activity design, and plan assembly workers let educators build lesson plans through composable steps.

| Worker | Task | What It Does |
|---|---|---|
| **DefineObjectivesWorker** | `lpl_define_objectives` | Establishes measurable learning objectives based on the course and lesson topic |
| **CreateContentWorker** | `lpl_create_content` | Builds instructional content (slides, activities, readings) aligned to the defined objectives |
| **ReviewWorker** | `lpl_review` | Reviews the lesson plan for pedagogical quality, accuracy, and curriculum alignment |
| **PublishWorker** | `lpl_publish` | Publishes the reviewed lesson plan to the course schedule for the specified week |

Workers implement educational operations. enrollment, grading, notifications,  with realistic outputs. Replace with real LMS and SIS integrations and the workflow stays the same.

### The Workflow

```
lpl_define_objectives
    │
    ▼
lpl_create_content
    │
    ▼
lpl_review
    │
    ▼
lpl_publish

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
java -jar target/lesson-planning-1.0.0.jar

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
java -jar target/lesson-planning-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow lpl_lesson_planning \
  --version 1 \
  --input '{"courseId": "TEST-001", "lessonTitle": "sample-lessonTitle", "week": "sample-week"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w lpl_lesson_planning -s COMPLETED -c 5

```

## How to Extend

Point each worker at your real curriculum tools. your curriculum database for learning objectives, Google Slides or your content authoring platform for materials creation, your LMS (Canvas, Moodle) for publishing, and the workflow runs identically in production.

- **DefineObjectivesWorker** (`lpl_define_objectives`): pull course learning outcomes from your curriculum database and generate Bloom's taxonomy-aligned objectives using an AI service
- **CreateContentWorker** (`lpl_create_content`): generate slide decks (Google Slides API), compile reading lists from your digital library, and create interactive activities using your content authoring tool
- **ReviewWorker** (`lpl_review`): route the lesson plan to a department reviewer via a WAIT task or integrate with a peer review system for instructor feedback
- **PublishWorker** (`lpl_publish`): push the finalized lesson plan to your LMS (Canvas Modules API, Blackboard Content API, Moodle Web Services) for the specified course week

Swap content libraries or activity templates and the planning pipeline structure persists.

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
lesson-planning/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/lessonplanning/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── LessonPlanningExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CreateContentWorker.java
│       ├── DefineObjectivesWorker.java
│       ├── PublishWorker.java
│       └── ReviewWorker.java
└── src/test/java/lessonplanning/workers/
    ├── CreateContentWorkerTest.java        # 2 tests
    ├── DefineObjectivesWorkerTest.java        # 2 tests
    ├── PublishWorkerTest.java        # 2 tests
    └── ReviewWorkerTest.java        # 2 tests

```
