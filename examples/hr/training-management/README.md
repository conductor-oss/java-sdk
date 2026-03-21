# Training Management in Java with Conductor : Course Assignment, Progress Tracking, Assessment, Certification, and Record Keeping

## The Problem

You need to manage employee training from assignment through certification. An employee is assigned to a course. whether mandatory compliance training (OSHA, HIPAA, anti-harassment), role-specific skills training, or professional development. The system must track the employee's progress through the course modules and verify completion. Upon finishing the material, the employee takes an assessment; a passing score (e.g., 80%+) earns a certification with an issue date and expiration. The certification must be recorded in the employee's permanent training record for audit purposes. Each step depends on the previous, you cannot assess without tracking completion, and you cannot certify without a passing assessment score. Missed compliance training exposes the organization to regulatory fines and liability.

Without orchestration, you'd manage training through spreadsheets and LMS reports. HR assigns courses, manually checks who completed them, reviews assessment results, and updates certification records one by one. If the LMS is down when recording certifications, the employee completed the training but has no record of it. If someone completes a course but the assessment results are not linked to the certification, they may be counted as non-compliant. Auditors for OSHA, HIPAA, and SOX compliance require proof that every employee completed their required training within the mandated timeframe.

## The Solution

**You just write the course assignment, progress tracking, assessment administration, certification, and record keeping logic. Conductor handles enrollment retries, progress tracking, and certification audit trails.**

Each stage of training management is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of tracking progress only after assignment, assessing only after course completion, certifying only if the assessment score meets the passing threshold, recording the certification as the final step, retrying if the LMS or HRIS is temporarily unavailable, and maintaining a complete audit trail for compliance reporting. ### What You Write: Workers

Course assignment, enrollment, progress tracking, and certification workers each manage one phase of employee skill development.

| Worker | Task | What It Does |
|---|---|---|
| **AssignWorker** | `trm_assign` | Enrolls the employee in the course, generates an enrollment ID, and sets a completion due date |
| **TrackWorker** | `trm_track` | Monitors the employee's progress through course modules and verifies they have completed all required content |
| **AssessWorker** | `trm_assess` | Administers the final assessment and returns the score, tracking which questions were answered correctly |
| **CertifyWorker** | `trm_certify` | Issues a certification based on the assessment score, with an issue date and expiration date for recertification tracking |
| **RecordWorker** | `trm_record` | Records the completed certification in the employee's permanent training record in the HRIS for compliance audits |

### The Workflow

```
trm_assign
 │
 ▼
trm_track
 │
 ▼
trm_assess
 │
 ▼
trm_certify
 │
 ▼
trm_record

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
