# Course Management in Java with Conductor : Course Creation, Scheduling, Instructor Assignment, and Catalog Publishing

## The Problem

You need to stand up a new course offering each semester. This means creating the course record in your student information system with the correct department and credit hours, scheduling class sessions into available time slots and rooms, assigning an instructor whose qualifications and availability match, and finally publishing the course to the catalog so students can register. Publishing a course without an assigned instructor or without scheduled sessions creates registration chaos.

Without orchestration, you'd build a single course-setup script that creates the record, queries room availability, checks faculty schedules, and pushes to the catalog. manually handling conflicts when the chosen room is double-booked, retrying failed database writes, and logging every step to debug why a course appeared in the catalog without an instructor.

## The Solution

**You just write the course creation, scheduling, instructor assignment, and catalog publishing logic. Conductor handles enrollment retries, scheduling coordination, and course lifecycle tracking.**

Each course setup concern is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of executing them in order (create, schedule, assign instructor, publish), retrying if the scheduling system is temporarily unavailable, tracking every course's setup lifecycle from creation to publication, and resuming from the last successful step if the process crashes. ### What You Write: Workers

Course creation, scheduling, enrollment management, and completion tracking workers handle distinct administrative functions independently.

| Worker | Task | What It Does |
|---|---|---|
| **CreateCourseWorker** | `crs_create` | Creates a new course record with name, department, and credit hours |
| **ScheduleCourseWorker** | `crs_schedule` | Assigns class sessions to time slots and rooms for the given semester |
| **AssignInstructorWorker** | `crs_assign_instructor` | Selects and assigns a qualified instructor from the department |
| **PublishCourseWorker** | `crs_publish` | Publishes the fully configured course to the student registration catalog |

### The Workflow

```
crs_create
 │
 ▼
crs_schedule
 │
 ▼
crs_assign_instructor
 │
 ▼
crs_publish

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
