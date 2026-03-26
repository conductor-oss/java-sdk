# Project Closure

Project closure: review deliverables, sign off, archive, lessons learned.

**Input:** `projectId`, `projectName`, `manager` | **Timeout:** 60s

## Pipeline

```
pcl_review_deliverables
    │
pcl_sign_off
    │
pcl_archive
    │
pcl_lessons_learned
```

## Workers

**ArchiveWorker** (`pcl_archive`)

Reads `projectId`. Outputs `archiveId`, `location`, `archived`.

**LessonsLearnedWorker** (`pcl_lessons_learned`)

Reads `projectId`. Outputs `report`.

**ReviewDeliverablesWorker** (`pcl_review_deliverables`)

Reads `projectId`. Outputs `deliverables`, `allComplete`.

**SignOffWorker** (`pcl_sign_off`)

Reads `projectId`. Outputs `signOff`, `signedBy`, `date`.

## Tests

**8 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
