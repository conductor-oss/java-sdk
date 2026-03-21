# Hierarchical Agents in Java Using Conductor : Manager, Team Leads, and Workers in a Development Org

Hierarchical agents. manager plans, team leads delegate to workers in parallel branches, manager merges results. ## Software Projects Need Hierarchical Coordination

A real development team has a manager who plans the work, team leads who coordinate their teams, and individual contributors who write code. The backend lead assigns API and database tasks; the frontend lead assigns UI and styling tasks. Both teams work simultaneously. The manager reviews everything at the end.

Modeling this hierarchy in an agent system requires multi-level parallelism: the two teams run in parallel, and within each team, the workers run in parallel with their lead. Six agents working on different aspects of the same project need to produce outputs that are consistent and mergeable. Without orchestration, coordinating a 6-agent hierarchy with two levels of parallelism, proper synchronization at each level, and a final merge step requires complex concurrency code.

## The Solution

**You write the manager, lead, and developer agent logic. Conductor handles nested parallelism, cross-team synchronization, and output merging.**

`ManagerPlanWorker` creates the project plan with feature requirements and team assignments. `FORK_JOIN` launches two parallel branches: the backend branch runs `LeadBackendWorker` (coordination) followed by `WorkerApiWorker` and `WorkerDbWorker` (implementation), while the frontend branch runs `LeadFrontendWorker` (coordination) followed by `WorkerUiWorker` and `WorkerStylingWorker` (implementation). After `JOIN` synchronizes both teams, `ManagerMergeWorker` reviews all outputs, checks for integration issues between frontend and backend, and produces the final deliverable. Conductor handles the nested parallelism and tracks each agent's contribution.

### What You Write: Workers

Eight agents form a hierarchy, the manager plans, team leads delegate to frontend and backend developers in parallel branches, and the manager merges all outputs.

| Worker | Task | What It Does |
|---|---|---|
| **LeadBackendWorker** | `hier_lead_backend` | Backend team lead that receives the backend workstream and breaks it down into an API task and a database task for th... |
| **LeadFrontendWorker** | `hier_lead_frontend` | Frontend team lead that receives the frontend workstream and breaks it down into a UI task and a styling task for the... |
| **ManagerMergeWorker** | `hier_manager_merge` | Manager agent that merges backend and frontend results into a final project report, computing total lines of code acr... |
| **ManagerPlanWorker** | `hier_manager_plan` | Manager agent that takes a project description and deadline, then produces a backend plan and a frontend plan for the... |
| **WorkerApiWorker** | `hier_worker_api` | API developer worker that implements the REST endpoints defined by the backend lead. |
| **WorkerDbWorker** | `hier_worker_db` | Database developer worker that builds the schema and migrations based on the DB task from the lead and the API output. |
| **WorkerStylingWorker** | `hier_worker_styling` | Styling developer worker that applies the design system, responsive layout, and theme support based on the styling ta... |
| **WorkerUiWorker** | `hier_worker_ui` | UI developer worker that builds the page components defined by the frontend lead. |

Workers implement agent decisions and tool calls with realistic outputs so you can see the routing and handoff patterns without live LLM calls. Add your API keys to switch to live mode. the agent workflow stays the same.

### The Workflow

```
hier_manager_plan
 │
 ▼
FORK_JOIN
 ├── hier_lead_backend -> hier_worker_api -> hier_worker_db
 └── hier_lead_frontend -> hier_worker_ui -> hier_worker_styling
 │
 ▼
JOIN (wait for all branches)
hier_manager_merge

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
