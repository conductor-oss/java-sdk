# Generating Marketing Copy with Cohere and Picking the Best Candidate

A marketing team needs multiple variations of product copy, not just one shot. The system generates three candidates via Cohere's Generate API, then selects the one with the highest likelihood score. This separates prompt construction, multi-generation, and quality-based selection into distinct pipeline stages.

## Workflow

```
productDescription, targetAudience
       │
       ▼
┌──────────────────────┐
│ cohere_build_prompt  │  Assemble generate request body
└──────────┬───────────┘
           │  requestBody
           ▼
┌──────────────────────┐
│ cohere_generate      │  Call Cohere API (3 generations)
└──────────┬───────────┘
           │  apiResponse.generations
           ▼
┌──────────────────────┐
│ cohere_select_best   │  Pick highest-likelihood generation
└──────────────────────┘
           │
           ▼
    bestGeneration, allTexts
```

## Workers

**CohereBuildPromptWorker** (`cohere_build_prompt`) -- Builds a `LinkedHashMap` request body for the Cohere Generate API. Constructs the prompt as `"Write a compelling marketing copy for the following product. Target audience: " + targetAudience + ". Product: " + productDescription`. Workflow defaults: model `"command"`, `maxTokens: 300`, `temperature: 0.9`, `k: 0`, `p: 0.75`, `numGenerations: 3`. Also sets `return_likelihoods: "GENERATION"` and `truncate: "END"`.

**CohereGenerateWorker** (`cohere_generate`) -- Checks `COHERE_API_KEY` env var at construction. In live mode, POSTs to `https://api.cohere.ai/v1/generate` with a 10-second connect timeout and 60-second request timeout. In fallback mode, returns 3 hardcoded generations with likelihoods of `-1.82`, `-1.65`, and `-1.91`, plus billing metadata of 42 input / 185 output tokens. The fallback generations are realistic SmartBoard Pro marketing copy variants.

**CohereSelectBestWorker** (`cohere_select_best`) -- Iterates through the `generations` list, comparing each entry's `likelihood` (a `Number` cast to `double`) against `Double.NEGATIVE_INFINITY` to find the maximum. The least-negative likelihood wins -- in the fallback data, that's `-1.65` (the second generation). Also collects all texts into an `allTexts` list via `.stream().map(...).toList()`.

## Tests

15 tests across 3 test files cover prompt construction, API call modes, and best-candidate selection logic.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
