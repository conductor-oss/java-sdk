# Document Q&A via Mistral Chat Completions

A team needs to answer questions about technical documents using Mistral's chat API. The workflow composes a request with system and user messages, calls the Mistral Chat Completions endpoint, and extracts the answer from the response.

## Workflow

```
prompt, context, model
       │
       ▼
┌────────────────────────────┐
│ mistral_compose_request    │  Build system + user messages
└─────────────┬──────────────┘
              │  requestBody
              ▼
┌────────────────────────────┐
│ mistral_chat               │  POST to api.mistral.ai
└─────────────┬──────────────┘
              │  apiResponse
              ▼
┌────────────────────────────┐
│ mistral_extract_answer     │  Extract answer text
└────────────────────────────┘
              │
              ▼
       answer, model
```

## Workers

**MistralComposeRequestWorker** (`mistral_compose_request`) -- Builds a request body with a `messages` array containing a system message and a user message. Assembles the context and prompt into the appropriate Mistral Chat Completions format.

**MistralChatWorker** (`mistral_chat`) -- Checks for `MISTRAL_API_KEY` env var. In live mode, POSTs to `https://api.mistral.ai/v1/chat/completions` with the composed request body. In fallback mode, returns a deterministic response. Handles HTTP errors by setting task to FAILED.

**MistralExtractAnswerWorker** (`mistral_extract_answer`) -- Extracts the generated text from the Mistral response structure and returns it as the final answer.

## Tests

12 tests cover request composition, API calling in both modes, and answer extraction.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
