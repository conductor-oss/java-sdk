# Tree of Thought in Java Using Conductor : Explore Three Solution Paths in Parallel, Evaluate, Select Best

Tree of Thought. define a problem, explore three parallel reasoning paths (analytical, creative, empirical), evaluate all paths, and select the best solution.

## Some Problems Have Multiple Solution Approaches

"How should we reduce cloud infrastructure costs by 30%?" has at least three valid approaches: right-sizing instances (analyze usage, downsize over-provisioned resources), reserved capacity (commit to 1-3 year reservations for predictable workloads), or architectural changes (move to serverless, consolidate services). Each approach has different risk profiles, implementation timelines, and savings potential.

Chain-of-thought picks one path and follows it. Tree-of-thought explores all three simultaneously and compares them. Path A might save 15% with low risk in 2 weeks. Path B might save 35% with medium risk in 3 months. Path C might save 40% with high risk in 6 months. Only by exploring all three can you make an informed decision. Without orchestration, parallel path exploration means managing concurrent LLM calls, collecting heterogeneous results, and comparing them consistently.

## The Solution

**You write the problem definition, path exploration, and evaluation logic. Conductor handles parallel path execution, consistent scoring, and comparative selection.**

`DefineProblemWorker` analyzes the problem and identifies the key constraints, success criteria, and evaluation metrics. `FORK_JOIN` dispatches three path explorers: `PathAWorker`, `PathBWorker`, and `PathCWorker` each develop a complete solution approach with rationale, estimated impact, risks, and implementation plan. After `JOIN` collects all three paths, `EvaluatePathsWorker` scores each path against the defined criteria using a consistent rubric. `SelectBestWorker` picks the highest-scoring path and produces the final recommendation with the comparative analysis. Conductor runs all three explorations in parallel and records each path's development and scoring.

### What You Write: Workers

Six workers explore solution space. Defining the problem, exploring three parallel reasoning paths (analytical, creative, empirical), evaluating all paths, and selecting the best.

| Worker | Task | What It Does |
|---|---|---|
| **DefineProblemWorker** | `tt_define_problem` | Defines Problem and computes problem |
| **EvaluatePathsWorker** | `tt_evaluate_paths` | Evaluates and computes scores, best path, best solution, evaluation |
| **PathAWorker** | `tt_path_a` | Analytical reasoning path. Proposes a conventional, well-proven solution using load balancers, auto-scaling groups, a... |
| **PathBWorker** | `tt_path_b` | Creative reasoning path. Proposes an innovative edge-computing approach using CDN-based logic, serverless edge functi... |
| **PathCWorker** | `tt_path_c` | Empirical reasoning path. Proposes a data-driven solution based on traffic analysis: regional clusters with geo-routi... |
| **SelectBestWorker** | `tt_select_best` | Selects and returns the best reasoning path and its solution as the final output of the tree-of-thought workflow. |

Workers implement agent decisions and tool calls with realistic outputs so you can see the routing and handoff patterns without live LLM calls. Add your API keys to switch to live mode. the agent workflow stays the same.

### The Workflow

```
tt_define_problem
 │
 ▼
FORK_JOIN
 ├── tt_path_a
 ├── tt_path_b
 └── tt_path_c
 │
 ▼
JOIN (wait for all branches)
tt_evaluate_paths
 │
 ▼
tt_select_best

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
