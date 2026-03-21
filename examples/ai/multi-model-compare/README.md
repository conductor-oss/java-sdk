# Multi-Model Compare in Java Using Conductor : GPT-4 vs Claude vs Gemini Side-by-Side Evaluation

## Choosing the Right Model with Data

Picking an LLM provider for your application is usually based on vibes. "GPT-4 feels smarter" or "Claude is better at code." A data-driven approach sends the same prompts to all three models, compares their responses on objective criteria (relevance, coherence, completeness), and builds a dataset showing which model performs best for your specific use cases.

Running this comparison manually means sequential API calls (3x the latency), ad-hoc comparison in a spreadsheet, and no historical record. If one provider's API is slow or rate-limited, it blocks the other comparisons.

## The Solution

**You write the per-model API calls and the comparison scoring logic. Conductor handles the parallel execution, retries, and observability.**

Each model call is an independent worker. GPT-4, Claude, Gemini. Conductor's `FORK_JOIN` runs all three in parallel and waits for all to complete. A comparison worker scores each response and picks a winner. If Claude's API is rate-limited, Conductor retries it independently without re-calling GPT-4 or Gemini. Every execution records all three responses, scores, and the winner. building an evaluation dataset over time.

### What You Write: Workers

Four workers run a model comparison. calling GPT-4, Claude, and Gemini in parallel via FORK_JOIN, then scoring and ranking all three responses for quality, speed, and cost in a single comparison worker.

| Worker | Task | What It Does |
|---|---|---|
| **McCallGpt4Worker** | `mc_call_gpt4` | Calls GPT-4 via OpenAI Chat Completions API. Uses `CONDUCTOR_OPENAI_API_KEY` env var; falls back to demo when unset. | Both |
| **McCallClaudeWorker** | `mc_call_claude` | Calls Claude via Anthropic Messages API. Uses `CONDUCTOR_ANTHROPIC_API_KEY` env var; falls back to demo when unset. | Both |
| **McCallGeminiWorker** | `mc_call_gemini` | Calls Gemini via Google Generative AI API. Uses `GOOGLE_API_KEY` env var; falls back to demo when unset. | Both |
| **McCompareWorker** | `mc_compare` | Compares results from GPT-4, Claude, and Gemini model calls. Determines the winner (highest quality), fastest, cheapest. | N/A |

Each worker auto-detects its API key at startup. When a key is present, the worker makes real API calls; when absent, it returns a deterministic demo response prefixed with `[DEMO]`. You can set any combination. for example, set only `CONDUCTOR_OPENAI_API_KEY` to get live GPT-4 results alongside demo Claude and Gemini.

### The Workflow

```
FORK_JOIN
 ├── mc_call_gpt4
 ├── mc_call_claude
 └── mc_call_gemini
 │
 ▼
JOIN (wait for all branches)
mc_compare

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
