# Reflection Agent: Generate, Reflect, Improve in Iterative Refinement

The agent generates an initial draft with a low quality score, then enters a DO_WHILE loop: reflect (producing feedback that varies by iteration for progressive improvement), then improve (applying the feedback). The loop continues until quality is sufficient. A final output is produced with the accumulated quality score.

## Workflow

```
topic -> rn_initial_generation -> DO_WHILE(rn_reflect -> rn_improve) -> rn_final_output
```

## Workers

**InitialGenerationWorker** (`rn_initial_generation`) -- Produces initial `content` with a deliberately low quality score.

**ReflectWorker** (`rn_reflect`) -- Produces `feedback` and a quality `score` that varies per iteration.

**ImproveWorker** (`rn_improve`) -- Applies feedback, outputs improved `content` and `applied` changes.

**FinalOutputWorker** (`rn_final_output`) -- Produces the refined `content` with final quality score.

## Tests

34 tests cover initial generation, reflection feedback, improvement iterations, and final output.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
