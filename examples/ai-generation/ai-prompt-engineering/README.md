# AI Prompt Engineering: Systematic Prompt Optimization

A team fine-tunes prompts for a summarization task but relies on gut feeling to decide which prompt version works best. Without systematic evaluation, they cannot tell whether "Summarize the following text" outperforms "Provide a concise summary of:" -- or by how much. The process needs to be rigorous: define the task, generate multiple prompt variants, test each against the same inputs, rank them by score, and select the winner with justification.

This workflow automates the full prompt engineering lifecycle: task definition, variant generation, testing, evaluation, and selection.

## Pipeline Architecture

```
taskDescription, modelId, evaluationCriteria
         |
         v
  ape_define_task        (taskSpec="summarization-text-200")
         |
         v
  ape_generate_prompts   (prompts="5-variants-generated", count=5)
         |
         v
  ape_test_variants      (variantCount=5, results="P3-leads-0.91")
         |
         v
  ape_evaluate           (rankings="P3-P4-P2-P1-P5")
         |
         v
  ape_select_best        (bestPromptId="P3", bestScore=0.91, selected=true)
```

## Worker: DefineTask (`ape_define_task`)

Parses the `taskDescription` into a structured task specification. When an LLM is available, generates a detailed spec via the model. Falls back to a deterministic `taskSpec: "summarization-text-200"` encoding the task type and target length.

## Worker: GeneratePrompts (`ape_generate_prompts`)

Produces multiple prompt variants from the task specification. When an LLM is available, generates diverse prompt formulations via the model. Falls back to `prompts: "5-variants-generated"` and `count: 5`. The variants are labeled P1 through P5 for tracking.

## Worker: TestVariants (`ape_test_variants`)

Tests all prompt variants against the target `modelId`. Runs each variant and collects performance data. Returns `variantCount: 5` and `results: "P3-leads-0.91"`, indicating that variant P3 achieved the highest score of 0.91.

## Worker: Evaluate (`ape_evaluate`)

Ranks all variants based on the test results and `evaluationCriteria`. When an LLM is available, generates detailed comparative analysis. Falls back to `rankings: "P3-P4-P2-P1-P5"`, ordering variants from best to worst performance.

## Worker: SelectBest (`ape_select_best`)

Selects the top-ranked prompt from the rankings. Returns `bestPromptId: "P3"`, `bestScore: 0.91`, and `selected: true`. When an LLM is available, also provides a `justification` explaining why P3 outperformed the alternatives.

## Tests

2 tests cover the prompt engineering pipeline.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
