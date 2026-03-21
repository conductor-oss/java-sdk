# User Survey in Java Using Conductor :  Creation, Distribution, Collection, Analysis, and Reporting

A Java Conductor workflow example for running user satisfaction surveys end-to-end .  creating a survey with custom questions, distributing it to a target audience, collecting responses, analyzing results (average satisfaction, top themes, sentiment breakdown), and generating a summary report. Uses [Conductor](https://github.

## The Problem

You need to gather structured feedback from users through surveys. That means creating a survey with a title and question set, sending it to a specific audience segment, collecting the responses, running analysis to extract satisfaction scores, recurring themes (ease of use, performance, pricing), and sentiment distribution (positive/neutral/negative), and finally producing a report that stakeholders can act on. Each step depends on the previous one .  you can't distribute a survey before it's created, you can't analyze responses before they're collected, and the report needs both the survey ID and the analysis results.

Without orchestration, you'd build a monolithic survey service that handles creation, email blasts, response aggregation, and analysis in a single codebase. If distribution fails for some recipients, the entire pipeline stalls. If the analysis step takes longer than expected, there's no timeout protection. When you need to rerun just the report generation with updated analysis, you have to replay the whole flow manually.

## The Solution

**You just write the survey-creation, distribution, collection, analysis, and reporting workers. Conductor handles the survey lifecycle and response data flow.**

Each survey lifecycle phase .  creation, distribution, collection, analysis, reporting ,  is a simple, independent worker. Conductor runs them in sequence, threads the generated survey ID from creation through every downstream step, feeds collected responses into the analyzer, and passes the analysis output into the report generator. If distribution times out or the analysis service fails, Conductor retries automatically and resumes from exactly where it left off. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

CreateSurveyWorker generates a survey ID, DistributeSurveyWorker sends it to the audience, CollectResponsesWorker gathers answers, AnalyzeSurveyWorker computes satisfaction and sentiment, and SurveyReportWorker produces the summary.

| Worker | Task | What It Does |
|---|---|---|
| **CreateSurveyWorker** | `usv_create` | Creates a survey with a unique ID (SRV-XXXXXXXX), registers the title and question list, returns the question count |
| **DistributeSurveyWorker** | `usv_distribute` | Sends the survey to the target audience segment via email, in-app notification, or SMS |
| **CollectResponsesWorker** | `usv_collect` | Gathers submitted responses for the survey and returns the response set with a count |
| **AnalyzeSurveyWorker** | `usv_analyze` | Computes average satisfaction score, extracts top themes, and breaks down sentiment (positive/neutral/negative percentages) |
| **SurveyReportWorker** | `usv_report` | Generates a summary report from the analysis results, tied to the survey ID |

Workers simulate user lifecycle operations .  account creation, verification, profile setup ,  with realistic outputs. Replace with real identity provider and database calls and the workflow stays the same.

### The Workflow

```
usv_create
    │
    ▼
usv_distribute
    │
    ▼
usv_collect
    │
    ▼
usv_analyze
    │
    ▼
usv_report

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
java -jar target/user-survey-1.0.0.jar

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
java -jar target/user-survey-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow usv_user_survey \
  --version 1 \
  --input '{"surveyTitle": "sample-surveyTitle", "questions": "What is workflow orchestration?", "targetAudience": "production"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w usv_user_survey -s COMPLETED -c 5

```

## How to Extend

Each worker handles one survey phase .  connect your survey platform (SurveyMonkey, Typeform, Google Forms) for distribution and your analytics backend for result analysis, and the survey workflow stays the same.

- **CreateSurveyWorker** (`usv_create`): persist surveys to a database, or create them via SurveyMonkey/Typeform API with custom branding and logic branching
- **DistributeSurveyWorker** (`usv_distribute`): send real survey invitations via SendGrid/SES for email, Twilio for SMS, or Firebase for push notifications, with audience segmentation
- **CollectResponsesWorker** (`usv_collect`): poll your survey platform or webhook endpoint for submitted responses, with configurable collection windows and response rate thresholds
- **AnalyzeSurveyWorker** (`usv_analyze`): run real NLP for theme extraction and sentiment analysis using OpenAI, AWS Comprehend, or a custom ML model instead of hardcoded scores
- **SurveyReportWorker** (`usv_report`): generate PDF reports with Apache PDFBox, post summaries to Slack, or push results to a BI dashboard like Metabase or Looker

Integrate SurveyMonkey or Typeform and the create-distribute-collect-analyze-report survey lifecycle continues unchanged.

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
user-survey/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/usersurvey/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── UserSurveyExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AnalyzeSurveyWorker.java
│       ├── CollectResponsesWorker.java
│       ├── CreateSurveyWorker.java
│       ├── DistributeSurveyWorker.java
│       └── SurveyReportWorker.java
└── src/test/java/usersurvey/workers/
    ├── AnalyzeSurveyWorkerTest.java        # 2 tests
    ├── CollectResponsesWorkerTest.java        # 3 tests
    ├── CreateSurveyWorkerTest.java        # 3 tests
    └── SurveyReportWorkerTest.java        # 2 tests

```
