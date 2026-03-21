# Prompt Templates in Java Using Conductor : Versioned Templates, Variable Resolution, and LLM Invocation

## Prompt Engineering Needs Version Control

When prompts are hardcoded in application code, changing a prompt means a code deploy. You can't A/B test prompts, roll back to a previous version, or track which prompt version produced which results. And when multiple applications share the same prompt patterns (summarization, classification, extraction), each one maintains its own copy.

A template system solves this: prompts are stored with IDs and version numbers, variables are resolved at runtime, and every LLM call records which template version was used. You can deploy a new prompt version without touching application code, and you can trace any output back to the exact prompt that produced it.

## The Solution

**You write the template resolution and LLM invocation logic. Conductor handles the versioned pipeline, retries, and observability.**

Each concern is an independent worker. template resolution (looking up the template by ID and version, substituting variables), LLM invocation (calling the model with the rendered prompt), and result collection (packaging the response with template metadata for auditability). Conductor chains them, retries the LLM call on transient errors, and records the template ID, version, rendered prompt, and response for every execution.

### What You Write: Workers

Three workers manage templated LLM calls. resolving a versioned template with variable substitution, calling the LLM with the resolved prompt, and collecting the result with template metadata for tracking.

| Worker | Task | What It Does |
|---|---|---|
| **CallLlmWorker** | `pt_call_llm` | Calls an LLM with a prompt. Uses OpenAI API in live mode, returns deterministic output in demo mode. |
| **CollectWorker** | `pt_collect` | Collects and logs the results of the prompt template pipeline. | Processing only |
| **ResolveTemplateWorker** | `pt_resolve_template` | Resolves a prompt template by substituting variables into placeholders. Template store contains versioned prompt templates. | Processing only |

**Live vs Demo mode:** When `CONDUCTOR_OPENAI_API_KEY` is set, `CallLlmWorker` calls the OpenAI Chat Completions API (model: `gpt-4o-mini`). Without the key, it runs in demo mode with deterministic output prefixed with `[DEMO]`. Non-LLM workers (template resolution, collection) always run their real logic.

### The Workflow

```
pt_resolve_template
 │
 ▼
pt_call_llm
 │
 ▼
pt_collect

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
