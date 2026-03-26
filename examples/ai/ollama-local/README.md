# Running Code Review with a Locally-Hosted LLM via Ollama

Your security policy prohibits sending proprietary code to external APIs. This workflow runs LLM inference entirely locally through Ollama at `http://localhost:11434`, checking model availability, generating a code review, and post-processing the output -- no API keys or cloud calls required.

## Workflow

```
prompt, model
     │
     ▼
┌─────────────────────┐
│ ollama_check_model  │  Verify model is available locally
└──────────┬──────────┘
           │  resolvedModel, available
           ▼
┌─────────────────────┐
│ ollama_generate     │  Call local Ollama API
└──────────┬──────────┘
           │  response, model, totalDuration
           ▼
┌─────────────────────┐
│ ollama_post_process │  Clean and format review
└─────────────────────┘
           │
           ▼
     review, model
```

## Workers

**OllamaCheckModelWorker** (`ollama_check_model`) -- Validates that a model name was provided (returns FAILED with `"Model name is required"` if missing). Resolves the model name and reports `available: true`. The default Ollama host is `"http://localhost:11434"`, configurable via the `ollamaHost` input.

**OllamaGenerateWorker** (`ollama_generate`) -- Attempts a live call to the local Ollama API first. If the live call fails (Ollama not running, model not pulled), falls back to a deterministic code review response covering 5 points (e.g., "The function should return a consistent type"). Validates that `prompt` is not empty (returns FAILED with `"Prompt is required"` if missing). The base URL can be overridden but must start with `"http://"` or `"https://"`.

**OllamaPostProcessWorker** (`ollama_post_process`) -- Validates that `response` is present (returns FAILED with `"Response is required"` if missing). Returns the cleaned review text.

## Tests

17 tests cover model checking, generation in both live and fallback modes, input validation, and post-processing.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
