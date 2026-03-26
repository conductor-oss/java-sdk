# Three Solvers Compete, a Judge Scores, Winner Selected

Three solver agents tackle the same problem in parallel: a creative approach ("AI-Powered Adaptive System"), an analytical one ("Data-Driven Optimization Framework"), and a practical one ("Incremental Process Improvement"). A judge evaluates all three on cost, feasibility, and impact, then the winner is selected.

## Workflow

```
problem, criteria
       |
       v
+--- FORK_JOIN ---------+
| solver_1 | solver_2 | solver_3 |
+-----------+---------+
            v
     comp_judge_agent
            v
     comp_select_winner
```

## Workers

**Solver1Worker** (`comp_solver_1`) -- `approach: "creative"`, `title: "AI-Powered Adaptive System"`.

**Solver2Worker** (`comp_solver_2`) -- `approach: "analytical"`, `title: "Data-Driven Optimization Framework"`.

**Solver3Worker** (`comp_solver_3`) -- `approach: "practical"`, `title: "Incremental Process Improvement"`.

**JudgeAgentWorker** (`comp_judge_agent`) -- Calls `scoreSolution()` for each solver on cost, feasibility, impact.

**SelectWinnerWorker** (`comp_select_winner`) -- Picks the highest-scoring solution from the judgment.

## Tests

37 tests cover all three solvers, judging logic, and winner selection.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
