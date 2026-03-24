# Multi-Turn Chat with Conversation-Aware Document Retrieval

A user asks "Tell me about Conductor workflows" and gets a good answer. Then they ask "What about versioning?" -- but without conversation history, the retrieval system has no idea what "versioning" refers to. This workflow maintains session state across turns: it loads history from a `ConcurrentHashMap`-backed session store, combines the last 2 turns with the current message for contextual embedding, retrieves documents, generates a history-aware response, and persists the new turn.

## Workflow

```
sessionId, userMessage
       │
       ▼
┌───────────────────┐
│ crag_load_history │  Load prior turns from SESSION_STORE
└────────┬──────────┘
         │  history, turnCount
         ▼
┌──────────────────────────┐
│ crag_embed_with_context  │  Embed query + last 2 turns
└────────┬─────────────────┘
         │  embedding, contextualQuery
         ▼
┌──────────────────┐
│ crag_retrieve    │  Search with contextual embedding
└────────┬─────────┘
         │  documents (3 results with scores)
         ▼
┌──────────────────┐
│ crag_generate    │  Generate history-aware response
└────────┬─────────┘
         │  response
         ▼
┌──────────────────┐
│ crag_save_history│  Persist turn to session store
└──────────────────┘
         │
         ▼
   response, turnNumber, sourcesUsed
```

## Workers

**LoadHistoryWorker** (`crag_load_history`) -- Reads from a static `ConcurrentHashMap<String, List<Map<String, String>>>` called `SESSION_STORE`, keyed by `sessionId` (defaults to `"default"`). Returns the conversation history list and its size as `turnCount`.

**EmbedWithContextWorker** (`crag_embed_with_context`) -- Concatenates the `user` field from the last 2 history turns (via `stream().skip(Math.max(0, history.size() - 2))`) with the current message to form `contextualQuery`. When `CONDUCTOR_OPENAI_API_KEY` is set, calls OpenAI Embeddings API with model `text-embedding-3-small`. Otherwise returns a fixed 8-dimensional vector `[0.1, -0.3, 0.5, 0.2, -0.8, 0.4, -0.1, 0.7]`. Truncates the query preview at 60 characters for logging.

**RetrieveWorker** (`crag_retrieve`) -- Returns 3 hardcoded documents about Conductor features (versioning with score 0.93, polyglot workers with 0.88, JSON data flow with 0.84). In production, this would query a vector database.

**GenerateWorker** (`crag_generate`) -- When `CONDUCTOR_OPENAI_API_KEY` is set, calls `gpt-4o-mini` Chat Completions (`max_tokens: 512`, `temperature: 0.3`) with a system prompt, context documents, full conversation history (alternating user/assistant messages), and the current query. In fallback mode, returns different responses depending on whether history is empty (first turn mentions "versioning, polyglot workers, and JSON inputs/outputs") or not (follow-up mentions the context count and prior turn count). Uses the same retryable vs terminal error distinction (429/503 -> `FAILED`, other 4xx -> `FAILED_WITH_TERMINAL_ERROR`).

**SaveHistoryWorker** (`crag_save_history`) -- Creates a mutable copy of the history list via `new ArrayList<>(history)`, appends the current `user`/`assistant` turn as a `Map.of(...)`, and writes it back to `LoadHistoryWorker.SESSION_STORE` using `put(sessionId, history)`.

## Tests

29 tests cover session loading, contextual embedding, retrieval, generation with/without history, and history persistence.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
