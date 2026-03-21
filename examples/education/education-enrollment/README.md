# Education Enrollment in Java with Conductor : Application, Review, Admission, Registration, and Orientation

## The Problem

You need to process new student enrollments from application to orientation. A prospective student submits an application for a program, the admissions office reviews their academic record and assigns a score based on GPA and other criteria, an admission decision is made, the admitted student is registered in the program, and finally an orientation session is scheduled. Each step depends on the previous. you cannot admit without a review score, and you cannot enroll without an admission decision.

Without orchestration, you'd build a single enrollment service that receives applications, queries GPA records, runs admission logic, inserts enrollment records, and emails orientation details. manually handling failures when the student information system is down, retrying rejected database writes, and logging every step to audit why an applicant was enrolled without completing review.

## The Solution

**You just write the application intake, academic review, admission decision, registration, and orientation scheduling logic. Conductor handles eligibility retries, seat assignment sequencing, and enrollment audit trails.**

Each enrollment concern is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of executing them in order (apply, review, admit, enroll, orient), retrying if the student information system times out, tracking every application's full journey from submission to orientation, and resuming from the last successful step if the process crashes. ### What You Write: Workers

Application intake, eligibility check, seat assignment, and confirmation workers process enrollments as a sequence of independent validations.

| Worker | Task | What It Does |
|---|---|---|
| **ApplyWorker** | `edu_apply` | Receives the student's application with name, program choice, and GPA |
| **ReviewWorker** | `edu_review` | Reviews the application and assigns a score based on GPA and program criteria |
| **AdmitWorker** | `edu_admit` | Makes the admission decision based on the review score |
| **EnrollWorker** | `edu_enroll` | Registers the admitted student in their chosen program |
| **OrientWorker** | `edu_orient` | Schedules an orientation session for the newly enrolled student |

### The Workflow

```
edu_apply
 │
 ▼
edu_review
 │
 ▼
edu_admit
 │
 ▼
edu_enroll
 │
 ▼
edu_orient

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
