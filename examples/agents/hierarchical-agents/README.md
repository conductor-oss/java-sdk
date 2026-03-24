# Manager-Leads-Workers Hierarchy for a Development Project

A manager plans the project, then two teams work in parallel via FORK_JOIN: backend (lead -> API worker -> DB worker) and frontend (lead -> UI worker -> styling worker). The manager merges all outputs into a project report.

## Workflow

```
project, deadline
  -> hier_manager_plan
  -> FORK_JOIN(
       [hier_lead_backend -> hier_worker_api -> hier_worker_db],
       [hier_lead_frontend -> hier_worker_ui -> hier_worker_styling]
     )
  -> hier_manager_merge
```

## Workers

**ManagerPlanWorker** -- Plans backend with `endpoints: ["/api/users", "/api/projects", "/api/tasks"]` and frontend with pages.

**LeadBackendWorker** -- Delegates API endpoints to workers.

**WorkerApiWorker** -- Builds endpoint details for each path.

**WorkerDbWorker** -- Creates table schemas for each table name.

**LeadFrontendWorker** -- Delegates UI pages to workers.

**WorkerUiWorker** -- Builds UI components per page.

**WorkerStylingWorker** -- Sets up design system and responsive layout.

**ManagerMergeWorker** -- Merges backend + frontend into `status: "completed"`.

## Tests

44 tests cover the full hierarchy from manager planning through worker execution to merge.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
