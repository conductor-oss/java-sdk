# First AI Workflow in Java Using Conductor: Prompt, Call, Parse in Three Steps

Your LLM feature works beautifully in a Jupyter notebook. Then you deploy it. The first OpenAI rate-limit error crashes the process and loses the carefully constructed prompt. Nobody knows how many tokens the batch job burned overnight until the invoice arrives. When you swap from GPT-4 to Claude, you have to rewrite everything because the prompt formatting, API call, and response parsing are tangled in a single function. And when the CEO asks "can we see every question our users asked last week and what it cost?" you realize you have no audit trail at all. This example builds the simplest possible production AI pipeline with Conductor: prepare the prompt, call the LLM (with automatic retries on rate limits), and parse the response: each step independent, observable, and swappable. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate the three-step chain as independent workers, you write the prompt formatting, LLM call, and response parsing, Conductor handles retries, durability, and observability.

## The Simplest AI Pipeline

Calling an LLM from application code sounds simple. Format a prompt, make an API call, use the response. But even this minimal chain has operational concerns. The prompt template needs to be decoupled from the LLM call so you can change formatting without touching the API integration. The LLM call can fail due to rate limits, timeouts, or transient API errors, and you want automatic retries with backoff, not a crashed process. The raw response needs parsing and validation before it reaches your application. And you want to track token usage across every call for cost monitoring.

When these three steps are tangled in a single method, swapping the LLM provider means rewriting everything, a rate-limit error loses the prepared prompt, and there's no record of which calls consumed how many tokens.

## The Solution

**You write the prompt template, LLM call, and response parser. Conductor handles the chaining, retries, and observability.**

Each step is an independent worker. Prompt preparation, LLM invocation, response parsing. Conductor chains them so the formatted prompt feeds into the LLM call, and the raw response feeds into the parser. If the LLM call is rate-limited, Conductor retries automatically. Every execution records the question, the formatted prompt, the raw response, the parsed answer, and the token usage, giving you a complete audit trail for cost tracking and debugging.

### What You Write: Workers

Three workers form the simplest possible LLM pipeline. Preparing the prompt with system and user messages, calling the model, and parsing the structured response into application-ready fields.

| Worker | Task | What It Does |
|---|---|---|
| **AiCallLlmWorker** | `ai_call_llm` | Worker that simulates an LLM call and returns a fixed response. |
| **AiParseResponseWorker** | `ai_parse_response` | Worker that parses the raw LLM response and validates it. |
| **AiPreparePromptWorker** | `ai_prepare_prompt` | Worker that prepares a formatted prompt from a question and model name. |

Workers run in demo mode by default, returning realistic outputs so you can run the full pipeline without API keys. Set `CONDUCTOR_OPENAI_API_KEY` to switch to live mode (see Configuration below). The workflow and worker interfaces stay the same.

### The Workflow

```
ai_prepare_prompt
 │
 ▼
ai_call_llm
 │
 ▼
ai_parse_response

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
