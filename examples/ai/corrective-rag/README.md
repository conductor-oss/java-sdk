# Corrective RAG in Java Using Conductor: Self-Healing Retrieval with Web Search Fallback

Your vector store retrieves three documents for a question, but two are about a completely different topic. Stale embeddings from last quarter's data dump. The LLM cheerfully generates an answer grounded in noise, and your users lose trust because the system sounds confident while being wrong. Standard RAG has no quality gate; it generates from whatever comes back, relevant or not. This example builds a self-healing corrective RAG pipeline using [Conductor](https://github.com/conductor-oss/conductor) that grades retrieved documents for relevance and automatically falls back to web search when the vector store misses the mark.

## When Your Vector Store Doesn't Have the Answer

Standard RAG pipelines retrieve documents and generate.; no questions asked. If the vector store returns irrelevant content (stale embeddings, topic drift, missing coverage), the LLM hallucinates confidently from bad context. Users get wrong answers with no indication that the retrieval failed.

Corrective RAG adds a quality gate: after retrieval, an LLM-based grader scores each document for relevance. If the average relevance score falls below a threshold, the pipeline abandons the vector store results entirely and falls back to web search. The answer is then generated from fresh web results instead.

This creates a branching decision. Retrieve, grade, then either generate from the original documents or pivot to web search and generate from those results. Without orchestration, you'd implement this as nested if/else blocks with separate error handling for each path, no visibility into which branch was taken, and no easy way to retry a failed web search without re-running the entire pipeline.

## The Solution

**You write the retrieval grading and web search fallback logic. Conductor handles the conditional routing, retries, and observability.**

Each stage is an independent worker. Retrieving documents, grading relevance, searching the web, generating answers. Conductor's `SWITCH` task inspects the grader's verdict and routes to the right generation path. If the web search times out, Conductor retries it. Every execution records which path was taken and why, so you can audit retrieval quality over time without adding logging code.

### What You Write: Workers

Five workers split the self-healing pipeline across retrieval, relevance grading, web search fallback, and two generation paths, the SWITCH task decides which generation path runs based on the grader's verdict.

| Worker | Task | What It Does | Real / Notes |
|---|---|---|---|
| **RetrieveDocsWorker** | `cr_retrieve_docs` | Retrieves 3 documents from the vector store with LOW relevance scores (0.15-0.25, producing low scores for off-topic queries) to demonstrate the corrective fallback path | Requires API key, or swap in Pinecone, Weaviate, Qdrant, or pgvector |
| **GradeRelevanceWorker** | `cr_grade_relevance` | Scores each retrieved document for relevance (0-1 scale), computes the average, and returns a verdict: `"relevant"` if avg >= 0.5, `"irrelevant"` otherwise | Requires API key, or swap in an LLM-based grader (Claude, GPT-4) |
| **GenerateAnswerWorker** | `cr_generate_answer` | Generates a grounded answer from the vector store documents (taken when the verdict is `"relevant"`) | Requires API key, or swap in Claude Messages API or OpenAI Chat Completions |
| **WebSearchWorker** | `cr_web_search` | Performs a web search fallback when retrieved documents are irrelevant, returning 3 web results with title and snippet | Requires API key, or swap in Tavily, Brave Search, SerpAPI, or Google Custom Search |
| **GenerateFromWebWorker** | `cr_generate_from_web` | Generates a grounded answer from web search results (taken when the verdict is `"irrelevant"`) | Requires API key, or swap in Claude Messages API or OpenAI Chat Completions |

GenerateAnswerWorker and GenerateFromWebWorker require CONDUCTOR_OPENAI_API_KEY. RetrieveDocsWorker uses Jaccard similarity over bundled docs. WebSearchWorker fetches results from the Wikipedia API.

### The Workflow

```
cr_retrieve_docs
 │
 ▼
cr_grade_relevance
 │
 ▼
SWITCH (switch_ref)
 ├── relevant: cr_generate_answer
 └── default: cr_web_search -> cr_generate_from_web

```

## The Corrective RAG Pipeline

Standard RAG blindly generates from whatever the vector store returns. Corrective RAG adds a quality gate:

1. **Retrieve** (`cr_retrieve_docs`): Query the vector store for documents related to the question. The documents come back with relevance scores.

2. **Grade** (`cr_grade_relevance`): An LLM-based grader scores each document for relevance to the question on a 0-1 scale. If the average score is >= 0.5, the verdict is `"relevant"`; otherwise `"irrelevant"`.

3. **Route** (SWITCH): Conductor's SWITCH task inspects the verdict and routes to the appropriate generation path:
 - **Relevant**: Generate the answer directly from the retrieved documents
 - **Irrelevant**: Fall back to web search, then generate from fresh web results

This self-healing pattern ensures the user gets a grounded answer even when the vector store has stale embeddings, topic drift, or missing coverage. Every execution records which path was taken and the average relevance score, so you can audit retrieval quality over time.

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
