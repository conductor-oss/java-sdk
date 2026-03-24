# Student Progress in Java with Conductor : Grade Collection, GPA Analysis, Progress Reports, and Notifications

## The Problem

You need to evaluate a student's academic progress at the end of each semester. This means pulling grades from all enrolled courses, computing the semester GPA and cumulative standing (good standing, probation, dean's list), generating a formal progress report for the student's academic record, and notifying the student of their standing. Sending a progress report with incorrect GPA calculations undermines institutional credibility; failing to flag probation status delays critical academic interventions.

Without orchestration, you'd build a single end-of-semester batch job that queries the gradebook, runs GPA calculations, generates PDF reports, and sends email notifications. manually handling missing grades from courses with incomplete submissions, retrying when the report generation service crashes, and logging everything to investigate discrepancies when a student disputes their standing.

## The Solution

**You just write the grade collection, GPA analysis, progress report generation, and student notification logic. Conductor handles data collection retries, alert routing, and progress tracking across terms.**

Each progress-tracking concern is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of executing them in order (collect grades, analyze, generate report, notify), retrying if the gradebook service is temporarily unavailable, maintaining an audit trail of every progress evaluation, and resuming from the last successful step if the process crashes.

### What You Write: Workers

Data collection, performance analysis, alert generation, and report distribution workers monitor student outcomes through independent checkpoints.

| Worker | Task | What It Does |
|---|---|---|
| **CollectGradesWorker** | `spr_collect_grades` | Pulls all course grades for the student for the specified semester |
| **AnalyzeWorker** | `spr_analyze` | Computes semester GPA and determines academic standing (good standing, probation, dean's list) |
| **GenerateReportWorker** | `spr_generate_report` | Creates a formal progress report with GPA, standing, and course-by-course breakdown |
| **NotifyWorker** | `spr_notify` | Notifies the student of their GPA and academic standing |

### The Workflow

```
spr_collect_grades
 │
 ▼
spr_analyze
 │
 ▼
spr_generate_report
 │
 ▼
spr_notify

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
