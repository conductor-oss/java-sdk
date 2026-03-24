# Building a RAG Pipeline with TF-IDF Scoring and OpenAI Generation

You want to answer questions about your product documentation, but the LLM was trained months ago and hallucinates when asked about your specific APIs. Retrieval-Augmented Generation solves this by finding relevant documents first, then sending only those documents as context to the LLM. The question is: how do you wire together embedding, search, and generation as independently deployable, observable steps?

This example implements a three-stage RAG pipeline where the search worker uses a complete TF-IDF cosine similarity algorithm against an 8-document knowledge base, and the generation worker calls OpenAI's Chat Completions API with the retrieved context.

## Pipeline

```
brag_embed_query     (OpenAI text-embedding-3-small -> 1536-dim vector)
       |
       v
brag_search_vectors  (TF-IDF cosine similarity over 8 bundled docs, top-K with threshold)
       |
       v
brag_generate_answer (OpenAI gpt-4o-mini with retrieved context, temperature 0.3)
```

## Worker: EmbedQueryWorker (`brag_embed_query`)

Calls the OpenAI Embeddings API (`POST https://api.openai.com/v1/embeddings`) with model `text-embedding-3-small` (configurable via `OPENAI_EMBED_MODEL`). Input text is escaped for JSON embedding (backslash, quotes, newlines, tabs). The response is parsed into a `List<Double>` representing the embedding vector.

Rate limit (HTTP 429) and overload (HTTP 503) responses are marked `FAILED` (retryable). All other API errors are `FAILED_WITH_TERMINAL_ERROR`. The API key is read from `CONDUCTOR_OPENAI_API_KEY` at class load time.

## Worker: SearchVectorsWorker (`brag_search_vectors`) -- The TF-IDF Algorithm

This is the heart of the pipeline. Rather than requiring an external vector database, it implements TF-IDF cosine similarity from scratch.

### Step 1: Tokenization

Text is lowercased, non-alphanumeric characters are replaced with spaces, and tokens shorter than 2 characters are filtered out:

```java
Arrays.stream(text.toLowerCase().replaceAll("[^a-z0-9 ]", " ").split("\\s+"))
    .filter(t -> !t.isBlank() && t.length() > 1)
    .collect(Collectors.toList());
```

### Step 2: IDF Computation

The corpus is the knowledge base plus the query (9 documents total). For each term in the vocabulary:

```java
idf.put(term, Math.log((double) n / (1 + docCount)) + 1.0);
```

The `+1.0` smoothing prevents zero IDF for terms appearing in all documents. The `1 + docCount` in the denominator is Laplace smoothing.

### Step 3: TF-IDF Vectors and Cosine Similarity

Each document becomes a vector where dimension `i` = `(count of term_i / total tokens) * idf(term_i)`. Similarity is standard cosine:

```java
double dot = 0, normA = 0, normB = 0;
for (int i = 0; i < a.length; i++) {
    dot += a[i] * b[i];
    normA += a[i] * a[i];
    normB += b[i] * b[i];
}
return dot / (Math.sqrt(normA) * Math.sqrt(normB));
```

### Step 4: Threshold Filtering

Documents scoring below `0.05` are excluded. If zero documents pass the threshold, the worker returns `FAILED` (not terminal) with diagnostic data:

```java
fail.getOutputData().put("totalSearched", KNOWLEDGE_BASE.size());
fail.getOutputData().put("threshold", MIN_SCORE_THRESHOLD);
```

This explicit failure prevents the generation worker from being called with empty context -- the pipeline stops here rather than producing a hallucinated answer.

## Worker: GenerateAnswerWorker (`brag_generate_answer`)

Formats retrieved documents as numbered context (`[1] ...`, `[2] ...`) and sends them to `gpt-4o-mini` with a system prompt constraining answers to the provided context. Temperature is set to 0.3 for factual consistency. The worker requires non-empty context -- it will not call the LLM without retrieval results:

```java
if (context == null || context.isEmpty()) {
    fail.setReasonForIncompletion("Cannot call LLM without retrieval context.");
}
```

## The Knowledge Base

8 documents covering Conductor, RAG, vector embeddings, Orkes, workflow orchestration, microservices, LLMs, and task queues. Queries like "What is Conductor?" match `doc-01` and `doc-05` strongly; nonsense queries like "xyzzy plugh qwerty" produce zero matches and trigger the threshold filter.

## Test Coverage

4 test classes, 22 tests:

**SearchVectorsWorkerTest (9 tests):** Top-K limiting, score descending order, text/id/score field presence, `totalSearched` output, blank/missing query rejection, zero-match failure with diagnostic output, `topK <= 0` rejection, fallback from `queryText` to `question` input.

**GenerateAnswerWorkerTest (6 tests):** Null/blank question, null/empty/missing context rejection, API key requirement.

**EmbedQueryWorkerTest (4 tests):** Null/blank/missing question rejection, API key requirement.

**BasicRagIntegrationTest (3 tests):** Search-to-generate data contract validation, search result field completeness for downstream consumption, empty search results preventing generation.

## Configuration

| Variable | Default | Purpose |
|---|---|---|
| `CONDUCTOR_OPENAI_API_KEY` | (required) | OpenAI API authentication |
| `OPENAI_EMBED_MODEL` | `text-embedding-3-small` | Embedding model |
| `OPENAI_CHAT_MODEL` | `gpt-4o-mini` | Generation model |

---

> **How to run this example:** See [RUNNING.md](../../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
