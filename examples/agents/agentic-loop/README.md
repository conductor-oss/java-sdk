# Think-Act-Observe Loop Until Goal Completion

An agent sets a goal, then iterates through think-act-observe cycles in a DO_WHILE loop until the goal is achieved. Each iteration plans the next action, executes it (using a `RESULTS` map with action-specific outcomes), and observes the result. The loop terminates when the goal is met, and a summary is produced.

## Workflow

```
goal -> al_set_goal -> DO_WHILE(al_think -> al_act -> al_observe) -> al_summarize
```

## Workers

**SetGoalWorker** (`al_set_goal`) -- Outputs `goal` and `status: "active"`.

**ThinkWorker** (`al_think`) -- Produces a plan and tracks `iteration` count.

**ActWorker** (`al_act`) -- Executes using a static `RESULTS` map; unknown actions return `DEFAULT_RESULT: "Completed action step successfully"`.

**ObserveWorker** (`al_observe`) -- Evaluates the action result, e.g., `"Pattern analysis reveals clear structure; ready to formulate recommendations"`.

**SummarizeWorker** (`al_summarize`) -- Produces the final summary.

## Tests

43 tests cover goal setting, loop iteration, action execution, observation, and summarization.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
