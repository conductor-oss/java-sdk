# Cohere Marketing Copy Generation in Java Using Conductor : Build Prompt, Generate Candidates, Select Best

## One Generation Is Not Enough for Production Copy

Asking an LLM to write marketing copy once gives you one option, and it might be mediocre. The best practice is to generate multiple candidates with different temperature settings or prompt variations, then select the one that best matches your criteria: engagement score, reading level, brand voice alignment, and call-to-action strength. But generating multiple candidates, scoring each one, and selecting the winner requires coordinating three distinct steps.

Without orchestration, you'd loop through generations in a single function, mix scoring logic with API calls, and have no record of which candidates were generated and why one was selected over the others. When the marketing team asks "why did we pick that version?", you can't answer.

## The Solution

**You write the prompt construction, Cohere API call, and candidate selection logic. Conductor handles the pipeline, retries, and observability.**

`CohereBuildPromptWorker` constructs a prompt tailored for marketing content. specifying tone, audience, product details, and desired call-to-action. `CohereGenerateWorker` calls the Cohere Generate API to produce multiple text candidates. `CohereSelectBestWorker` evaluates each candidate against quality criteria and selects the winner. Conductor records the prompt, all generated candidates, and the selection rationale, so the marketing team can review alternatives and understand why one version was chosen.

### What You Write: Workers

Three workers cover the full copy generation pipeline. prompt construction with tone and audience parameters, multi-candidate generation via Cohere, and quality-based selection of the best candidate.

| Worker | Task | What It Does |
|---|---|---|
| **CohereBuildPromptWorker** | `cohere_build_prompt` | Builds a Cohere generate request body from workflow input parameters. |
| **CohereGenerateWorker** | `cohere_generate` | Simulates a Cohere API generate call, returning a fixed response with 3 generations. |
| **CohereSelectBestWorker** | `cohere_select_best` | Selects the generation with the highest likelihood (least negative value). |

Workers implement LLM API responses with realistic outputs so you can run the full pipeline without API keys. Set the provider API key environment variable to switch to live mode. the workflow and worker interfaces stay the same.

### The Workflow

```
cohere_build_prompt
 │
 ▼
cohere_generate
 │
 ▼
cohere_select_best

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
