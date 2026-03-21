# System Prompts in Java Using Conductor : A/B Test Formal vs Casual Tone with Side-by-Side Comparison

## Testing System Prompt Variations

The system prompt dramatically affects LLM output. formal prompts produce longer, more structured responses while casual prompts produce shorter, more conversational ones. But the effect varies by model and use case. To choose the right system prompt, you need to run the same user prompt through multiple variations and compare the outputs on metrics that matter (tone, length, helpfulness, accuracy).

This workflow builds the prompt with each system prompt style, calls the LLM for each, and compares the outputs. recording both responses and the comparison metrics.

## The Solution

**You write the prompt variations and comparison scoring logic. Conductor handles the A/B test sequencing, retries, and observability.**

Each step is an independent worker. prompt building (combining system prompt with user message), LLM invocation, and output comparison. Conductor sequences the two LLM calls and the comparison step, retries if either call is rate-limited, and tracks every A/B test with both responses and the comparison results.

### What You Write: Workers

Three workers enable system prompt experimentation. building the full prompt with a system persona and user message, calling the LLM, and comparing outputs from different system prompts (formal vs, casual) side by side.

| Worker | Task | What It Does |
|---|---|---|
| **SpBuildPromptWorker** | `sp_build_prompt` | Builds a full prompt by combining a system prompt, few-shot examples, and the user's prompt based on the requested style. | Processing only |
| **SpCallLlmWorker** | `sp_call_llm` | Calls an LLM with the assembled prompt. Uses OpenAI API in live mode, returns style-based deterministic output in demo mode. |
| **SpCompareOutputsWorker** | `sp_compare_outputs` | Compares the formal and casual LLM responses, reporting length differences and tone insights. | Processing only |

**Live vs Demo mode:** When `CONDUCTOR_OPENAI_API_KEY` is set, `SpCallLlmWorker` calls the OpenAI Chat Completions API (model: `gpt-4o-mini`) with system prompt, few-shot examples, and user message. Without the key, it runs in demo mode with style-based deterministic output prefixed with `[DEMO]`. Non-LLM workers (prompt building, comparison) always run their real logic.

### The Workflow

```
sp_build_prompt
 │
 ▼
sp_call_llm
 │
 ▼
sp_build_prompt
 │
 ▼
sp_call_llm
 │
 ▼
sp_compare_outputs

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
