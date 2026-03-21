# Grading Workflow in Java with Conductor : Submission, Scoring, Review, Recording, and Student Notification

## The Problem

You need to grade student assignments end-to-end. A student submits their work, the grading system scores it against the assignment rubric, an instructor or peer reviews the score for fairness and accuracy, the final grade is recorded in the course gradebook, and the student is notified of their result. Recording a grade before review is complete risks posting inaccurate scores; failing to notify leaves students in the dark about their performance.

Without orchestration, you'd build a single grading script that ingests submissions, runs scoring logic, updates the gradebook, and sends notification emails. manually ensuring grades are never recorded without review, retrying when the gradebook database is locked, and logging every step to investigate grade disputes when a student claims they were scored incorrectly.

## The Solution

**You just write the submission intake, rubric scoring, grade review, gradebook recording, and student notification logic. Conductor handles scoring retries, grade routing, and complete assessment audit trails.**

Each grading concern is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of executing them in order (submit, grade, review, record, notify), retrying if the gradebook service is temporarily unavailable, maintaining a complete audit trail of every grade from submission to notification, and resuming from the last successful step if the process crashes. ### What You Write: Workers

Submission collection, grading, review, and grade posting workers each handle one phase of the assessment evaluation cycle.

| Worker | Task | What It Does |
|---|---|---|
| **SubmitWorker** | `grd_submit` | Receives and logs the student's assignment submission |
| **GradeWorker** | `grd_grade` | Scores the submission against the assignment rubric |
| **ReviewWorker** | `grd_review` | Reviews the assigned score for accuracy and fairness, producing a final score |
| **RecordWorker** | `grd_record` | Records the final score in the course gradebook |
| **NotifyWorker** | `grd_notify` | Notifies the student of their final grade |

### The Workflow

```
grd_submit
 │
 ▼
grd_grade
 │
 ▼
grd_review
 │
 ▼
grd_record
 │
 ▼
grd_notify

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
