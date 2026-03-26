# The Simplest AI Pipeline: Prompt, Call, Parse

Your LLM feature works in a notebook, but in production a single rate-limit error crashes the process and loses the carefully constructed prompt. This three-step workflow separates prompt preparation, LLM invocation, and response parsing into independent workers so each step can be retried, observed, and swapped independently.

## Workflow

```
question, model
       │
       ▼
┌────────────────────┐
│ ai_prepare_prompt  │  Format the prompt string
└─────────┬──────────┘
          │  formattedPrompt
          ▼
┌────────────────────┐
│ ai_call_llm        │  POST to OpenAI Chat Completions
└─────────┬──────────┘
          │  rawResponse, tokenUsage
          ▼
┌────────────────────┐
│ ai_parse_response  │  Validate and extract answer
└────────────────────┘
          │
          ▼
   question, answer, model, tokens
```

## Workers

**AiPreparePromptWorker** (`ai_prepare_prompt`) -- Takes `question` and `model` from workflow input (both default to `"Hello"` and `"gpt-4"` if blank). Constructs a formatted prompt: `"You are a helpful assistant. Using " + model + ", please answer: " + question`.

**AiCallLlmWorker** (`ai_call_llm`) -- When `CONDUCTOR_OPENAI_API_KEY` is set, calls `https://api.openai.com/v1/chat/completions` using Java's `HttpClient` with a 10-second connect timeout and 60-second request timeout. Extracts the response text from `choices[0].message.content` and token usage from `usage.prompt_tokens` / `usage.completion_tokens` / `usage.total_tokens`, mapping them to camelCase keys (`promptTokens`, `completionTokens`, `totalTokens`). In demo mode (no API key), returns a fixed response about Orkes Conductor with `promptTokens: 45`, `completionTokens: 38`, `totalTokens: 83` and sets `demoMode: true`. The workflow passes `maxTokens: 1024` and `temperature: 0.7`.

**AiParseResponseWorker** (`ai_parse_response`) -- Receives `rawResponse` and `tokenUsage`, logs the response length, and outputs `answer` (the raw response) plus `valid: true`.

## Tests

11 tests cover prompt formatting, API call behavior in both demo and live modes, and response parsing.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
