# LLM Cost Tracking in Java Using Conductor: Multi-Model Calls with Per-Provider Cost Aggregation

End of month AWS bill: $12,000 in OpenAI API calls. Nobody knows which feature consumed what, or that the summarization pipeline was running GPT-4 on 10-word inputs that Gemini could have handled for pennies. Every provider bills differently. Per-token with separate input/output rates, usage metadata buried in different response fields, and without centralized tracking you're flying blind. This example builds a multi-provider cost tracking pipeline using [Conductor](https://github.com/conductor-oss/conductor) that sends the same prompt to GPT-4, Claude, and Gemini, captures per-call token usage, and aggregates a side-by-side cost breakdown so you know exactly where every dollar goes.

## Knowing What Your LLM Calls Actually Cost

When your application calls multiple LLM providers, cost tracking becomes fragmented. GPT-4 charges per token with different rates for input vs, output. Claude has its own token pricing. Gemini uses a different pricing model entirely. Without centralized tracking, you get a surprise bill at the end of the month with no breakdown of which provider, which prompt, or which feature drove the cost.

This workflow makes cost visible per call: each provider returns its token usage (prompt tokens, completion tokens), and an aggregation step applies each provider's pricing to compute per-model costs and a total. Over many executions, you build a dataset showing exactly how much each provider costs for your specific workloads. Enabling data-driven decisions about which model to use for which tasks.

## The Solution

**You write the provider API calls and pricing aggregation logic. Conductor handles the sequencing, retries, and observability.**

Each provider call is an independent worker. GPT-4, Claude, Gemini, each returning token usage alongside its response. An aggregation worker applies per-provider pricing rates to compute costs. Conductor sequences the calls, retries if any provider's API is temporarily unavailable, and tracks every execution with full token usage data. Over time, the execution history becomes your cost analytics dataset.

### What You Write: Workers

Four workers track costs across providers. Calling GPT-4, Claude, and Gemini sequentially with per-call token and cost tracking, then aggregating total spend, token usage, and per-model breakdowns in a final report.

| Worker | Task | What It Does |
|---|---|---|
| **CallGpt4Worker** | `ct_call_gpt4` | Calls GPT-4 with the prompt and returns token usage alongside the response text | **Live** when `CONDUCTOR_OPENAI_API_KEY` is set (calls OpenAI API, extracts `usage.prompt_tokens` and `usage.completion_tokens`). **Demo** otherwise (returns fixed tokens with `` prefix) |
| **CallClaudeWorker** | `ct_call_claude` | Calls Claude with the prompt and returns token usage alongside the response text | **Live** when `CONDUCTOR_ANTHROPIC_API_KEY` is set (calls Anthropic API, extracts `usage.input_tokens` and `usage.output_tokens`). **Demo** otherwise |
| **CallGeminiWorker** | `ct_call_gemini` | Calls Gemini with the prompt and returns token usage alongside the response text | **Live** when `GOOGLE_API_KEY` is set (calls Gemini API, extracts `usageMetadata`). **Demo** otherwise |
| **AggregateCostsWorker** | `ct_aggregate_costs` | Aggregate Costs. Computes and returns breakdown, total cost, total tokens | Pricing-based aggregation of real or HMAC-signed JWT counts |

Each worker auto-detects its API key from the environment. Set one, two, or all three keys to mix live and demo providers in the same workflow run. Without any keys, everything runs in demo mode with realistic output shapes.

### The Workflow

```
ct_call_gpt4
 |
 v
ct_call_claude
 |
 v
ct_call_gemini
 |
 v
ct_aggregate_costs

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
