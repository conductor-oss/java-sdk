# Task Assignment Automation in Java with Conductor : Task Analysis, Skill Matching, Assignment, Notification, and Tracking

## Why Task Assignment Needs Orchestration

Assigning tasks to the right person requires a pipeline where each step narrows the decision. You analyze the task. parsing the title and description to extract required skills (e.g., JavaScript, React) and assessing complexity (low, medium, high). You match those skills against your team, scoring each team member on skill overlap and checking availability, producing a best match with a compatibility score (e.g., Alice at 95% match, availability "open"). You formally assign the task to the selected candidate. You notify the assignee so they know work is waiting. You set up tracking, recording the assignee, setting the status to IN_PROGRESS, and computing a due date based on complexity.

Each step depends on the previous one. skill matching needs the analyzed skill list, assignment needs the best match candidate, notification needs the confirmed assignee, and tracking needs the assignment confirmation. If the best-match team member becomes unavailable between matching and assignment (they got pulled into an incident), the assignment step should fail and retry after the skill matcher finds the next-best candidate, not silently assign to an unavailable person. Without orchestration, you'd build a monolithic assignment function that mixes NLP task analysis, team directory lookups, PM tool API calls, Slack notifications, and status updates, making it impossible to swap your skill matching algorithm, add a new notification channel, or audit why a specific task was assigned to a specific person.

## How This Workflow Solves It

**You just write the task analysis, skill matching, assignment, notification, and tracking logic. Conductor handles skill matching retries, notification delivery, and assignment audit trails.**

Each assignment stage is an independent worker. analyze, match skills, assign, notify, track. Conductor sequences them, passes the extracted skills into matching, feeds the best match candidate into assignment, hands the confirmed assignee to notification and tracking, retries if your team directory API is temporarily unavailable during skill matching, and records every decision from task analysis through tracking setup.

### What You Write: Workers

Workload analysis, skill matching, assignment, and notification workers each own one step of distributing work to team members.

| Worker | Task | What It Does |
|---|---|---|
| **AnalyzeWorker** | `tas_analyze` | Analyzes and returns skills, complexity |
| **AssignWorker** | `tas_assign` | Assigning task to candidate |
| **MatchSkillsWorker** | `tas_match_skills` | Finding best match for skills |
| **NotifyWorker** | `tas_notify` | Notify. Computes and returns notified, channel |
| **TrackWorker** | `tas_track` | Tracks task progress and updates completion status for the assignee |

### The Workflow

```
tas_analyze
 │
 ▼
tas_match_skills
 │
 ▼
tas_assign
 │
 ▼
tas_notify
 │
 ▼
tas_track

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
