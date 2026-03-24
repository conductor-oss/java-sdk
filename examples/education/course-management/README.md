# Course Management

Orchestrates course management through a multi-stage Conductor workflow.

**Input:** `courseName`, `department`, `credits`, `semester` | **Timeout:** 60s

## Pipeline

```
crs_create
    │
crs_schedule
    │
crs_assign_instructor
    │
crs_publish
```

## Workers

**AssignInstructorWorker** (`crs_assign_instructor`): Assigns an instructor to the course.

Reads `courseId`. Outputs `instructor`.

**CreateCourseWorker** (`crs_create`): Creates a new course entry.

Reads `courseName`, `credits`, `department`. Outputs `courseId`.

**PublishCourseWorker** (`crs_publish`): Publishes the course to the catalog.

Reads `courseId`. Outputs `published`, `enrollmentOpen`.

**ScheduleCourseWorker** (`crs_schedule`): Schedules course sessions for the semester.

```java
Map<String, String> schedule = Map.of(
```

Reads `courseId`. Outputs `schedule`.

## Tests

**9 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
