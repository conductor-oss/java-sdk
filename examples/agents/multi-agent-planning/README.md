# Multi-Agent Planning in Java Using Conductor : Architect, Parallel Estimators, Project Manager Timeline

Multi-Agent Project Planning. architect designs the system, three estimators run in parallel (frontend, backend, infra), then PM builds the timeline. ## Project Planning Needs Multiple Expert Perspectives

A project estimate from a single person is always biased toward their specialty. The backend engineer underestimates frontend work. The infrastructure engineer forgets about API development. An accurate plan needs domain experts estimating their own areas independently. frontend complexity, backend API surface, infrastructure provisioning, with an architect providing the overall design and a PM resolving the estimates into a realistic timeline.

The three estimation areas are independent and can run simultaneously: the frontend estimator doesn't need to wait for the backend estimate, and vice versa. But all three must complete before the PM can build the timeline. If the infrastructure estimator times out, the frontend and backend estimates are still valid. you just need to retry that one estimation.

## The Solution

**You write the architecture design, domain-specific estimations, and timeline logic. Conductor handles parallel estimation, result collection, and milestone assembly.**

`ArchitectDesignWorker` creates the system design with components, interfaces, and technology choices that guide the estimators. `FORK_JOIN` dispatches three estimators in parallel: `EstimateFrontendWorker` estimates UI components, pages, and client-side complexity. `EstimateBackendWorker` estimates API endpoints, business logic, and integrations. `EstimateInfraWorker` estimates infrastructure provisioning, CI/CD setup, and operational overhead. After `JOIN` collects all three estimates, `PmTimelineWorker` builds a project timeline with milestones, dependencies, critical path identification, and risk buffers. Conductor runs the three estimations in parallel and tracks each estimator's output for estimation accuracy analysis over time.

### What You Write: Workers

Five agents plan the project, the architect designs the system, three estimators run in parallel for frontend, backend, and infrastructure, and the PM assembles the timeline.

| Worker | Task | What It Does |
|---|---|---|
| **ArchitectDesignWorker** | `pp_architect_design` | Architect agent. takes projectName and requirements, produces a system architecture with frontend components, backen.. |
| **EstimateBackendWorker** | `pp_estimate_backend` | Backend estimation agent. takes components, complexity, and teamSize, computes a per-component breakdown (designWeek.. |
| **EstimateFrontendWorker** | `pp_estimate_frontend` | Frontend estimation agent. takes components, complexity, and teamSize, computes a per-component breakdown (designWee.. |
| **EstimateInfraWorker** | `pp_estimate_infra` | Infrastructure estimation agent. takes components, complexity, and teamSize, computes a per-component breakdown (set.. |
| **PmTimelineWorker** | `pp_pm_timeline` | PM timeline agent. takes all three estimates and the architecture summary, computes total project duration = infraCa.. |

Workers implement agent decisions and tool calls with realistic outputs so you can see the routing and handoff patterns without live LLM calls. Add your API keys to switch to live mode. the agent workflow stays the same.

### The Workflow

```
pp_architect_design
 │
 ▼
FORK_JOIN
 ├── pp_estimate_frontend
 ├── pp_estimate_backend
 └── pp_estimate_infra
 │
 ▼
JOIN (wait for all branches)
pp_pm_timeline

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
