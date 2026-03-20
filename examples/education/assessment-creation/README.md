# Assessment Creation in Java with Conductor :  Criteria Definition, Question Generation, Review, and Publishing

A Java Conductor workflow example for creating educational assessments .  defining grading criteria from course topics, generating questions that align with those criteria, reviewing questions for quality and accuracy, and publishing the finalized assessment to the course. Uses [Conductor](https://github.## The Problem

You need to create exams, quizzes, or assignments for a course. This means defining the assessment criteria based on the course's topics and desired difficulty level, generating questions that cover those criteria (multiple choice, short answer, essay), having an instructor or peer review the question bank for accuracy and fairness, and publishing the finalized assessment so students can access it. Each step depends on the previous .  you cannot generate questions without criteria, and you cannot publish without review.

Without orchestration, you'd build a monolithic assessment-builder service that mixes criteria logic, question generation, review tracking, and LMS publishing into a single class .  manually ensuring questions align with criteria, handling review rejection loops with ad-hoc state flags, and logging everything to figure out why a published exam contained unreviewed questions.

## The Solution

**You just write the criteria definition, question generation, review, and publishing logic. Conductor handles generation retries, review routing, and assessment version tracking.**

Each assessment concern is a simple, independent worker .  a plain Java class that does one thing. Conductor takes care of executing them in order (define criteria, create questions, review, publish), retrying if the LMS publishing API times out, tracking the full lifecycle of every assessment from criteria to published exam, and resuming from the last successful step if the process crashes. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Workers for criteria definition, question generation, review, and publishing each own one stage of assessment authoring.

| Worker | Task | What It Does |
|---|---|---|
| **DefineCriteriaWorker** | `asc_define_criteria` | Establishes grading criteria based on the course ID, assessment type, and selected topics |
| **CreateQuestionsWorker** | `asc_create_questions` | Generates a question bank that aligns with the defined criteria |
| **ReviewWorker** | `asc_review` | Reviews the generated questions for accuracy, difficulty balance, and coverage |
| **PublishWorker** | `asc_publish` | Publishes the reviewed assessment to the course in the learning management system |

Workers simulate educational operations .  enrollment, grading, notifications ,  with realistic outputs. Replace with real LMS and SIS integrations and the workflow stays the same.

### The Workflow

```
asc_define_criteria
    │
    ▼
asc_create_questions
    │
    ▼
asc_review
    │
    ▼
asc_publish
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
java -jar target/assessment-creation-1.0.0.jar
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
java -jar target/assessment-creation-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow asc_assessment_creation \
  --version 1 \
  --input '{"courseId": "TEST-001", "assessmentType": "test-value", "topics": "test-value"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w asc_assessment_creation -s COMPLETED -c 5
```

## How to Extend

Connect each worker to your real assessment tools .  your LMS (Canvas, Blackboard) for criteria and publishing, an AI question generator for content creation, a peer review system for quality checks, and the workflow runs identically in production.

- **DefineCriteriaWorker** (`asc_define_criteria`): pull learning objectives and Bloom's taxonomy levels from your curriculum database or LMS API (Canvas, Blackboard, Moodle)
- **CreateQuestionsWorker** (`asc_create_questions`): generate questions using an AI service (OpenAI, Claude) or draw from a curated question bank database; tag each question with difficulty and topic
- **ReviewWorker** (`asc_review`): integrate with a human-in-the-loop review system or use a WAIT task to pause for instructor approval before publishing
- **PublishWorker** (`asc_publish`): publish the assessment to your LMS via its API (Canvas REST API, Blackboard Learn API, Moodle Web Services)

Swap your question bank or review criteria and the assessment pipeline adjusts without structural changes.

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
assessment-creation/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/assessmentcreation/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── AssessmentCreationExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CreateQuestionsWorker.java
│       ├── DefineCriteriaWorker.java
│       ├── PublishWorker.java
│       └── ReviewWorker.java
└── src/test/java/assessmentcreation/workers/
    ├── CreateQuestionsWorkerTest.java        # 2 tests
    ├── DefineCriteriaWorkerTest.java        # 2 tests
    ├── PublishWorkerTest.java        # 2 tests
    └── ReviewWorkerTest.java        # 2 tests
```
