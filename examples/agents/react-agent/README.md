# ReAct Agent: Reason-Act-Observe Loop for Question Answering

The agent initializes a task from the question, then iterates through a DO_WHILE loop of reason-act-observe cycles. The reasoner produces a `thought` and selects an `action`. The actor executes the action and returns a `result` with its `source`. The observer evaluates whether the result is `useful: true`. After sufficient information is gathered, the final answer is produced with a confidence score.

## Workflow

```
question -> rx_init_task -> DO_WHILE(rx_reason -> rx_act -> rx_observe) -> rx_final_answer
```

## Workers

**InitTaskWorker** (`rx_init_task`) -- Sets up `question` and initial `context`.

**ReasonWorker** (`rx_reason`) -- Produces `thought` about what info is needed and selects an `action`.

**ActWorker** (`rx_act`) -- Executes the action and returns `result` with `source`.

**ObserveWorker** (`rx_observe`) -- Evaluates: `observation` string and `useful: true`.

**FinalAnswerWorker** (`rx_final_answer`) -- Produces the deterministic `answer` with confidence score.

## Tests

42 tests cover initialization, reasoning, action execution, observation, and answer synthesis.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
