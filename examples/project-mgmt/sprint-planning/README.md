# Sprint Planning Automation in Java with Conductor : Story Selection, Estimation, Assignment, and Sprint Creation

## Why Sprint Planning Needs Orchestration

Planning a sprint requires a sequence where each decision constrains the next. You select stories from the prioritized backlog that fit the team's capacity. pulling high-priority items first (US-101 "User login"), then medium (US-102 "Dashboard view"), then low (US-103 "Export CSV") until you approach the capacity limit. You estimate each selected story in points, 5 points for the login feature, 8 for the dashboard, 3 for CSV export, producing a total commitment of 16 points. You assign each estimated story to a team member based on skills and individual capacity. Alice takes US-101, Bob takes US-102, Carol takes US-103. Finally, you create the sprint, recording the sprint number, story count, total points, and setting the status to ACTIVE.

Each step depends on the previous one. you cannot estimate stories you have not selected, you cannot assign stories without knowing their point values (a 3-point story and an 8-point story require different capacity from the assignee), and you cannot create the sprint without finalized assignments. If estimation reveals that the selected stories exceed capacity (e.g., 16 points selected for a 15-point team), you need to drop the lowest-priority story, not re-select from the backlog. Without orchestration, you'd build a monolithic planning script that mixes backlog queries, estimation logic, team availability lookups, and sprint creation, making it impossible to swap your estimation method (planning poker vs: t-shirt sizing), retry a failed Jira API call without re-estimating, or audit why a specific story was assigned to a specific developer.

## How This Workflow Solves It

**You just write the story selection, estimation, developer assignment, and sprint creation logic. Conductor handles capacity retries, task assignment sequencing, and sprint audit trails.**

Each sprint planning stage is an independent worker. select stories, estimate, assign, create sprint. Conductor sequences them, passes the selected stories into estimation, feeds the estimated stories into assignment, hands the finalized assignments to sprint creation, retries if your project management tool's API times out during story selection, and records every planning decision for retrospective analysis.

### What You Write: Workers

Backlog analysis, capacity calculation, sprint goal setting, and task assignment workers each handle one aspect of iteration planning.

| Worker | Task | What It Does |
|---|---|---|
| **AssignWorker** | `spn_assign` | Assigning stories to team members |
| **CreateSprintWorker** | `spn_create_sprint` | Creates the sprint |
| **EstimateWorker** | `spn_estimate` | Estimating stories |
| **SelectStoriesWorker** | `spn_select_stories` | Selecting stories for capacity |

### The Workflow

```
spn_select_stories
 │
 ▼
spn_estimate
 │
 ▼
spn_assign
 │
 ▼
spn_create_sprint

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
