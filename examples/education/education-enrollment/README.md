# Education Enrollment

Orchestrates education enrollment through a multi-stage Conductor workflow.

**Input:** `studentName`, `program`, `gpa` | **Timeout:** 60s

## Pipeline

```
edu_apply
    │
edu_review
    │
edu_admit
    │
edu_enroll
    │
edu_orient
```

## Workers

**AdmitWorker** (`edu_admit`): Decides whether to admit the student based on review score.

```java
boolean admitted = reviewScore >= 75;
```

Reads `applicationId`, `reviewScore`. Outputs `admitted`, `studentId`.

**ApplyWorker** (`edu_apply`): Processes student application submission.

Reads `program`, `studentName`. Outputs `applicationId`.

**EnrollWorker** (`edu_enroll`): Enrolls an admitted student in their program.

Reads `program`, `studentId`. Outputs `enrolled`, `semester`.

**OrientWorker** (`edu_orient`): Schedules orientation for a newly enrolled student.

Reads `studentId`. Outputs `orientationDate`, `location`.

**ReviewWorker** (`edu_review`): Reviews a student application and assigns a score based on GPA.

```java
int score = (int) Math.min(100, Math.round(gpa * 25));
```

Reads `applicationId`, `gpa`. Outputs `score`, `reviewer`.

## Tests

**23 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
