# Project Kickoff Automation in Java with Conductor : Scope Definition, Team Assignment, Plan Creation, and Kickoff

## Why Project Kickoff Needs Orchestration

Starting a project requires a sequence of decisions where each step depends on the one before it. You define the scope. listing objectives ("Deliver MVP", "User testing"), counting deliverables, and setting a timeline (12 weeks). You assign a team based on that scope, selecting a project lead, adding team members with the right skills, and confirming headcount fits the budget. You create a project plan that maps the scope to the team, breaking work into phases (Discovery, Design, Build, Launch) with milestones gating each transition. Finally, you formally kick off the project, setting the status to ACTIVE, recording the kickoff date, and notifying the sponsor.

Each step gates the next. you cannot assign a team without knowing the scope's skill requirements, you cannot create a plan without knowing both the scope and the team's capacity, and you cannot kick off without a finalized plan. If team assignment fails because a key role is unavailable, the plan should not be created with an incomplete team. Without orchestration, you'd build a monolithic kickoff script that mixes scope documents, HR lookups, project planning, and status updates, making it impossible to retry a failed team assignment without re-defining the scope, audit which step delayed the kickoff, or reuse the scope definition for a different team configuration.

## How This Workflow Solves It

**You just write the scope definition, team assignment, plan creation, and kickoff notification logic. Conductor handles team assignment retries, plan creation sequencing, and kickoff audit trails.**

Each kickoff stage is an independent worker. define scope, assign team, create plan, kick off. Conductor sequences them, passes the scope into team assignment, feeds both scope and team into plan creation, hands the finalized plan to kickoff, retries if an HR system is temporarily unavailable during team assignment, and records every decision from scope definition through project activation.

### What You Write: Workers

Scope definition, team assignment, plan creation, and kickoff workers each automate one preparatory phase of launching a new project.

| Worker | Task | What It Does |
|---|---|---|
| **AssignTeamWorker** | `pkf_assign_team` | Assigns the team |
| **CreatePlanWorker** | `pkf_create_plan` | Creating project plan |
| **DefineScopeWorker** | `pkf_define_scope` | Defines the scope |
| **KickOffWorker** | `pkf_kick_off` | Kick Off. Computes and returns project |

### The Workflow

```
pkf_define_scope
 │
 ▼
pkf_assign_team
 │
 ▼
pkf_create_plan
 │
 ▼
pkf_kick_off

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
