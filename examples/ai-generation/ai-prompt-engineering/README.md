# AI Prompt Engineering in Java Using Conductor : Define Task, Generate Variants, Test, Evaluate, Select Best

## Finding the Best Prompt Through Systematic Testing

Prompt engineering by trial and error is slow and unreliable. "Summarize this document" might work, but "You are an expert technical writer. Read the following document carefully and produce a concise summary that captures the key findings, methodology, and conclusions in 3-4 sentences" might work much better. Without systematic testing, you'll never know.

Automated prompt engineering generates multiple variants (different system prompts, instruction styles, few-shot examples, output format specifications), tests each against a standardized benchmark with consistent inputs, evaluates the outputs on quality metrics (accuracy, relevance, format compliance), and selects the variant that scores highest. This is the scientific method applied to prompt design.

## The Solution

**You just write the task definition, prompt variant generation, benchmark testing, evaluation scoring, and best-prompt selection logic. Conductor handles variant testing orchestration, score aggregation, and complete prompt iteration history.**

`DefineTaskWorker` establishes the task description, evaluation criteria (accuracy, format compliance, relevance), and test inputs. `GeneratePromptsWorker` creates multiple prompt variants. varying instruction style, system prompt, few-shot examples, and output format. `TestVariantsWorker` runs each variant against the test inputs and collects outputs. `EvaluateWorker` scores each variant's outputs against the evaluation criteria. `SelectBestWorker` picks the highest-scoring prompt variant and produces the final prompt with its evaluation metrics. Conductor records every variant and its scores for prompt iteration history.

### What You Write: Workers

Workers for variant generation, benchmark testing, and scoring operate independently, letting you iterate on prompt strategies without touching the evaluation logic.

| Worker | Task | What It Does |
|---|---|---|
| **DefineTaskWorker** | `ape_define_task` | Establishes the task specification from the description. defines input format, expected output, and evaluation criteria |
| **EvaluateWorker** | `ape_evaluate` | Ranked by quality. P3 leads with 0.91 |
| **GeneratePromptsWorker** | `ape_generate_prompts` | 5 prompt variants generated |
| **SelectBestWorker** | `ape_select_best` | Best prompt: P3 (score: 0.91) |

Workers implement AI generation stages with realistic outputs so you can see the pipeline without API keys. Set the provider API key to switch to live mode. the generation workflow stays the same.

### The Workflow

```
ape_define_task
 │
 ▼
ape_generate_prompts
 │
 ▼
ape_test_variants
 │
 ▼
ape_evaluate
 │
 ▼
ape_select_best

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
