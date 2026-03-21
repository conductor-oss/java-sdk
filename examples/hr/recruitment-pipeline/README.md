# Recruitment Pipeline in Java with Conductor :  Job Posting, Resume Screening, Interview, Evaluation, and Offer

A Java Conductor workflow example for recruitment. posting a job requisition to job boards, screening candidate resumes against requirements, conducting structured interviews, evaluating composite scores from screening and interview, and extending an offer to the top candidate. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## The Problem

You need to manage the hiring pipeline from job posting through offer letter. A hiring manager opens a requisition for a role in their department. The job must be posted to job boards and the company careers page. As applications arrive, each candidate's resume must be screened against the role's requirements. years of experience, required skills, education,  producing a screening score (e.g., 85/100). Candidates who pass screening move to interviews, where structured questions produce an interview score. Both scores feed into a composite evaluation that generates a hire/no-hire recommendation. Top candidates receive an offer with compensation details. Each step depends on the previous,  you cannot interview without screening, and you cannot make an offer without a composite evaluation.

Without orchestration, you'd build a monolithic ATS (applicant tracking system) that posts jobs, parses resumes, tracks interview feedback in spreadsheets, and generates offer letters manually. If the job board API is temporarily down when posting, you'd need retry logic. If the system crashes after the interview but before the evaluation is recorded, the candidate is left in limbo with no decision. Recruiters have no single dashboard showing where each candidate stands in the pipeline, and EEOC compliance requires documenting the objective criteria used at each stage.

## The Solution

**You just write the job posting, resume screening, interview coordination, scoring, and offer extension logic. Conductor handles screening retries, candidate routing, and hiring funnel audit trails.**

Each stage of the recruitment pipeline is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of screening only after the job is posted, interviewing only after screening passes, evaluating using both the screening score and interview score, extending the offer only after a positive recommendation, retrying if job boards or HRIS systems are temporarily unavailable, and maintaining a complete audit trail for EEOC and OFCCP compliance. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Job posting, application screening, shortlisting, and offer extension workers handle talent acquisition as a multi-stage funnel.

| Worker | Task | What It Does |
|---|---|---|
| **PostWorker** | `rcp_post` | Posts the job requisition to job boards and the careers page with title, department, and requirements, returning a job ID |
| **ScreenWorker** | `rcp_screen` | Screens the candidate's resume against the job requirements, scoring qualifications on a 0-100 scale and returning a pass/fail decision |
| **InterviewWorker** | `rcp_interview` | Records structured interview results with competency ratings and an overall interview score |
| **EvaluateWorker** | `rcp_evaluate` | Combines the screening score and interview score into a composite evaluation with a hire/no-hire recommendation |
| **OfferWorker** | `rcp_offer` | Generates and extends the offer to the recommended candidate with compensation, start date, and terms |

Workers implement HR operations. onboarding tasks, approvals, provisioning,  with realistic outputs. Replace with real HRIS and identity provider integrations and the workflow stays the same.

### The Workflow

```
rcp_post
    │
    ▼
rcp_screen
    │
    ▼
rcp_interview
    │
    ▼
rcp_evaluate
    │
    ▼
rcp_offer

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
java -jar target/recruitment-pipeline-1.0.0.jar

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
java -jar target/recruitment-pipeline-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow rcp_recruitment_pipeline \
  --version 1 \
  --input '{"jobTitle": "sample-jobTitle", "department": "engineering", "candidateName": "test"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w rcp_recruitment_pipeline -s COMPLETED -c 5

```

## How to Extend

Point each worker at your real recruitment tools. LinkedIn or Indeed APIs for job posting, your ATS for resume screening, your interview platform for candidate evaluation, and the workflow runs identically in production.

- **PostWorker** → post to real job boards (Indeed, LinkedIn, Glassdoor) via their APIs and syndicate to your company careers page through your ATS
- **ScreenWorker** → use AI-powered resume parsing (Textkernel, Sovren) to extract skills and experience, then score against the job's required and preferred qualifications
- **InterviewWorker** → integrate with your interview scheduling platform (GoodTime, Calendly) and collect structured scorecards from each interviewer
- **EvaluateWorker** → apply weighted rubrics combining screening, interview, and assessment scores with configurable weights per role level
- **OfferWorker** → generate offer letters from templates in your HRIS, route for compensation committee approval, and send via DocuSign for e-signature
- Add a **AssessmentWorker** between screening and interview to administer skills tests or coding challenges (HackerRank, Codility) and factor scores into the evaluation
- Add a **BackgroundCheckWorker** after offer acceptance to initiate background verification before the start date

Integrate a new ATS or screening tool and the recruitment pipeline adapts seamlessly.

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
recruitment-pipeline-recruitment-pipeline/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/recruitmentpipeline/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── RecruitmentPipelineExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── EvaluateWorker.java
│       ├── InterviewWorker.java
│       ├── OfferWorker.java
│       ├── PostWorker.java
│       └── ScreenWorker.java
└── src/test/java/recruitmentpipeline/workers/
    ├── EvaluateWorkerTest.java        # 2 tests
    ├── InterviewWorkerTest.java        # 2 tests
    ├── OfferWorkerTest.java        # 2 tests
    ├── PostWorkerTest.java        # 2 tests
    └── ScreenWorkerTest.java        # 2 tests

```
