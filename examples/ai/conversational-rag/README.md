# Conversational RAG in Java Using Conductor : Multi-Turn Chat with Context-Aware Retrieval

## Why Conversational RAG Is Harder Than Single-Shot RAG

A single-turn RAG pipeline is straightforward: embed the query, retrieve documents, generate a response. But once users ask follow-up questions ("What about the pricing?" after asking "Tell me about product X"), the query alone is ambiguous. You need conversation history to resolve references and maintain coherence.

That means every request now depends on session state. loading prior turns, rewriting the query with conversational context, retrieving against the enriched query, generating a history-aware response, and persisting the new turn. Each of these steps can fail independently (embedding service timeout, vector store unreachable, LLM rate-limited), and if any step fails mid-conversation, you need to retry without corrupting session state or losing the user's place.

Without orchestration, you'd build this as a single method with nested try/catch blocks, manual session locking, and ad-hoc retry logic. code that's fragile, hard to test, and impossible to observe when a user reports "the bot forgot what I said."

## The Solution

**You write the session management, context-aware embedding, and generation logic. Conductor handles the multi-turn sequencing, retries, and observability.**

Each stage of the conversational RAG pipeline is an independent worker. loading history, embedding with context, retrieving documents, generating a response, saving the turn. Conductor sequences them, passes outputs between stages, retries on transient failures, and tracks every turn of every conversation for debugging. You get durable, observable multi-turn RAG without writing a line of orchestration code.

### What You Write: Workers

Five workers manage the full conversation turn. loading session history, embedding with conversational context, retrieving documents, generating a history-aware response, and persisting the new turn.

| Worker | Task | What It Does |
|---|---|---|
| **EmbedWithContextWorker** | `crag_embed_with_context` | Worker that embeds the user query with conversational context. Combines recent history with the current message to fo... |
| **GenerateWorker** | `crag_generate` | Worker that generates a response using user message, conversation history, and retrieved context documents. Returns a... |
| **LoadHistoryWorker** | `crag_load_history` | Worker that loads conversation history for a given session. Uses a static in-memory ConcurrentHashMap as the session ... |
| **RetrieveWorker** | `crag_retrieve` | Worker that retrieves relevant documents based on the contextual query. Returns 3 fixed documents with text and simil... |
| **SaveHistoryWorker** | `crag_save_history` | Worker that saves the current turn to conversation history. Appends user + assistant messages to the shared SESSION_S... |

Workers implement LLM API responses with realistic outputs so you can run the full pipeline without API keys. Set the provider API key environment variable to switch to live mode. the workflow and worker interfaces stay the same.

### The Workflow

```
crag_load_history
 │
 ▼
crag_embed_with_context
 │
 ▼
crag_retrieve
 │
 ▼
crag_generate
 │
 ▼
crag_save_history

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
