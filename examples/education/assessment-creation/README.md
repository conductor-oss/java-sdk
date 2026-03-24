# Assessment Creation in Java with Conductor : Criteria Definition, Question Generation, Review, and Publishing

## The Problem

You need to create exams, quizzes, or assignments for a course. This means defining the assessment criteria based on the course's topics and desired difficulty level, generating questions that cover those criteria (multiple choice, short answer, essay), having an instructor or peer review the question bank for accuracy and fairness, and publishing the finalized assessment so students can access it. Each step depends on the previous. you cannot generate questions without criteria, and you cannot publish without review.

Without orchestration, you'd build a monolithic assessment-builder service that mixes criteria logic, question generation, review tracking, and LMS publishing into a single class. manually ensuring questions align with criteria, handling review rejection loops with ad-hoc state flags, and logging everything to figure out why a published exam contained unreviewed questions.

## The Solution

**You just write the criteria definition, question generation, review, and publishing logic. Conductor handles generation retries, review routing, and assessment version tracking.**

Each assessment concern is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of executing them in order (define criteria, create questions, review, publish), retrying if the LMS publishing API times out, tracking the full lifecycle of every assessment from criteria to published exam, and resuming from the last successful step if the process crashes.

### What You Write: Workers

Workers for criteria definition, question generation, review, and publishing each own one stage of assessment authoring.

| Worker | Task | What It Does |
|---|---|---|
| **DefineCriteriaWorker** | `asc_define_criteria` | Establishes grading criteria based on the course ID, assessment type, and selected topics |
| **CreateQuestionsWorker** | `asc_create_questions` | Generates a question bank that aligns with the defined criteria |
| **ReviewWorker** | `asc_review` | Reviews the generated questions for accuracy, difficulty balance, and coverage |
| **PublishWorker** | `asc_publish` | Publishes the reviewed assessment to the course in the learning management system |

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

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
