# Five-Step Chain-of-Thought Reasoning for Math Problems

A math problem needs step-by-step reasoning, not a one-shot answer. This pipeline understands the problem (identifying known values), reasons about the approach, calculates using regex-based pattern matching, verifies the answer, and produces the final response with a confidence score.

## Workflow

```
problem -> ct_understand_problem -> ct_step_1_reason -> ct_step_2_calculate -> ct_step_3_verify -> ct_final_answer
```

## Workers

**UnderstandProblemWorker** (`ct_understand_problem`) -- Extracts known values as a `Map.of(...)` and produces a structured understanding.

**Step1ReasonWorker** (`ct_step_1_reason`) -- Identifies variables and produces reasoning about the approach.

**Step2CalculateWorker** (`ct_step_2_calculate`) -- Uses `Pattern`/`Matcher` (regex) to parse and compute intermediate values.

**Step3VerifyWorker** (`ct_step_3_verify`) -- Uses `Pattern`/`Matcher` to verify the calculation.

**FinalAnswerWorker** (`ct_final_answer`) -- Produces the answer with a confidence score.

## Tests

45 tests cover understanding, reasoning, calculation, verification, and final answer generation.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
