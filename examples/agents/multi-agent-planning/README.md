# Architect Designs, Three Estimators Work in Parallel, PM Creates Timeline

An architect produces frontend and backend component lists. Three estimators (frontend, backend, infra) work in parallel via FORK_JOIN, each producing per-component effort estimates. The PM creates a timeline with milestones like `{name: "Infrastructure Ready", week: infraCalendar}`.

## Workflow

```
projectName, requirements
  -> pp_architect_design
  -> FORK_JOIN(pp_estimate_frontend | pp_estimate_backend | pp_estimate_infra)
  -> pp_pm_timeline
```

## Tests

47 tests cover architecture design, all three estimation tracks, and timeline generation.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
