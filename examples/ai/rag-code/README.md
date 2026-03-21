# Code RAG in Java Using Conductor : Language-Aware Code Search and Answer Generation

## Code Search Is Not Text Search

Searching code requires different strategies than searching documents. A question like "How do I sort a list in Python?" needs language-aware parsing (extract "sort", "list", "Python"), code-specific embeddings (that understand function signatures, not just prose), and an index that stores code snippets with metadata like language, framework, and purpose. Standard text-based RAG misses semantic code structures.

The pipeline has four steps: parse the natural language query to extract language and intent, generate a code-aware embedding, search a code-specific index, and generate an answer with code examples from the retrieved snippets.

## The Solution

**You write the query parsing, code-aware embedding, and code index search logic. Conductor handles the pipeline, retries, and observability.**

Each stage is an independent worker. query parsing (extracting language and intent), code-aware embedding, code index search, and answer generation with code snippets. Conductor sequences them, retries the search if the index is temporarily unavailable, and tracks every query with the parsed intent, retrieved code snippets, and generated answer.

### What You Write: Workers

Four workers handle code-aware retrieval. parsing the natural language query for language and intent, embedding the code-specific query, searching a code index with language-aware filtering, and generating an answer that includes code snippets with syntax context.

| Worker | Task | What It Does |
|---|---|---|
| **EmbedCodeQueryWorker** | `cr_embed_code_query` | Worker that embeds a parsed code query into a vector representation using OpenAI text-embedding-3-small. |
| **GenerateCodeAnswerWorker** | `cr_generate_code_answer` | Worker that generates a code-aware answer from the question and retrieved code snippets. |
| **ParseQueryWorker** | `cr_parse_query` | Worker that parses a code-related question to extract intent, keywords, and language. Returns a fixed parsed intent o... |
| **SearchCodeIndexWorker** | `cr_search_code_index` | Worker that searches a code index using the embedding vector and code filter. Returns 3 fixed code snippets with id, ... |

Workers implement LLM API responses with realistic outputs so you can run the full pipeline without API keys. Set the provider API key environment variable to switch to live mode. the workflow and worker interfaces stay the same.

### The Workflow

```
cr_parse_query
 │
 ▼
cr_embed_code_query
 │
 ▼
cr_search_code_index
 │
 ▼
cr_generate_code_answer

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
