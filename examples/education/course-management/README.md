# Course Management in Java with Conductor :  Course Creation, Scheduling, Instructor Assignment, and Catalog Publishing

A Java Conductor workflow example for setting up a new course .  creating the course record with department and credit-hour details, scheduling class sessions for a semester, assigning a qualified instructor from the department, and publishing the fully configured course to the student-facing catalog. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers ,  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## The Problem

You need to stand up a new course offering each semester. This means creating the course record in your student information system with the correct department and credit hours, scheduling class sessions into available time slots and rooms, assigning an instructor whose qualifications and availability match, and finally publishing the course to the catalog so students can register. Publishing a course without an assigned instructor or without scheduled sessions creates registration chaos.

Without orchestration, you'd build a single course-setup script that creates the record, queries room availability, checks faculty schedules, and pushes to the catalog .  manually handling conflicts when the chosen room is double-booked, retrying failed database writes, and logging every step to debug why a course appeared in the catalog without an instructor.

## The Solution

**You just write the course creation, scheduling, instructor assignment, and catalog publishing logic. Conductor handles enrollment retries, scheduling coordination, and course lifecycle tracking.**

Each course setup concern is a simple, independent worker .  a plain Java class that does one thing. Conductor takes care of executing them in order (create, schedule, assign instructor, publish), retrying if the scheduling system is temporarily unavailable, tracking every course's setup lifecycle from creation to publication, and resuming from the last successful step if the process crashes. You get all of that for free, without writing a single line of orchestration code.

### What You Write: Workers

Course creation, scheduling, enrollment management, and completion tracking workers handle distinct administrative functions independently.

| Worker | Task | What It Does |
|---|---|---|
| **CreateCourseWorker** | `crs_create` | Creates a new course record with name, department, and credit hours |
| **ScheduleCourseWorker** | `crs_schedule` | Assigns class sessions to time slots and rooms for the given semester |
| **AssignInstructorWorker** | `crs_assign_instructor` | Selects and assigns a qualified instructor from the department |
| **PublishCourseWorker** | `crs_publish` | Publishes the fully configured course to the student registration catalog |

Workers simulate educational operations .  enrollment, grading, notifications ,  with realistic outputs. Replace with real LMS and SIS integrations and the workflow stays the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

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

## Example Output

```
=== Example 672: Course Management ===

Step 1: Registering task definitions...
  Registered: crs_create, crs_schedule, crs_assign_instructor, crs_publish

Step 2: Registering workflow 'crs_course_management'...
  Workflow registered.

Step 3: Starting workers...
  4 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [assign]
  [create] Course:
  [publish]
  [schedule]

  Status: COMPLETED
  Output: {instructor=..., courseId=..., published=..., enrollmentOpen=...}

Result: PASSED
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
docker run -d -p 8080:8080 -p 1234:5000 orkesio/orkes-conductor-standalone:latest

# Wait for Conductor to be ready
until curl -sf http://localhost:8080/health > /dev/null; do sleep 2; done

# Build and run
mvn package -DskipTests
java -jar target/course-management-1.0.0.jar
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
java -jar target/course-management-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow crs_course_management \
  --version 1 \
  --input '{"courseName": "sample-name", "Data Structures and Algorithms": "sample-Data Structures and Algorithms", "department": "sample-department", "Computer Science": "sample-Computer Science", "credits": "sample-credits", "semester": "sample-semester"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w crs_course_management -s COMPLETED -c 5
```

## How to Extend

Point each worker at your real academic systems .  your SIS (Banner, PeopleSoft) for course records, your room scheduling system (25Live, Ad Astra) for sessions, your faculty database for instructor assignment, and the workflow runs identically in production.

- **CreateCourseWorker** (`crs_create`): insert the course record into your SIS (Banner, PeopleSoft, Workday Student) with proper department codes and credit-hour mappings
- **ScheduleCourseWorker** (`crs_schedule`): query your room/scheduling system (25Live, Ad Astra, EMS) for available time slots and classrooms that match capacity requirements
- **AssignInstructorWorker** (`crs_assign_instructor`): check faculty availability and qualifications against the department's teaching load database; respect union rules and preference rankings
- **PublishCourseWorker** (`crs_publish`): push the finalized course to your registration catalog API and notify the advising system so students can add it to their plans

Connect to a different LMS or scheduling system and the management workflows stay intact.

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
course-management/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/coursemanagement/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── CourseManagementExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AssignInstructorWorker.java
│       ├── CreateCourseWorker.java
│       ├── PublishCourseWorker.java
│       └── ScheduleCourseWorker.java
└── src/test/java/coursemanagement/workers/
    ├── AssignInstructorWorkerTest.java        # 2 tests
    ├── CreateCourseWorkerTest.java        # 3 tests
    ├── PublishCourseWorkerTest.java        # 2 tests
    └── ScheduleCourseWorkerTest.java        # 2 tests
```
