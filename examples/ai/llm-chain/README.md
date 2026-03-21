# LLM Chain in Java Using Conductor: Prompt, Generate, Parse, Validate

First LLM call summarizes a customer email. Second one extracts product IDs. Third one validates against the catalog. If the second one fails with a rate limit, you've wasted the first call's tokens and the third one never runs, or worse, you retry the whole chain from scratch because everything was jammed into one function. When the final answer is wrong, you can't tell if the prompt was bad, the generation hallucinated, the parser choked, or the validator missed a rule. This example builds a four-step LLM chain using [Conductor](https://github.com/conductor-oss/conductor): prompt, generate, parse, validate, where each step is independently retryable and its inputs and outputs are recorded for debugging.

## LLM Output You Can Trust

LLMs generate text, but applications need structured, validated data. When a customer email asks about product recommendations, the LLM's response needs to be parsed into a structured format (product IDs, quantities, reasons) and validated against the actual product catalog. Does product X exist? Is it in stock? Is the price correct?

Each step in this chain depends on the previous one: the prompt must be formatted before generation, the raw text must be generated before parsing, and the parsed data must exist before validation. If the LLM generates malformed JSON, the parser catches it. If the parser succeeds but references a product ID that doesn't exist, the validator catches it. Without separating these concerns, you end up with a single method where prompt formatting, API calls, JSON parsing, and business validation are tangled together. Impossible to test individually and impossible to retry a failed LLM call without re-building the prompt.

## The Solution

**You write the prompt engineering, generation, parsing, and business validation logic. Conductor handles the chain sequencing, retries, and observability.**

Each stage of the chain is an independent worker. prompt construction (combining customer email with product catalog), LLM generation, response parsing (extracting structured JSON from raw text), and catalog validation (checking product IDs and prices). Conductor sequences them, retries the LLM call if it's rate-limited, and tracks every step so you can see exactly where in the chain a failure occurred, was it a bad prompt, a generation error, a parse failure, or a validation mismatch?

### What You Write: Workers

Four workers form a sequential processing chain: prompt construction from customer email context, LLM generation, structured response parsing, and output validation, each step refining the previous output.

| Worker | Task | What It Does |
|---|---|---|
| **ChainGenerateWorker** | `chain_generate` | LLM generation. Takes formattedPrompt, model, temperature, maxTokens. Calls OpenAI API in live mode, returns deterministic output in demo mode. |
| **ChainParseWorker** | `chain_parse` | Worker 3: Parses rawText JSON string into a Map. Returns FAILED status if parsing fails. | Processing only |
| **ChainPromptWorker** | `chain_prompt` | Worker 1: Takes customerEmail and productCatalog, builds a structured prompt with few-shot examples and expected JSON format. | Processing only |
| **ChainValidateWorker** | `chain_validate` | Worker 4: Validates parsedData against business rules. Runs 4 checks: valid_intent, valid_sentiment, products_in_catalog, reply_length. | Processing only |

**Live vs Demo mode:** When `CONDUCTOR_OPENAI_API_KEY` is set, `ChainGenerateWorker` calls the OpenAI Chat Completions API (model: `gpt-4o-mini`). Without the key, it runs in demo mode with deterministic output prefixed with ``. Non-LLM workers (prompt building, parsing, validation) always run their real logic.

### The Workflow

```
chain_prompt
 │
 ▼
chain_generate
 │
 ▼
chain_parse
 │
 ▼
chain_validate

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
