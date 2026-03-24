# Automated Prompt Engineering: Define, Generate Variants, Test, Evaluate, Select Best

Systematically optimize prompts: define the task, generate multiple prompt variants via LLM, test each variant against 100 sample inputs, evaluate and rank them, and select the best by ID and score.

## Workflow

```
taskDescription, modelId, evaluationCriteria
  -> ape_define_task -> ape_generate_prompts -> ape_test_variants -> ape_evaluate -> ape_select_best
```

## Tests

2 tests cover the prompt engineering pipeline.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
