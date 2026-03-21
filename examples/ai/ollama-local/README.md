# Ollama Local in Java Using Conductor : Code Review via Locally-Hosted LLMs

## Running LLMs Locally for Code Review

Cloud-hosted LLMs require API keys, cost money per token, and send your code to a third party. For code review on proprietary codebases, that's often a non-starter. Ollama lets you run models like CodeLlama, Mistral, and Llama 2 entirely on your local machine; but you need to verify the model is actually downloaded and loaded before calling it, handle generation timeouts (local inference is slower), and post-process the raw output into structured review feedback.

Without orchestration, a model-not-found error means a cryptic failure, a generation timeout means lost work, and there's no record of which model version reviewed which code.

## The Solution

**You write the Ollama model check, local generation call, and review formatting logic. Conductor handles the sequencing, retries, and observability.**

Each step is an independent worker. model availability check (is the requested model loaded in Ollama?), local generation (calling Ollama's `/api/generate` endpoint), and post-processing (structuring raw output into review comments). Conductor sequences them, retries the generation if Ollama's local inference times out, and tracks every review with the model used, the code reviewed, and the structured feedback produced.

### What You Write: Workers

Three workers manage local model inference. checking that the Ollama model is loaded and ready, generating a response locally, and post-processing the output with token counts and latency metrics.

| Worker | Task | What It Does |
|---|---|---|
| **OllamaCheckModelWorker** | `ollama_check_model` | Worker that verifies an Ollama model is available (demo). Takes model and ollamaHost, returns resolvedModel and ... |
| **OllamaGenerateWorker** | `ollama_generate` | Worker that simulates Ollama text generation. Takes prompt, model, ollamaHost, and options. Returns a fixed response ... |
| **OllamaPostProcessWorker** | `ollama_post_process` | Worker that post-processes the Ollama generation response. Takes response and wraps it in a review field. |

Workers implement LLM API responses with realistic outputs so you can run the full pipeline without API keys. Set the provider API key environment variable to switch to live mode. the workflow and worker interfaces stay the same.

### The Workflow

```
ollama_check_model
 │
 ▼
ollama_generate
 │
 ▼
ollama_post_process

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
