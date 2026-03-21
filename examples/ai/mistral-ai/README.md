# Mistral AI in Java Using Conductor : Document Q&A via Mistral Chat Completions

## Structured Mistral API Integration

Mistral's chat completion API uses a messages-based format with system and user roles, supports safe prompt mode for content moderation, and returns responses with usage metadata. When you embed document context into a chat completion call, the request composition (building the messages array with the document as context and the question as the user message) should be separate from the API call itself, and the answer extraction (pulling text from the choices array) should be decoupled from both.

Bundling request composition, API calls, and response parsing into a single method means changing the prompt format requires touching API code, a rate-limit error loses the composed request, and there's no record of which model/temperature combination produced which answer.

## The Solution

**You write the Mistral request composition and answer extraction logic. Conductor handles the API pipeline, retries, and observability.**

Each concern is an independent worker. composing the Mistral chat request (messages array, generation config, safety settings), calling the chat completion API, and extracting the answer from the response. Conductor chains them so the composed request feeds into the API call, and the raw response feeds into the extractor. If Mistral rate-limits the call, Conductor retries it without re-composing the request.

### What You Write: Workers

Three workers manage the Mistral integration. composing the chat request with system and user messages, calling the Mistral Chat API, and extracting the answer from the choices array.

| Worker | Task | What It Does |
|---|---|---|
| **MistralChatWorker** | `mistral_chat` | Simulates calling the Mistral chat completion API. In production this would make an HTTP call to the Mistral API. Her... |
| **MistralComposeRequestWorker** | `mistral_compose_request` | Composes a Mistral chat completion request body from workflow inputs. Inputs: document, question, model, temperature,... |
| **MistralExtractAnswerWorker** | `mistral_extract_answer` | Extracts the assistant's answer from a Mistral chat completion response. |

Workers implement LLM API responses with realistic outputs so you can run the full pipeline without API keys. Set the provider API key environment variable to switch to live mode. the workflow and worker interfaces stay the same.

### The Workflow

```
mistral_compose_request
 │
 ▼
mistral_chat
 │
 ▼
mistral_extract_answer

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
