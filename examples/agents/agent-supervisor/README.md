# Agent Supervisor in Java Using Conductor: Plan, Fan Out to Coder/Tester/Documenter, Review

Assign code review to three unsupervised AI agents and watch what happens: one rewrites your architecture unprompted, another nitpicks formatting while ignoring logic bugs, and the third hallucinates a critical security vulnerability that doesn't exist. Without a supervisor to define the plan upfront and gate the output afterward, parallel agents produce inconsistent, uncoordinated work that nobody catches until it ships. This example uses [Conductor](https://github.com/conductor-oss/conductor) to implement the supervisor pattern, a planning worker defines the scope, `FORK_JOIN` fans out to coder, tester, and documenter agents in parallel, and a review worker inspects all outputs against the original plan before anything is delivered.

## Coordinating Specialist Agents Needs a Supervisor

Building a feature requires coding, testing, and documentation. three tasks that can run in parallel but all depend on the same plan and all need quality review afterward. The coder implements the feature (AuthController, AuthService, AuthRepository, 245 lines of Java with JWT handling). The tester writes and runs test cases (unit tests, integration tests, edge cases, 18 tests, 82% coverage). The documenter creates API docs, architecture diagrams, and a changelog.

Without a supervisor, these agents work independently with no shared plan and no quality gate. The coder might implement something the tester doesn't cover. The documenter might describe an API that doesn't match the implementation. The supervisor pattern adds two coordination steps: planning before execution (so all agents work from the same specification) and review after execution (so inconsistencies are caught before delivery).

## The Solution

**You write the planning, agent, and review logic. Conductor handles parallel fan-out, synchronization, and quality tracking.**

`PlanWorker` (the supervisor) creates a development plan with feature requirements, tasks for each specialist, and acceptance criteria. `FORK_JOIN` fans out to three agents in parallel: `CoderAgentWorker` implements the feature and reports files created and lines of code, `TesterAgentWorker` writes and runs tests reporting coverage and pass rates, and `DocumenterAgentWorker` generates API docs and architecture documentation. After `JOIN` synchronizes all three, `ReviewWorker` (the supervisor again) inspects all outputs against the plan, flags inconsistencies, and produces a quality report with per-agent scores. Conductor handles the parallel execution, waits for all agents to finish, and tracks each agent's execution time and output.

### What You Write: Workers

A supervisor plans the work, three specialist agents (coder, tester, documenter) execute in parallel, and the supervisor reviews all outputs for quality.

| Worker | Task | What It Does |
|---|---|---|
| **PlanWorker** | `sup_plan` | Creates a development plan for the requested feature. Returns a plan object with feature name, priority, 5 development phases (design, implementation, testing, documentation, review), a deadline, and task descriptions for each specialist agent. |
| **CoderAgentWorker** | `sup_coder_agent` | Implements the feature. Returns implementation results: 3 files created (AuthController, AuthService, AuthRepository), 245 lines of Java, and implementation notes describing JWT token handling and role-based access control. |
| **TesterAgentWorker** | `sup_tester_agent` | Creates and runs test suites. Returns test execution results: 3 test suites, 18 total tests, 17 passed, 1 failed (testTokenExpirationEdgeCase), 82% coverage. Status is "needs_fix" due to the failing test. |
| **DocumenterAgentWorker** | `sup_documenter_agent` | Generates documentation. Returns 3 documents created (API_REFERENCE.md, SETUP_GUIDE.md, EXAMPLES.md), 5 sections (Overview, Authentication Flow, API Endpoints, Configuration, Troubleshooting), and 1200 words total. |
| **ReviewWorker** | `sup_review` | Reviews all agent outputs against the plan. Determines overall status: APPROVED if all agents pass, NEEDS_REVISION if any agent has issues. Returns action items (fix failing test, increase coverage, add error handling docs) and metrics (lines of code, test count, documentation word count). |

The demo workers produce realistic, deterministic output shapes so the workflow runs end-to-end. To go to production, replace the simulation with the real API call, the worker interface stays the same, and no workflow changes are needed.

### The Workflow

```
sup_plan
 |
 v
FORK_JOIN
 +-- sup_coder_agent
 +-- sup_tester_agent
 +-- sup_documenter_agent
 |
 v
JOIN (wait for all branches)
 |
 v
sup_review

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
